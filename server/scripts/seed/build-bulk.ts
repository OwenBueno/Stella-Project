import { generateCheckInLifeLogs, generateCheckIns } from './generate-check-ins';
import { generateCalendarEvents } from './generate-calendar';
import { generateDailyIntents } from './generate-daily-intents';
import { generateEveningReviews } from './generate-evening-reviews';
import { generateFinances } from './generate-finances';
import { generateHabits } from './generate-habits';
import { extraLifeLogsForVolume, generateLifeLogs } from './generate-life-logs';
import { generateAssistant } from './generate-assistant';
import { generateTasks, skippedTasks, tasksToDocs } from './generate-tasks';
import { mergeSeedData } from './merge';
import type { GeneratedSeedData, SeedContext, TodayFixtures } from './types';
import { countDocs } from './utils';

const FIXTURE_SKIP_DAYS = new Set<string>();

export function buildBulkSeed(ctx: SeedContext, fixtures: TodayFixtures): GeneratedSeedData {
  const { today, options } = ctx;
  const skipDays = new Set([today, ...FIXTURE_SKIP_DAYS]);
  const skipCheckInDates = new Set([
    today,
    ...fixtures.habitCheckIns.map((c) => c.date as string),
  ]);
  const skipReviewDates = new Set([
    ...fixtures.eveningReviews.map((r) => r.date as string),
  ]);

  let tasksPerDay = 4;
  let data: GeneratedSeedData | null = null;

  for (let attempt = 0; attempt < 3; attempt++) {
    const habits = generateHabits(ctx);
    const habitCheckIns = generateCheckIns(ctx, habits, skipCheckInDates);
    const { tasks, byDay } = generateTasks(ctx, skipDays, tasksPerDay);
    const dailyIntents = generateDailyIntents(ctx, byDay, skipDays);
    const eveningReviews = generateEveningReviews(
      ctx,
      dailyIntents,
      habits,
      habitCheckIns,
      skipReviewDates,
    );
    const calendarEvents = generateCalendarEvents(ctx, byDay, skipDays);
    const { transactions, debts } = generateFinances(ctx, skippedTasks(tasks));
    const assistant = generateAssistant(ctx, dailyIntents, skipDays);

    let lifeLogs = [
      ...generateLifeLogs(ctx, dailyIntents, tasks, eveningReviews, skipDays),
      ...generateCheckInLifeLogs(ctx, habitCheckIns),
    ];

    data = {
      habits,
      habitCheckIns,
      tasks: tasksToDocs(tasks),
      calendarEvents,
      dailyIntents,
      eveningReviews,
      transactions,
      debts,
      assistantThreads: assistant.threads,
      assistantMessages: assistant.messages,
      lifeLogs,
    };

    const merged = mergeSeedData(data, fixtures, today);
    const total = countDocs(merged as unknown as Record<string, Record<string, unknown>[]>);

    if (total >= options.minDocs) {
      const extraNeeded = options.minDocs - countDocs(data as unknown as Record<string, Record<string, unknown>[]>);
      if (extraNeeded > 0) {
        data.lifeLogs = [...lifeLogs, ...extraLifeLogsForVolume(ctx, lifeLogs.length, lifeLogs.length + extraNeeded)];
      }
      return data;
    }

    tasksPerDay += 1;
  }

  if (!data) {
    throw new Error('Failed to generate seed data');
  }

  const currentLifeLogs = data.lifeLogs.length;
  const merged = mergeSeedData(data, fixtures, today);
  const total = countDocs(merged as unknown as Record<string, Record<string, unknown>[]>);
  const extraNeeded = Math.max(0, options.minDocs - total);
  if (extraNeeded > 0) {
    data.lifeLogs = [
      ...data.lifeLogs,
      ...extraLifeLogsForVolume(ctx, currentLifeLogs, currentLifeLogs + extraNeeded),
    ];
  }

  return data;
}
