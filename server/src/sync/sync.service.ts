import { ConflictException, Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { mapDoc, mapDocs } from '../database/document.util';
import { CalendarEvent, CalendarEventDocument } from '../database/schemas/calendar-event.schema';
import { DailyIntent, DailyIntentDocument } from '../database/schemas/daily-intent.schema';
import { EveningReview, EveningReviewDocument } from '../database/schemas/evening-review.schema';
import { Habit, HabitDocument } from '../database/schemas/habit.schema';
import { HabitCheckIn, HabitCheckInDocument } from '../database/schemas/habit-check-in.schema';
import { LifeLog, LifeLogDocument } from '../database/schemas/life-log.schema';
import { Task, TaskDocument } from '../database/schemas/task.schema';
import { Transaction, TransactionDocument } from '../database/schemas/transaction.schema';
import { Debt, DebtDocument } from '../database/schemas/debt.schema';
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
import { isIncomingNewer } from '../common/sync/lww.util';
import {
  SyncAssistantMessageDto,
  SyncAssistantThreadDto,
  SyncCheckInDto,
  SyncDailyIntentDto,
  SyncDebtDto,
  SyncEventDto,
  SyncEveningReviewDto,
  SyncHabitDto,
  SyncLifeLogDto,
  SyncPushDto,
  SyncTaskDto,
  SyncTransactionDto,
} from './dto/sync-push.dto';

@Injectable()
export class SyncService {
  constructor(
    @InjectModel(Habit.name) private readonly habitModel: Model<HabitDocument>,
    @InjectModel(HabitCheckIn.name)
    private readonly checkInModel: Model<HabitCheckInDocument>,
    @InjectModel(Task.name) private readonly taskModel: Model<TaskDocument>,
    @InjectModel(CalendarEvent.name)
    private readonly eventModel: Model<CalendarEventDocument>,
    @InjectModel(DailyIntent.name)
    private readonly dailyIntentModel: Model<DailyIntentDocument>,
    @InjectModel(EveningReview.name)
    private readonly eveningReviewModel: Model<EveningReviewDocument>,
    @InjectModel(LifeLog.name) private readonly lifeLogModel: Model<LifeLogDocument>,
    @InjectModel(Transaction.name)
    private readonly transactionModel: Model<TransactionDocument>,
    @InjectModel(Debt.name) private readonly debtModel: Model<DebtDocument>,
    @InjectModel(AssistantThread.name)
    private readonly assistantThreadModel: Model<AssistantThreadDocument>,
    @InjectModel(AssistantMessage.name)
    private readonly assistantMessageModel: Model<AssistantMessageDocument>,
  ) {}

  async push(dto: SyncPushDto) {
    let accepted = 0;
    const conflicts: Array<{
      entity: string;
      id: string;
      serverDocument: unknown;
    }> = [];

    for (const habit of dto.habits) {
      const result = await this.upsertHabit(habit);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const checkIn of dto.habitCheckIns) {
      await this.upsertCheckIn(checkIn);
      accepted++;
    }

    for (const task of dto.tasks) {
      const result = await this.upsertTask(task);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const event of dto.events) {
      const result = await this.upsertEvent(event);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const intent of dto.dailyIntents ?? []) {
      const result = await this.upsertDailyIntent(intent);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const review of dto.eveningReviews ?? []) {
      const result = await this.upsertEveningReview(review);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const log of dto.lifeLogs ?? []) {
      const result = await this.upsertLifeLog(log);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const tx of dto.transactions ?? []) {
      const result = await this.upsertTransaction(tx);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const debt of dto.debts ?? []) {
      const result = await this.upsertDebt(debt);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const thread of dto.assistantThreads ?? []) {
      const result = await this.upsertAssistantThread(thread);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    for (const message of dto.assistantMessages ?? []) {
      const result = await this.upsertAssistantMessage(message);
      if (result === 'accepted') accepted++;
      else if (result.conflict) conflicts.push(result.conflict);
    }

    return { accepted, conflicts };
  }

  private static readonly SYNC_ENTITIES = [
    'habits',
    'habitCheckIns',
    'tasks',
    'events',
    'dailyIntents',
    'eveningReviews',
    'lifeLogs',
    'transactions',
    'debts',
    'assistantThreads',
    'assistantMessages',
  ] as const;

  async pull(since?: string, entity?: string, limit?: string, cursor?: string) {
    if (entity) {
      return this.pullEntity(since, entity, limit, cursor);
    }
    const sinceDate = since ? new Date(since) : new Date(0);
    const serverTime = new Date().toISOString();
    const filter = { updatedAt: { $gt: sinceDate } };

    const [
      habits,
      habitCheckIns,
      tasks,
      events,
      dailyIntents,
      eveningReviews,
      lifeLogs,
      transactions,
      debts,
      assistantThreads,
      assistantMessages,
    ] = await Promise.all([
      this.habitModel.find(filter).exec(),
      this.checkInModel.find(filter).exec(),
      this.taskModel.find(filter).exec(),
      this.eventModel.find(filter).exec(),
      this.dailyIntentModel.find(filter).exec(),
      this.eveningReviewModel.find(filter).exec(),
      this.lifeLogModel.find(filter).exec(),
      this.transactionModel.find(filter).exec(),
      this.debtModel.find(filter).exec(),
      this.assistantThreadModel.find(filter).exec(),
      this.assistantMessageModel.find(filter).exec(),
    ]);

    return {
      serverTime,
      habits: mapDocs(habits),
      habitCheckIns: mapDocs(habitCheckIns),
      tasks: mapDocs(tasks),
      events: mapDocs(events),
      dailyIntents: mapDocs(dailyIntents),
      eveningReviews: mapDocs(eveningReviews),
      lifeLogs: mapDocs(lifeLogs).map((log) => this.mapLifeLogForSync(log)),
      transactions: mapDocs(transactions),
      debts: mapDocs(debts),
      assistantThreads: mapDocs(assistantThreads),
      assistantMessages: mapDocs(assistantMessages),
      nextCursor: null,
      hasMore: false,
    };
  }

  private async pullEntity(
    since: string | undefined,
    entity: string,
    limit?: string,
    cursor?: string,
  ) {
    if (!SyncService.SYNC_ENTITIES.includes(entity as (typeof SyncService.SYNC_ENTITIES)[number])) {
      return {
        serverTime: new Date().toISOString(),
        entity,
        items: [],
        nextCursor: null,
        hasMore: false,
      };
    }
    const sinceDate = since ? new Date(since) : new Date(0);
    const baseFilter = { updatedAt: { $gt: sinceDate } };
    const pageLimit = clampLimit(limit);
    const filter = cursorFilter(baseFilter, cursor);

    const docs = await this.findEntityPage(entity, filter, pageLimit + 1);
    let mapped = mapDocs(docs);
    if (entity === 'lifeLogs') {
      mapped = mapped.map((log) => this.mapLifeLogForSync(log));
    }
    const page = buildPaginatedResult(
      mapped,
      pageLimit,
      (i) => i.id as string,
      (i) => new Date(i.updatedAt as string),
    );
    return {
      serverTime: page.serverTime,
      entity,
      items: page.items,
      nextCursor: page.nextCursor,
      hasMore: page.nextCursor != null,
    };
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private async findEntityPage(entity: string, filter: Record<string, unknown>, limit: number): Promise<any[]> {
    const sort = { updatedAt: 1 as const, _id: 1 as const };
    switch (entity) {
      case 'habits':
        return this.habitModel.find(filter).sort(sort).limit(limit).exec();
      case 'habitCheckIns':
        return this.checkInModel.find(filter).sort(sort).limit(limit).exec();
      case 'tasks':
        return this.taskModel.find(filter).sort(sort).limit(limit).exec();
      case 'events':
        return this.eventModel.find(filter).sort(sort).limit(limit).exec();
      case 'dailyIntents':
        return this.dailyIntentModel.find(filter).sort(sort).limit(limit).exec();
      case 'eveningReviews':
        return this.eveningReviewModel.find(filter).sort(sort).limit(limit).exec();
      case 'lifeLogs':
        return this.lifeLogModel.find(filter).sort(sort).limit(limit).exec();
      case 'transactions':
        return this.transactionModel.find(filter).sort(sort).limit(limit).exec();
      case 'debts':
        return this.debtModel.find(filter).sort(sort).limit(limit).exec();
      case 'assistantThreads':
        return this.assistantThreadModel.find(filter).sort(sort).limit(limit).exec();
      case 'assistantMessages':
        return this.assistantMessageModel.find(filter).sort(sort).limit(limit).exec();
      default:
        return [];
    }
  }

  /** API contract: payload is a JSON string (Room / Retrofit), not a Mixed object. */
  private mapLifeLogForSync(doc: Record<string, unknown>): Record<string, unknown> {
    const timestamp = doc.timestamp;
    const updatedAt = doc.updatedAt;
    return {
      ...doc,
      payload: this.serializeLifeLogPayload(doc.payload),
      timestamp:
        timestamp instanceof Date ? timestamp.toISOString() : String(timestamp),
      updatedAt:
        updatedAt instanceof Date ? updatedAt.toISOString() : String(updatedAt),
    };
  }

  private serializeLifeLogPayload(payload: unknown): string {
    if (typeof payload === 'string') return payload;
    return JSON.stringify(payload ?? {});
  }

  private async upsertHabit(
    dto: SyncHabitDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.habitModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: { entity: 'habit', id: dto.id, serverDocument: mapDoc(existing) },
      };
    }

    const data = {
      name: dto.name,
      sortOrder: dto.sortOrder,
      active: dto.active,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.habitModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.habitModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertCheckIn(dto: SyncCheckInDto) {
    const existing = await this.checkInModel
      .findOne({ habitId: dto.habitId, date: dto.date })
      .exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      throw new ConflictException({
        code: 'SYNC_CONFLICT',
        message: 'Check-in conflict',
        serverDocument: mapDoc(existing),
      });
    }
    const completedAt =
      dto.status === 'DONE' && dto.completedAt
        ? new Date(dto.completedAt)
        : dto.status === 'DONE'
          ? new Date(dto.updatedAt)
          : null;
    if (existing) {
      await this.checkInModel
        .updateOne(
          { _id: existing._id },
          {
            status: dto.status,
            completedAt,
            updatedAt: new Date(dto.updatedAt),
          },
        )
        .exec();
    } else {
      await this.checkInModel.create({
        _id: dto.id,
        habitId: dto.habitId,
        date: dto.date,
        status: dto.status,
        completedAt,
        updatedAt: new Date(dto.updatedAt),
      });
    }
  }

  private async upsertTask(
    dto: SyncTaskDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.taskModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: { entity: 'task', id: dto.id, serverDocument: mapDoc(existing) },
      };
    }

    const data = {
      title: dto.title,
      notes: dto.notes ?? null,
      scheduledAt: dto.scheduledAt ? new Date(dto.scheduledAt) : null,
      durationMinutes: dto.durationMinutes ?? null,
      status: dto.status,
      sortOrder: dto.sortOrder ?? 0,
      priority: dto.priority ?? null,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.taskModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.taskModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertEvent(
    dto: SyncEventDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.eventModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: { entity: 'event', id: dto.id, serverDocument: mapDoc(existing) },
      };
    }

    const data = {
      title: dto.title,
      startAt: new Date(dto.startAt),
      endAt: new Date(dto.endAt),
      linkedTaskId: dto.linkedTaskId ?? null,
      recurrenceRuleJson: dto.recurrenceRuleJson ?? null,
      reminderOffsetsJson: dto.reminderOffsetsJson ?? null,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.eventModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.eventModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertDailyIntent(
    dto: SyncDailyIntentDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.dailyIntentModel.findOne({ date: dto.date }).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: {
          entity: 'dailyIntent',
          id: dto.id,
          serverDocument: mapDoc(existing),
        },
      };
    }

    const data = {
      plannedTaskIds: dto.plannedTaskIds,
      completedAt: new Date(dto.completedAt),
      nfcTagId: dto.nfcTagId,
      updatedAt: new Date(dto.updatedAt),
    };

    if (existing) {
      await this.dailyIntentModel.updateOne({ date: dto.date }, data).exec();
    } else {
      await this.dailyIntentModel.create({ _id: dto.id, date: dto.date, ...data });
    }
    return 'accepted';
  }

  private async upsertEveningReview(
    dto: SyncEveningReviewDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.eveningReviewModel.findOne({ date: dto.date }).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: {
          entity: 'eveningReview',
          id: dto.id,
          serverDocument: mapDoc(existing),
        },
      };
    }

    const data = {
      plannedVsActual: dto.plannedVsActual ?? null,
      reflectionText: dto.reflectionText ?? null,
      habitGridSnapshot: JSON.parse(dto.habitGridSnapshot) as Record<string, unknown>,
      completedAt: new Date(dto.completedAt),
      updatedAt: new Date(dto.updatedAt),
    };

    if (existing) {
      await this.eveningReviewModel.updateOne({ date: dto.date }, data).exec();
    } else {
      await this.eveningReviewModel.create({ _id: dto.id, date: dto.date, ...data });
    }
    return 'accepted';
  }

  private async upsertLifeLog(
    dto: SyncLifeLogDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.lifeLogModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: {
          entity: 'lifeLog',
          id: dto.id,
          serverDocument: this.mapLifeLogForSync(mapDoc(existing)!),
        },
      };
    }

    const data = {
      type: dto.type,
      payload: JSON.parse(dto.payload) as Record<string, unknown>,
      timestamp: new Date(dto.timestamp),
      updatedAt: new Date(dto.updatedAt),
    };

    if (existing) {
      await this.lifeLogModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.lifeLogModel.create({ _id: dto.id, ...data });
    }
    return 'accepted';
  }

  private async upsertTransaction(
    dto: SyncTransactionDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.transactionModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: { entity: 'transaction', id: dto.id, serverDocument: mapDoc(existing) },
      };
    }

    const data = {
      type: dto.type,
      amount: dto.amount,
      category: dto.category,
      description: dto.description ?? null,
      date: new Date(dto.date),
      linkedTaskId: dto.linkedTaskId ?? null,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.transactionModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.transactionModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertDebt(
    dto: SyncDebtDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.debtModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: { entity: 'debt', id: dto.id, serverDocument: mapDoc(existing) },
      };
    }

    const data = {
      contactName: dto.contactName,
      direction: dto.direction,
      totalAmount: dto.totalAmount,
      remainingAmount: dto.remainingAmount,
      dueDate: dto.dueDate ? new Date(dto.dueDate) : null,
      notes: dto.notes ?? null,
      isResolved: dto.isResolved,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.debtModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.debtModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertAssistantThread(
    dto: SyncAssistantThreadDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.assistantThreadModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: {
          entity: 'assistantThread',
          id: dto.id,
          serverDocument: mapDoc(existing),
        },
      };
    }

    const data = {
      title: dto.title ?? null,
      sessionDate: dto.sessionDate ?? null,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.assistantThreadModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.assistantThreadModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }

  private async upsertAssistantMessage(
    dto: SyncAssistantMessageDto,
  ): Promise<'accepted' | { conflict: { entity: string; id: string; serverDocument: unknown } }> {
    const existing = await this.assistantMessageModel.findById(dto.id).exec();
    if (existing && !isIncomingNewer(dto.updatedAt, existing.updatedAt)) {
      return {
        conflict: {
          entity: 'assistantMessage',
          id: dto.id,
          serverDocument: mapDoc(existing),
        },
      };
    }

    const data = {
      threadId: dto.threadId,
      role: dto.role,
      content: dto.content,
      clientMessageId: dto.clientMessageId ?? null,
      metadata: dto.metadata ?? null,
      updatedAt: new Date(dto.updatedAt),
      deletedAt: dto.deletedAt ? new Date(dto.deletedAt) : null,
    };

    if (existing) {
      await this.assistantMessageModel.updateOne({ _id: dto.id }, data).exec();
    } else {
      await this.assistantMessageModel.create({
        _id: dto.id,
        ...data,
        createdAt: new Date(dto.createdAt),
      });
    }
    return 'accepted';
  }
}
