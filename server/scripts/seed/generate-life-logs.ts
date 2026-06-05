import { addMinutes, atLocalTime, uuid } from '../collections';
import type { DailyIntentRecord, SeedContext, TaskRecord } from './types';
import { chance, eachDay, parseDateKey, pickOne, randomInt } from './utils';

export function generateLifeLogs(
  ctx: SeedContext,
  intents: DailyIntentRecord[],
  tasks: TaskRecord[],
  eveningReviews: Record<string, unknown>[],
  skipDays: Set<string>,
): Record<string, unknown>[] {
  const { now, today, range, rng } = ctx;
  const logs: Record<string, unknown>[] = [];
  const intentByDate = new Map(intents.map((i) => [i.date, i]));
  const reviewByDate = new Map(
    eveningReviews.map((r) => [r.date as string, r._id as string]),
  );

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDays.has(dayKey)) continue;

    const dayDate = parseDateKey(dayKey);
    const intent = intentByDate.get(dayKey);

    if (intent) {
      logs.push({
        _id: uuid(),
        type: 'MORNING_UNLOCK',
        payload: { dailyIntentId: intent._id, nfcTagId: 'seed-nfc-tag-001' },
        timestamp: intent.completedAt,
        updatedAt: intent.updatedAt,
      });
    }

    const dayTasks = tasks.filter((t) => t.dayKey === dayKey);
    for (const task of dayTasks) {
      if (task.status === 'DONE' && chance(rng, 0.4)) {
        logs.push({
          _id: uuid(),
          type: 'TASK_STARTED',
          payload: { taskId: task._id },
          timestamp: task.scheduledAt,
          updatedAt: task.updatedAt,
        });
      }
      if (task.status === 'SKIPPED' && chance(rng, 0.5)) {
        logs.push({
          _id: uuid(),
          type: 'TASK_SKIPPED',
          payload: { taskId: task._id, reason: 'seed_skip' },
          timestamp: addMinutes(task.scheduledAt ?? dayDate, 30),
          updatedAt: task.updatedAt,
        });
      }
    }

    const reviewId = reviewByDate.get(dayKey);
    if (reviewId) {
      logs.push({
        _id: uuid(),
        type: 'EVENING_REVIEW',
        payload: { eveningReviewId: reviewId },
        timestamp: atLocalTime(dayDate, 21, 30),
        updatedAt: atLocalTime(dayDate, 21, 45),
      });
    }

    if (chance(rng, 0.25)) {
      logs.push({
        _id: uuid(),
        type: 'SYNC',
        payload: {
          direction: pickOne(rng, ['push', 'pull', 'push+pull']),
          counts: { tasks: dayTasks.length, habits: 5 },
        },
        timestamp: atLocalTime(dayDate, 12, 0),
        updatedAt: dayKey === today ? now : atLocalTime(dayDate, 12, 5),
      });
    }
  }

  return logs;
}

export function extraLifeLogsForVolume(
  ctx: SeedContext,
  baseCount: number,
  targetMin: number,
): Record<string, unknown>[] {
  const extra: Record<string, unknown>[] = [];
  const needed = Math.max(0, targetMin - baseCount);
  if (needed === 0) return extra;

  const { range, rng } = ctx;
  const types = ['SYNC', 'HABIT_CHECKIN', 'TASK_STARTED'] as const;

  const dayKeys = [...eachDay(range.fromKey, range.toKey)];
  for (let i = 0; i < needed; i++) {
    const dayKey = dayKeys[randomInt(rng, 0, dayKeys.length - 1)]!;
    const dayDate = parseDateKey(dayKey);
    extra.push({
      _id: uuid(),
      type: pickOne(rng, [...types]),
      payload: { seed: true, index: i },
      timestamp: atLocalTime(dayDate, 10, 0),
      updatedAt: atLocalTime(dayDate, 10, 0),
    });
  }

  return extra;
}
