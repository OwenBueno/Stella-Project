import {
  addDays,
  atLocalTime,
  mondayOfWeek,
  uuid,
} from '../collections';
import type { GeneratedSeedData, SeedContext, TodayFixtureIds, TodayFixtures } from './types';
import { offsetDateKey, parseDateKey } from './utils';

export const FIXTURE_IDS: TodayFixtureIds = {
  habitIds: {
    morning: 'a1111111-1111-4111-8111-111111111101',
    exercise: 'a1111111-1111-4111-8111-111111111102',
    reading: 'a1111111-1111-4111-8111-111111111103',
    meditation: 'a1111111-1111-4111-8111-111111111104',
  },
  taskIds: {
    overdue: 'b2222222-2222-4222-8222-222222222201',
    upcoming: 'b2222222-2222-4222-8222-222222222202',
    deepWork: 'b2222222-2222-4222-8222-222222222203',
    admin: 'b2222222-2222-4222-8222-222222222204',
    review: 'b2222222-2222-4222-8222-222222222205',
    done: 'b2222222-2222-4222-8222-222222222206',
  },
  threadId: 'c3333333-3333-4333-8333-333333333301',
  userMessageId: 'c3333333-3333-4333-8333-333333333302',
  coachMessageId: 'c3333333-3333-4333-8333-333333333303',
  dailyIntentId: 'd4444444-4444-4444-8444-444444444401',
  eveningReviewId: 'd4444444-4444-4444-8444-444444444402',
  calendarEventId: 'e5555555-5555-4555-8555-555555555501',
  debtId: 'f6666666-6666-4666-8666-666666666601',
  yesterdayThreadId: 'c3333333-3333-4333-8333-333333333304',
};

