import { addDays } from '../collections';
import { FIXTURE_IDS } from './fixtures-today';
import type { HabitRecord, SeedContext } from './types';

export function generateHabits(ctx: SeedContext): HabitRecord[] {
  const { now, range } = ctx;
  const createdAt = addDays(range.from, -30);

  return [
    {
      _id: FIXTURE_IDS.habitIds.morning,
      name: 'Morning routine',
      sortOrder: 0,
      active: true,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: FIXTURE_IDS.habitIds.exercise,
      name: 'Exercise',
      sortOrder: 1,
      active: true,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: FIXTURE_IDS.habitIds.reading,
      name: 'Read 30 min',
      sortOrder: 2,
      active: true,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: FIXTURE_IDS.habitIds.meditation,
      name: 'Meditation',
      sortOrder: 3,
      active: false,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: 'a1111111-1111-4111-8111-111111111105',
      name: 'Journal',
      sortOrder: 4,
      active: true,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
    {
      _id: 'a1111111-1111-4111-8111-111111111106',
      name: 'Stretch',
      sortOrder: 5,
      active: true,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    },
  ];
}

export function activeHabitIds(habits: HabitRecord[]): string[] {
  return habits.filter((h) => h.active).map((h) => h._id);
}
