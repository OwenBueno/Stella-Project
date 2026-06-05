import { atLocalTime, uuid } from '../collections';
import type { DailyIntentRecord, SeedContext } from './types';
import { chance, eachDay, parseDateKey, pickOne, randomInt } from './utils';

const USER_PROMPTS = [
  'What should I focus on first today?',
  'Help me prioritize my tasks.',
  'How did I do yesterday?',
  'Suggest a morning routine tweak.',
  'What habits need attention?',
];

const ASSISTANT_REPLIES = [
  'Start with your highest-priority block before noon.',
  'You have overdue items — tackle the oldest first.',
  'Your habit streak is solid; keep the morning routine.',
  'Batch admin tasks into a single 30-minute window.',
  'Consider a focus session for deep work this afternoon.',
];

export function generateAssistant(
  ctx: SeedContext,
  intents: DailyIntentRecord[],
  skipDays: Set<string>,
): { threads: Record<string, unknown>[]; messages: Record<string, unknown>[] } {
  const { now, today, range, rng } = ctx;
  const intentDates = new Set(intents.map((i) => i.date));
  const threads: Record<string, unknown>[] = [];
  const messages: Record<string, unknown>[] = [];

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDays.has(dayKey)) continue;
    if (!intentDates.has(dayKey)) continue;
    if (!chance(rng, 0.7)) continue;

    const dayDate = parseDateKey(dayKey);
    const threadId = uuid();
    const createdAt = atLocalTime(dayDate, 9 + Math.floor(rng() * 3), randomInt(rng, 0, 45));

    threads.push({
      _id: threadId,
      title: null,
      sessionDate: dayKey,
      createdAt,
      updatedAt: dayKey === today ? now : atLocalTime(dayDate, 18, 0),
      deletedAt: null,
    });

    const messageCount = randomInt(rng, 2, 4);
    for (let i = 0; i < messageCount; i++) {
      const isUser = i % 2 === 0;
      const msgTime = addMinutesSafe(createdAt, 5 + i * 8);
      messages.push({
        _id: uuid(),
        threadId,
        role: isUser ? 'user' : 'assistant',
        content: isUser ? pickOne(rng, USER_PROMPTS) : pickOne(rng, ASSISTANT_REPLIES),
        clientMessageId: isUser ? uuid() : null,
        metadata: undefined,
        createdAt: msgTime,
        updatedAt: msgTime,
        deletedAt: null,
      });
    }

    if (chance(rng, 0.05)) {
      const coachTime = atLocalTime(dayDate, 14, randomInt(rng, 0, 30));
      messages.push({
        _id: uuid(),
        threadId,
        role: 'assistant',
        content: '**Reminder:** You have tasks due soon. Check your frontline list.',
        clientMessageId: `coach:${dayKey}:TASK_UPCOMING::`,
        metadata: {
          source: 'coach_nudge',
          coachTitle: 'Tasks due soon',
          deepLink: 'assistant?context=upcoming',
          signalTypes: ['TASK_UPCOMING'],
          priority: 'medium',
        },
        createdAt: coachTime,
        updatedAt: coachTime,
        deletedAt: null,
      });
    }
  }

  return { threads, messages };
}

function addMinutesSafe(date: Date, minutes: number): Date {
  return new Date(date.getTime() + minutes * 60_000);
}