export function buildTodayFixtures(ctx: SeedContext): TodayFixtures {
  const { now, today } = ctx;
  const ids = FIXTURE_IDS;
  const yesterday = offsetDateKey(today, -1);
  const twoDaysAgo = offsetDateKey(today, -2);
  const threeDaysAgo = offsetDateKey(today, -3);
  const tomorrow = offsetDateKey(today, 1);
  const yesterdayDate = parseDateKey(yesterday);
  const tomorrowDate = parseDateKey(tomorrow);
  const weekStart = mondayOfWeek(now);

  const habits: GeneratedSeedData['habits'] = [
    {
      _id: ids.habitIds.morning,
      name: 'Morning routine',
      sortOrder: 0,
      active: true,
      createdAt: addDays(now, -30),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.habitIds.exercise,
      name: 'Exercise',
      sortOrder: 1,
      active: true,
      createdAt: addDays(now, -30),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.habitIds.reading,
      name: 'Read 30 min',
      sortOrder: 2,
      active: true,
      createdAt: addDays(now, -30),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.habitIds.meditation,
      name: 'Meditation',
      sortOrder: 3,
      active: false,
      createdAt: addDays(now, -30),
      updatedAt: now,
      deletedAt: null,
    },
  ];

  const checkInDates = [threeDaysAgo, twoDaysAgo, yesterday];
  const checkInStatuses: Record<string, string[]> = {
    [ids.habitIds.morning]: ['DONE', 'DONE', 'MISSED'],
    [ids.habitIds.exercise]: ['MISSED', 'DONE', 'MISSED'],
    [ids.habitIds.reading]: ['DONE', 'MISSED', 'DONE'],
  };

  const habitCheckIns = Object.entries(checkInStatuses).flatMap(([habitId, statuses]) =>
    statuses.map((status, index) => ({
      _id: uuid(),
      habitId,
      date: checkInDates[index],
      status,
      completedAt: status === 'DONE' ? addDays(now, index - 2) : null,
      updatedAt: now,
    })),
  );

  const tasks: GeneratedSeedData['tasks'] = [
    {
      _id: ids.taskIds.overdue,
      title: 'Submit expense report',
      notes: 'Overdue (yesterday) — coach TASK_OVERDUE when evaluated live',
      scheduledAt: atLocalTime(yesterdayDate, 10, 0),
      durationMinutes: 45,
      status: 'TODO',
      sortOrder: 0,
      priority: 'HIGH',
      createdAt: addDays(now, -3),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.taskIds.upcoming,
      title: 'Team standup prep',
      notes: 'Scheduled tomorrow — no takeover until that local day',
      scheduledAt: atLocalTime(tomorrowDate, 9, 0),
      durationMinutes: 25,
      status: 'TODO',
      sortOrder: 1,
      priority: 'MEDIUM',
      createdAt: addDays(now, -1),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.taskIds.deepWork,
      title: 'Deep work block',
      notes: null,
      scheduledAt: atLocalTime(yesterdayDate, 14, 0),
      durationMinutes: 90,
      status: 'TODO',
      sortOrder: 2,
      priority: 'HIGH',
      createdAt: addDays(now, -2),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.taskIds.admin,
      title: 'Inbox zero',
      notes: null,
      scheduledAt: atLocalTime(yesterdayDate, 16, 30),
      durationMinutes: 30,
      status: 'TODO',
      sortOrder: 3,
      priority: 'LOW',
      createdAt: addDays(now, -1),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.taskIds.review,
      title: 'Plan tomorrow',
      notes: null,
      scheduledAt: atLocalTime(tomorrowDate, 20, 0),
      durationMinutes: 20,
      status: 'TODO',
      sortOrder: 4,
      priority: 'MEDIUM',
      createdAt: addDays(now, -1),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.taskIds.done,
      title: 'Morning email sweep',
      notes: null,
      scheduledAt: atLocalTime(yesterdayDate, 8, 0),
      durationMinutes: 15,
      status: 'DONE',
      sortOrder: 5,
      priority: 'LOW',
      createdAt: addDays(now, -1),
      updatedAt: now,
      deletedAt: null,
    },
  ];

  const calendarEvents: GeneratedSeedData['calendarEvents'] = [
    {
      _id: ids.calendarEventId,
      title: 'Focus block',
      startAt: atLocalTime(yesterdayDate, 14, 0),
      endAt: atLocalTime(yesterdayDate, 15, 30),
      linkedTaskId: ids.taskIds.deepWork,
      recurrenceRuleJson: null,
      reminderOffsetsJson: JSON.stringify([15]),
      createdAt: addDays(now, -5),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: uuid(),
      title: '1:1 with Alex',
      startAt: atLocalTime(tomorrowDate, 11, 0),
      endAt: atLocalTime(tomorrowDate, 11, 30),
      linkedTaskId: null,
      recurrenceRuleJson: null,
      reminderOffsetsJson: JSON.stringify([10]),
      createdAt: addDays(now, -5),
      updatedAt: now,
      deletedAt: null,
    },
  ];

  const dailyIntents: GeneratedSeedData['dailyIntents'] = [
    {
      _id: ids.dailyIntentId,
      date: yesterday,
      plannedTaskIds: [
        ids.taskIds.overdue,
        ids.taskIds.deepWork,
        ids.taskIds.admin,
        ids.taskIds.done,
      ],
      completedAt: atLocalTime(yesterdayDate, 7, 15),
      nfcTagId: 'seed-nfc-tag-001',
      updatedAt: now,
    },
  ];

  const habitGridCells = habits
    .filter((h) => h.active)
    .flatMap((habit) =>
      checkInDates.map((date) => {
        const match = habitCheckIns.find((c) => c.habitId === habit._id && c.date === date);
        return {
          habitId: habit._id as string,
          date,
          status: (match?.status as string) ?? 'MISSED',
        };
      }),
    );

  const eveningReviews: GeneratedSeedData['eveningReviews'] = [
    {
      _id: ids.eveningReviewId,
      date: yesterday,
      plannedVsActual: 'Planned 4 tasks, completed 2. Exercise missed.',
      reflectionText: 'Need tighter morning block.',
      habitGridSnapshot: {
        weekStart,
        cells: habitGridCells,
      },
      completedAt: atLocalTime(addDays(now, -1), 21, 30),
      updatedAt: addDays(now, -1),
    },
  ];

  const transactions: GeneratedSeedData['transactions'] = [
    {
      _id: uuid(),
      type: 'ingress',
      amount: 3200,
      category: 'Salary',
      description: 'Monthly salary (seed)',
      date: addDays(now, -5),
      linkedTaskId: null,
      createdAt: addDays(now, -5),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: uuid(),
      type: 'egress',
      amount: 42.5,
      category: 'Food',
      description: 'Groceries',
      date: addDays(now, -1),
      linkedTaskId: null,
      createdAt: addDays(now, -1),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: uuid(),
      type: 'egress',
      amount: 15,
      category: 'Penalty',
      description: 'Skipped task penalty (ledger only)',
      date: addDays(now, -2),
      linkedTaskId: ids.taskIds.done,
      createdAt: addDays(now, -2),
      updatedAt: now,
      deletedAt: null,
    },
  ];

  const debts: GeneratedSeedData['debts'] = [
    {
      _id: ids.debtId,
      contactName: 'Jordan',
      direction: 'owed_by_me',
      totalAmount: 120,
      remainingAmount: 80,
      dueDate: addDays(now, 2),
      notes: 'Dinner split — DEBT_DUE_SOON signal',
      isResolved: false,
      createdAt: addDays(now, -10),
      updatedAt: now,
      deletedAt: null,
    },
  ];

  const assistantThreads: GeneratedSeedData['assistantThreads'] = [
    {
      _id: ids.threadId,
      title: null,
      sessionDate: yesterday,
      createdAt: atLocalTime(yesterdayDate, 9, 0),
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: ids.yesterdayThreadId,
      title: null,
      sessionDate: twoDaysAgo,
      createdAt: atLocalTime(parseDateKey(twoDaysAgo), 10, 0),
      updatedAt: addDays(now, -2),
      deletedAt: null,
    },
  ];

  const coachClientMessageId = `coach:${yesterday}:TASK_OVERDUE::|EVENING_REVIEW_DUE::`;

  const assistantMessages: GeneratedSeedData['assistantMessages'] = [
    {
      _id: ids.userMessageId,
      threadId: ids.threadId,
      role: 'user',
      content: 'What should I focus on first after syncing seed data?',
      clientMessageId: uuid(),
      metadata: undefined,
      createdAt: atLocalTime(yesterdayDate, 9, 5),
      updatedAt: atLocalTime(yesterdayDate, 9, 5),
      deletedAt: null,
    },
    {
      _id: uuid(),
      threadId: ids.yesterdayThreadId,
      role: 'assistant',
      content: 'Two days ago you finished 2 of 4 planned tasks. Start with the hardest block first.',
      clientMessageId: null,
      metadata: undefined,
      createdAt: atLocalTime(parseDateKey(twoDaysAgo), 10, 6),
      updatedAt: atLocalTime(parseDateKey(twoDaysAgo), 10, 6),
      deletedAt: null,
    },
    {
      _id: ids.coachMessageId,
      threadId: ids.threadId,
      role: 'assistant',
      content:
        '**Submit expense report is overdue** (scheduled yesterday).\n\n**Reminders**\n- Overdue: Submit expense report\n- Upcoming: Team standup prep (tomorrow)\n- Habits missed yesterday: Morning routine, Exercise',
      clientMessageId: coachClientMessageId,
      metadata: {
        source: 'coach_nudge',
        coachTitle: 'Expense report overdue',
        deepLink: 'assistant?context=overdue',
        signalTypes: ['TASK_OVERDUE', 'TASK_UPCOMING', 'HABIT_RED_TODAY'],
        priority: 'high',
      },
      createdAt: atLocalTime(yesterdayDate, 18, 50),
      updatedAt: atLocalTime(yesterdayDate, 18, 50),
      deletedAt: null,
    },
  ];

  const lifeLogs: GeneratedSeedData['lifeLogs'] = [
    {
      _id: uuid(),
      type: 'MORNING_UNLOCK',
      payload: { dailyIntentId: ids.dailyIntentId, nfcTagId: 'seed-nfc-tag-001' },
      timestamp: atLocalTime(yesterdayDate, 7, 15),
      updatedAt: now,
    },
    {
      _id: uuid(),
      type: 'COACH_NUDGE',
      payload: {
        title: 'Expense report overdue',
        deepLink: 'assistant?context=overdue',
        signalTypes: ['TASK_OVERDUE', 'TASK_UPCOMING'],
      },
      timestamp: atLocalTime(yesterdayDate, 18, 50),
      updatedAt: now,
    },
    {
      _id: uuid(),
      type: 'SYNC',
      payload: { direction: 'pull', counts: { tasks: tasks.length, habits: habits.length } },
      timestamp: atLocalTime(yesterdayDate, 12, 0),
      updatedAt: now,
    },
    {
      _id: uuid(),
      type: 'EVENING_REVIEW',
      payload: { eveningReviewId: ids.eveningReviewId },
      timestamp: atLocalTime(addDays(now, -1), 21, 30),
      updatedAt: addDays(now, -1),
    },
  ];

  return {
    ids,
    habits,
    habitCheckIns,
    tasks,
    calendarEvents,
    dailyIntents,
    eveningReviews,
    transactions,
    debts,
    assistantThreads,
    assistantMessages,
    lifeLogs,
  };
}
