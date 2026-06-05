import {
  BadGatewayException,
  Injectable,
  Logger,
  PayloadTooLargeException,
  ServiceUnavailableException,
} from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { randomUUID } from 'crypto';
import { Model } from 'mongoose';
import {
  AssistantMessage,
  AssistantMessageDocument,
} from '../database/schemas/assistant-message.schema';
import {
  AssistantThread,
  AssistantThreadDocument,
} from '../database/schemas/assistant-thread.schema';
import {
  buildPaginatedResult,
  clampLimit,
  cursorFilter,
} from '../common/pagination/cursor.util';
import { mapDoc, mapDocs } from '../database/document.util';
import { validateAssistantActions } from './assistant-action.validator';
import { ChatRequestDto } from './dto/chat-request.dto';
import { AssistantActionDto } from './dto/assistant-action.dto';
import { CoachEvaluateRequestDto } from './dto/coach-evaluate.dto';
import { CoachSignalsRequestDto } from './dto/coach-signals.dto';
import { COACH_SIGNAL_TYPES, CoachSignalDto } from './dto/coach-signal.dto';
import {
  buildChatUserPrompt,
  SYSTEM_CHAT_PROMPT,
} from './prompts/system-chat';
import {
  buildCoachEvaluateUserPrompt,
  SYSTEM_COACH_EVALUATE_PROMPT,
} from './prompts/system-coach-evaluate';
import {
  buildCoachSignalsUserPrompt,
  SYSTEM_COACH_SIGNALS_PROMPT,
} from './prompts/system-coach-signals';
import { assertAiRateLimit } from './ai-rate-limit';
import {
  OpenRouterNotConfiguredError,
  resolveOpenRouterModels,
} from './openrouter-models';

const SNAPSHOT_MAX_BYTES = 32 * 1024;
const OPENROUTER_URL = 'https://openrouter.ai/api/v1/chat/completions';

export interface CoachEvaluateResult {
  shouldNotify: boolean;
  priority?: string;
  title?: string;
  body?: string;
  chatMessage?: string;
  deepLink?: string;
  rationale?: string;
}

export interface CoachSignalsResult {
  signals: CoachSignalDto[];
}

@Injectable()
export class AiService {
  private readonly logger = new Logger(AiService.name);

  constructor(
    @InjectModel(AssistantThread.name)
    private readonly threadModel: Model<AssistantThreadDocument>,
    @InjectModel(AssistantMessage.name)
    private readonly messageModel: Model<AssistantMessageDocument>,
  ) {}

  async chat(dto: ChatRequestDto) {
    assertAiRateLimit('ai-chat');
    this.assertSnapshotSize(dto.contextSnapshot);

    const existing = await this.messageModel
      .findOne({ clientMessageId: dto.clientMessageId, role: 'user' })
      .exec();
    if (existing) {
      const assistant = await this.messageModel
        .findOne({
          threadId: existing.threadId,
          role: 'assistant',
          createdAt: { $gte: existing.createdAt },
        })
        .sort({ createdAt: 1 })
        .exec();
      if (assistant) {
        return {
          threadId: existing.threadId,
          userMessageId: existing._id,
          assistantMessageId: assistant._id,
          content: assistant.content,
          actions: assistant.metadata?.proposedActions ?? [],
          createdAt: assistant.createdAt.toISOString(),
        };
      }
    }

    const threadId = dto.threadId ?? randomUUID();
    const now = new Date();
    await this.ensureThread(threadId, dto.message, now);

    const history = await this.messageModel
      .find({ threadId, deletedAt: null })
      .sort({ createdAt: 1 })
      .limit(20)
      .exec();

    const messages = [
      { role: 'system' as const, content: SYSTEM_CHAT_PROMPT },
      ...history.map((m) => ({
        role: m.role as 'user' | 'assistant' | 'system',
        content: m.content,
      })),
      {
        role: 'user' as const,
        content: buildChatUserPrompt(dto.message, dto.contextSnapshot),
      },
    ];

    const rawAssistant = await this.completeChat(messages, { maxTokens: 1536 });
    const { reply, actions } = this.parseChatResult(rawAssistant);

    const userMessageId = randomUUID();
    const assistantMessageId = randomUUID();
    const assistantCreated = new Date(now.getTime() + 1);
    await this.messageModel.create([
      {
        _id: userMessageId,
        threadId,
        role: 'user',
        content: dto.message,
        clientMessageId: dto.clientMessageId,
        createdAt: now,
        updatedAt: now,
      },
      {
        _id: assistantMessageId,
        threadId,
        role: 'assistant',
        content: reply,
        metadata:
          actions.length > 0
            ? { proposedActions: actions, actionsStatus: 'pending' as const }
            : undefined,
        createdAt: assistantCreated,
        updatedAt: assistantCreated,
      },
    ]);
    await this.threadModel.updateOne(
      { _id: threadId },
      { updatedAt: new Date() },
    );

    return {
      threadId,
      userMessageId,
      assistantMessageId,
      content: reply,
      actions,
      createdAt: now.toISOString(),
    };
  }

