import type { GeneratedSeedData, TodayFixtures } from './types';
import { dedupeByKey, localDateFromInstant } from './utils';

function mergeCheckIns(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
): Record<string, unknown>[] {
  return dedupeByKey([...bulk, ...fixtures], (c) => `${c.habitId}:${c.date}`);
}

function mergeByDate(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
): Record<string, unknown>[] {
  return dedupeByKey([...bulk, ...fixtures], (d) => d.date as string);
}

function mergeById(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
): Record<string, unknown>[] {
  return dedupeByKey([...bulk, ...fixtures], (d) => d._id as string);
}

function mergeTasks(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
  today: string,
): Record<string, unknown>[] {
  const withoutToday = bulk.filter((t) => {
    const scheduledAt = t.scheduledAt as Date | null;
    if (!scheduledAt) return true;
    return localDateFromInstant(scheduledAt) !== today;
  });
  return [...withoutToday, ...fixtures];
}

function mergeThreads(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
): Record<string, unknown>[] {
  return dedupeByKey([...bulk, ...fixtures], (t) => (t.sessionDate as string) ?? (t._id as string));
}

function mergeMessages(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
  fixtureThreadIds: Set<string>,
): Record<string, unknown>[] {
  const withoutFixtureThreads = bulk.filter((m) => !fixtureThreadIds.has(m.threadId as string));
  return [...withoutFixtureThreads, ...fixtures];
}

function mergeCalendar(
  bulk: Record<string, unknown>[],
  fixtures: Record<string, unknown>[],
  today: string,
): Record<string, unknown>[] {
  const withoutToday = bulk.filter((e) => {
    const startAt = e.startAt as Date;
    return localDateFromInstant(startAt) !== today;
  });
  return [...withoutToday, ...fixtures];
}

export function mergeSeedData(
  bulk: GeneratedSeedData,
  fixtures: TodayFixtures,
  today: string,
): GeneratedSeedData {
  const fixtureThreadIds = new Set(fixtures.assistantThreads.map((t) => t._id as string));

  return {
    habits: mergeById(bulk.habits, fixtures.habits),
    habitCheckIns: mergeCheckIns(bulk.habitCheckIns, fixtures.habitCheckIns),
    tasks: mergeTasks(bulk.tasks, fixtures.tasks, today),
    calendarEvents: mergeCalendar(bulk.calendarEvents, fixtures.calendarEvents, today),
    dailyIntents: mergeByDate(bulk.dailyIntents, fixtures.dailyIntents),
    eveningReviews: mergeByDate(bulk.eveningReviews, fixtures.eveningReviews),
    transactions: [...bulk.transactions, ...fixtures.transactions],
    debts: mergeById(bulk.debts, fixtures.debts),
    assistantThreads: mergeThreads(bulk.assistantThreads, fixtures.assistantThreads),
    assistantMessages: mergeMessages(
      bulk.assistantMessages,
      fixtures.assistantMessages,
      fixtureThreadIds,
    ),
    lifeLogs: [...bulk.lifeLogs, ...fixtures.lifeLogs],
  };
}

export function toInserts(data: GeneratedSeedData): Array<{ collection: string; docs: Record<string, unknown>[] }> {
  return [
    { collection: 'Habit', docs: data.habits },
    { collection: 'HabitCheckIn', docs: data.habitCheckIns },
    { collection: 'Task', docs: data.tasks },
    { collection: 'CalendarEvent', docs: data.calendarEvents },
    { collection: 'DailyIntent', docs: data.dailyIntents },
    { collection: 'EveningReview', docs: data.eveningReviews },
    { collection: 'Transaction', docs: data.transactions },
    { collection: 'Debt', docs: data.debts },
    { collection: 'AssistantThread', docs: data.assistantThreads },
    { collection: 'AssistantMessage', docs: data.assistantMessages },
    { collection: 'LifeLog', docs: data.lifeLogs },
  ];
}
