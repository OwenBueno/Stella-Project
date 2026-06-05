import { atLocalTime, uuid } from '../collections';
import type { HabitRecord, SeedContext } from './types';
import { activeHabitIds } from './generate-habits';
import { addLocalDays, chance, eachDay, localDateKey, parseDateKey, pickWeighted } from './utils';

export function generateCheckIns(
  ctx: SeedContext,
  habits: HabitRecord[],
  skipDates: Set<string>,
): Record<string, unknown>[] {
  const { now, today, range, rng } = ctx;
  const activeIds = activeHabitIds(habits);
  const checkIns: Record<string, unknown>[] = [];

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDates.has(dayKey)) continue;
    const dayDate = parseDateKey(dayKey);
    const isPast = dayKey < today;

    for (const habitId of activeIds) {
      const status = pickWeighted(rng, [
        { value: 'DONE', weight: 0.68 },
        { value: 'MISSED', weight: 0.32 },
      ]);

      checkIns.push({
        _id: uuid(),
        habitId,
        date: dayKey,
        status,
        completedAt:
          status === 'DONE' && isPast ? atLocalTime(dayDate, 8 + Math.floor(rng() * 4), 15) : null,
        updatedAt: isPast ? atLocalTime(dayDate, 23, 0) : now,
      });
    }
  }

  return checkIns;
}

export function buildHabitGridCells(
  habits: HabitRecord[],
  checkIns: Record<string, unknown>[],
  weekStartKey: string,
): Array<{ habitId: string; date: string; status: string }> {
  const activeIds = activeHabitIds(habits);
  const cells: Array<{ habitId: string; date: string; status: string }> = [];
  const weekStart = parseDateKey(weekStartKey);

  for (let offset = 0; offset < 7; offset++) {
    const date = addLocalDays(weekStart, offset);
    const dateStr = localDateKey(date);
    for (const habitId of activeIds) {
      const match = checkIns.find((c) => c.habitId === habitId && c.date === dateStr);
      cells.push({
        habitId,
        date: dateStr,
        status: (match?.status as string) ?? 'MISSED',
      });
    }
  }

  return cells;
}

export function generateCheckInLifeLogs(
  ctx: SeedContext,
  checkIns: Record<string, unknown>[],
): Record<string, unknown>[] {
  const logs: Record<string, unknown>[] = [];
  for (const checkIn of checkIns) {
    if (!chance(ctx.rng, 0.15)) continue;
    logs.push({
      _id: uuid(),
      type: 'HABIT_CHECKIN',
      payload: {
        habitId: checkIn.habitId,
        date: checkIn.date,
        status: checkIn.status,
      },
      timestamp: checkIn.updatedAt,
      updatedAt: checkIn.updatedAt,
    });
  }
  return logs;
}