  async coachEvaluate(dto: CoachEvaluateRequestDto): Promise<CoachEvaluateResult> {
    assertAiRateLimit('ai-coach');
    this.assertSnapshotSize(dto.contextSnapshot);
    const raw = await this.completeChat(
      [
        { role: 'system', content: SYSTEM_COACH_EVALUATE_PROMPT },
        {
          role: 'user',
          content: buildCoachEvaluateUserPrompt(
            dto.contextSnapshot,
            dto.signals,
            dto.lastNudgeAt,
            dto.dismissCountToday,
          ),
        },
      ],
      { temperature: 0.3, maxTokens: 400 },
    );
    return this.parseCoachResult(raw);
  }

  async coachSignals(dto: CoachSignalsRequestDto): Promise<CoachSignalsResult> {
    assertAiRateLimit('ai-coach-signals');
    this.assertSnapshotSize(dto.contextSnapshot);
    const raw = await this.completeChat(
      [
        { role: 'system', content: SYSTEM_COACH_SIGNALS_PROMPT },
        {
          role: 'user',
          content: buildCoachSignalsUserPrompt(
            dto.contextSnapshot,
            dto.knownSignalTypes,
          ),
        },
      ],
      { temperature: 0.2, maxTokens: 600 },
    );
    return this.parseCoachSignalsResult(raw);
  }

  async listThreadMessages(
    threadId: string,
    limit?: string,
    cursor?: string,
  ) {
    const filter: Record<string, unknown> = { threadId, deletedAt: null };
    const pageLimit = clampLimit(limit ?? '50');
    if (cursor) {
      const docs = await this.messageModel
        .find(cursorFilter(filter, cursor))
        .sort({ updatedAt: 1, _id: 1 })
        .limit(pageLimit + 1)
        .exec();
      const mapped = mapDocs(docs);
      return buildPaginatedResult(
        mapped,
        pageLimit,
        (i) => i.id as string,
        (i) => new Date(i.updatedAt as string),
      );
    }
    const items = await this.messageModel
      .find(filter)
      .sort({ createdAt: 1 })
      .limit(pageLimit)
      .exec();
    return {
      items: mapDocs(items),
      nextCursor: null,
      serverTime: new Date().toISOString(),
    };
  }

  private async ensureThread(threadId: string, firstMessage: string, now: Date) {
    const existing = await this.threadModel.findById(threadId).exec();
    if (existing) return;
    const title = firstMessage.trim().slice(0, 80) || 'Coach';
    await this.threadModel.create({
      _id: threadId,
      title,
      createdAt: now,
      updatedAt: now,
    });
  }

  private assertSnapshotSize(snapshot: Record<string, unknown>) {
    const bytes = Buffer.byteLength(JSON.stringify(snapshot), 'utf8');
    if (bytes > SNAPSHOT_MAX_BYTES) {
      throw new PayloadTooLargeException({
        code: 'SNAPSHOT_TOO_LARGE',
        message: `contextSnapshot exceeds ${SNAPSHOT_MAX_BYTES} bytes`,
      });
    }
  }

