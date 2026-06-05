import { addDays, addMinutes, atLocalTime, uuid } from '../collections';
import type { SeedContext, TaskRecord } from './types';
import { chance, eachDay, parseDateKey, pickOne, randomInt } from './utils';

const EVENT_TITLES = [
  'Team sync',
  'Focus block',
  '1:1 meeting',
  'Doctor appointment',
  'Gym session',
  'Lunch break',
  'Project review',
  'Client call',
];

export function generateCalendarEvents(
  ctx: SeedContext,
  tasksByDay: Map<string, TaskRecord[]>,
  skipDays: Set<string>,
): Record<string, unknown>[] {
  const { now, range, rng } = ctx;
  const events: Record<string, unknown>[] = [];

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDays.has(dayKey)) continue;

    const dayDate = parseDateKey(dayKey);
    const dayTasks = tasksByDay.get(dayKey) ?? [];
    const eventCount = randomInt(rng, 1, 2);

    for (let i = 0; i < eventCount; i++) {
      const hour = 9 + Math.floor(rng() * 8);
      const startAt = atLocalTime(dayDate, hour, 0);
      const duration = pickOne(rng, [30, 45, 60, 90]);
      const endAt = addMinutes(startAt, duration);

      const linkCandidate = dayTasks[i];
      const linkedTaskId =
        linkCandidate && chance(rng, 0.35) ? linkCandidate._id : null;

      events.push({
        _id: uuid(),
        title: pickOne(rng, EVENT_TITLES),
        startAt,
        endAt,
        linkedTaskId,
        recurrenceRuleJson: null,
        reminderOffsetsJson: JSON.stringify([pickOne(rng, [5, 10, 15])]),
        createdAt: addDays(dayDate, -randomInt(rng, 1, 7)),
        updatedAt: dayKey < ctx.today ? endAt : now,
        deletedAt: null,
      });
    }
  }

  return events;
}
