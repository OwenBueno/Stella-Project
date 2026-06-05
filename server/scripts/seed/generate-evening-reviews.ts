import { atLocalTime, mondayOfWeek, uuid } from '../collections';
import type { DailyIntentRecord, HabitRecord, SeedContext } from './types';
import { buildHabitGridCells } from './generate-check-ins';
import { chance, parseDateKey } from './utils';

export function generateEveningReviews(
  ctx: SeedContext,
  intents: DailyIntentRecord[],
  habits: HabitRecord[],
  checkIns: Record<string, unknown>[],
  skipDates: Set<string>,
): Record<string, unknown>[] {
  const { now, today, rng } = ctx;
  const reviews: Record<string, unknown>[] = [];

  for (const intent of intents) {
    if (skipDates.has(intent.date)) continue;
    if (!chance(rng, 0.82)) continue;

    const dayDate = parseDateKey(intent.date);
    const weekStart = mondayOfWeek(dayDate);
    const cells = buildHabitGridCells(habits, checkIns, weekStart);
    const completed = intent.plannedTaskIds.length;
    const doneCount = Math.max(0, Math.floor(completed * (0.4 + rng() * 0.5)));

    reviews.push({
      _id: uuid(),
      date: intent.date,
      plannedVsActual: `Planned ${completed} tasks, completed ${doneCount}.`,
      reflectionText: chance(rng, 0.7)
        ? pickReflection(ctx)
        : null,
      habitGridSnapshot: { weekStart, cells },
      completedAt: atLocalTime(dayDate, 21, 15 + Math.floor(rng() * 30)),
      updatedAt: intent.date === today ? now : atLocalTime(dayDate, 21, 45),
    });
  }

  return reviews;
}

function pickReflection(ctx: SeedContext): string {
  const options = [
    'Solid day overall — protect the morning block tomorrow.',
    'Too many context switches; batch admin work.',
    'Missed exercise — schedule it earlier.',
    'Good focus in the afternoon block.',
    'Need to finish planning before noon.',
  ];
  return options[Math.floor(ctx.rng() * options.length)]!;
}