  private async completeChat(
    messages: { role: 'system' | 'user' | 'assistant'; content: string }[],
    opts?: { temperature?: number; maxTokens?: number },
  ): Promise<string> {
    const apiKey = process.env.OPENROUTER_API_KEY?.trim();
    if (!apiKey) {
      throw new ServiceUnavailableException({
        code: 'AI_NOT_CONFIGURED',
        message: 'Set OPENROUTER_API_KEY and OPENROUTER_MODEL on the server',
      });
    }

    let modelConfig;
    try {
      modelConfig = resolveOpenRouterModels();
    } catch (err) {
      if (err instanceof OpenRouterNotConfiguredError) {
        throw new ServiceUnavailableException({
          code: 'AI_NOT_CONFIGURED',
          message: err.message,
        });
      }
      throw err;
    }

    const headers: Record<string, string> = {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    };
    const referer = process.env.OPENROUTER_HTTP_REFERER?.trim();
    if (referer) headers['HTTP-Referer'] = referer;
    headers['X-Title'] = process.env.OPENROUTER_APP_NAME?.trim() || 'Stella';

    const payload = {
      model: modelConfig.primary,
      models: modelConfig.models,
      messages,
      temperature: opts?.temperature ?? 0.6,
      max_tokens: opts?.maxTokens ?? 1024,
    };

    const response = await this.fetchOpenRouter(headers, payload);

    if (!response.ok) {
      const errText = await response.text();
      throw new BadGatewayException({
        code: 'OPENROUTER_ERROR',
        message: `OpenRouter ${response.status}: ${errText.slice(0, 500)}`,
      });
    }

    const body = (await response.json()) as {
      model?: string;
      choices?: { message?: { content?: string } }[];
    };
    const usedModel = body.model?.trim();
    if (usedModel && usedModel !== modelConfig.primary) {
      this.logger.warn(
        `OpenRouter used fallback model: requested=${modelConfig.primary} used=${usedModel}`,
      );
    }
    const content = body.choices?.[0]?.message?.content?.trim();
    if (!content) {
      throw new BadGatewayException({
        code: 'OPENROUTER_EMPTY',
        message: 'No content in OpenRouter response',
      });
    }
    return content;
  }

  private async fetchOpenRouter(
    headers: Record<string, string>,
    payload: Record<string, unknown>,
  ): Promise<Response> {
    try {
      return await fetch(OPENROUTER_URL, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
      });
    } catch (firstErr) {
      try {
        return await fetch(OPENROUTER_URL, {
          method: 'POST',
          headers,
          body: JSON.stringify(payload),
        });
      } catch {
        throw new BadGatewayException({
          code: 'OPENROUTER_ERROR',
          message: `OpenRouter network error: ${String(firstErr)}`,
        });
      }
    }
  }

  private parseChatResult(raw: string): {
    reply: string;
    actions: AssistantActionDto[];
  } {
    const jsonMatch = raw.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      return { reply: raw.trim(), actions: [] };
    }
    try {
      const parsed = JSON.parse(jsonMatch[0]) as {
        reply?: string;
        actions?: unknown[];
      };
      const reply =
        typeof parsed.reply === 'string' && parsed.reply.trim()
          ? parsed.reply.trim()
          : raw.trim();
      const actions = validateAssistantActions(parsed.actions ?? []);
      return { reply, actions };
    } catch {
      return { reply: raw.trim(), actions: [] };
    }
  }

  private parseCoachSignalsResult(raw: string): CoachSignalsResult {
    const jsonMatch = raw.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      return { signals: [] };
    }
    try {
      const parsed = JSON.parse(jsonMatch[0]) as {
        signals?: unknown[];
      };
      const allowedTypes = new Set<string>(COACH_SIGNAL_TYPES);
      const allowedSeverity = new Set(['low', 'medium', 'high']);
      const signals: CoachSignalDto[] = [];
      for (const item of parsed.signals ?? []) {
        if (!item || typeof item !== 'object') continue;
        const record = item as Record<string, unknown>;
        const type = String(record.type ?? '');
        if (!allowedTypes.has(type)) continue;
        const severityRaw = record.severity != null ? String(record.severity) : 'medium';
        const severity = allowedSeverity.has(severityRaw) ? severityRaw : 'medium';
        signals.push({
          type: type as CoachSignalDto['type'],
          taskId: record.taskId != null ? String(record.taskId) : undefined,
          habitId: record.habitId != null ? String(record.habitId) : undefined,
          severity,
        });
      }
      return { signals };
    } catch {
      return { signals: [] };
    }
  }

  private parseCoachResult(raw: string): CoachEvaluateResult {
    const jsonMatch = raw.match(/\{[\s\S]*\}/);
    if (!jsonMatch) {
      return { shouldNotify: false, rationale: 'invalid_json' };
    }
    try {
      const parsed = JSON.parse(jsonMatch[0]) as CoachEvaluateResult;
      return {
        shouldNotify: Boolean(parsed.shouldNotify),
        priority: parsed.priority,
        title: parsed.title,
        body: parsed.body,
        chatMessage: parsed.chatMessage,
        deepLink: parsed.deepLink,
        rationale: parsed.rationale,
      };
    } catch {
      return { shouldNotify: false, rationale: 'parse_error' };
    }
  }
}
