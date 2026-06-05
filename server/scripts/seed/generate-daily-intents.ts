import { atLocalTime, uuid } from '../collections';
import type { DailyIntentRecord, SeedContext, TaskRecord } from './types';
import { chance, eachDay } from './utils';

export function generateDailyIntents(
  ctx: SeedContext,
  tasksByDay: Map<string, TaskRecord[]>,
  skipDays: Set<string>,
): DailyIntentRecord[] {
  const { now, today, range, rng } = ctx;
  const intents: DailyIntentRecord[] = [];

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDays.has(dayKey)) continue;
    if (!chance(rng, 0.88)) continue;

    const dayTasks = tasksByDay.get(dayKey) ?? [];
    if (dayTasks.length < 3) continue;

    const plannedCount = Math.min(dayTasks.length, 3 + Math.floor(rng() * 3));
    const plannedTaskIds = dayTasks.slice(0, plannedCount).map((t) => t._id);
    const dayDate = new Date(`${dayKey}T12:00:00`);

    intents.push({
      _id: uuid(),
      date: dayKey,
      plannedTaskIds,
      completedAt: atLocalTime(dayDate, 7, 10 + Math.floor(rng() * 20)),
      nfcTagId: 'seed-nfc-tag-001',
      updatedAt: dayKey === today ? now : atLocalTime(dayDate, 7, 30),
    });
  }

  return intents;
}

export function intentsToDocs(intents: DailyIntentRecord[]): Record<string, unknown>[] {
  return intents;
}

export function intentDates(intents: DailyIntentRecord[]): Set<string> {
  return new Set(intents.map((i) => i.date));
}
