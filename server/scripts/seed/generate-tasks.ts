import { addDays, atLocalTime, uuid } from '../collections';
import type { SeedContext, TaskRecord } from './types';
import { chance, eachDay, localDateFromInstant, parseDateKey, pickOne, pickWeighted, randomInt } from './utils';

const TASK_TITLES = [
  'Review pull requests',
  'Write weekly update',
  'Deep work session',
  'Respond to emails',
  'Plan sprint tasks',
  'Code review backlog',
  'Update documentation',
  'Sync with team',
  'Prepare presentation',
  'Organize workspace',
  'Research new feature',
  'Fix flaky test',
  'Refactor module',
  'Customer follow-up',
  'Budget review',
];

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'] as const;

export interface TasksByDay {
  tasks: TaskRecord[];
  byDay: Map<string, TaskRecord[]>;
}

export function generateTasks(
  ctx: SeedContext,
  skipDays: Set<string>,
  tasksPerDay = 4,
): TasksByDay {
  const { now, today, range, rng } = ctx;
  const tasks: TaskRecord[] = [];
  const byDay = new Map<string, TaskRecord[]>();

  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (skipDays.has(dayKey)) continue;

    const dayDate = parseDateKey(dayKey);
    const isToday = dayKey === today;
    const isPast = dayKey < today;
    const count = randomInt(rng, tasksPerDay, tasksPerDay + 1);
    const dayTasks: TaskRecord[] = [];

    for (let i = 0; i < count; i++) {
      const hour = 8 + Math.floor(rng() * 11);
      const scheduledAt = atLocalTime(dayDate, hour, randomInt(rng, 0, 45));

      let status: string;
      if (isToday) {
        status = pickWeighted(rng, [
          { value: 'TODO', weight: 0.5 },
          { value: 'IN_PROGRESS', weight: 0.2 },
          { value: 'DONE', weight: 0.25 },
          { value: 'SKIPPED', weight: 0.05 },
        ]);
      } else if (isPast) {
        status = pickWeighted(rng, [
          { value: 'DONE', weight: 0.72 },
          { value: 'SKIPPED', weight: 0.18 },
          { value: 'TODO', weight: 0.1 },
        ]);
      } else {
        status = 'TODO';
      }

      const task: TaskRecord = {
        _id: uuid(),
        title: pickOne(rng, TASK_TITLES),
        notes: chance(rng, 0.2) ? 'Auto-generated seed task' : null,
        scheduledAt,
        durationMinutes: pickOne(rng, [15, 25, 30, 45, 60, 90]),
        status,
        sortOrder: i,
        priority: pickOne(rng, [...PRIORITIES]),
        createdAt: addDays(dayDate, -randomInt(rng, 0, 3)),
        updatedAt: isPast ? atLocalTime(dayDate, hour + 1, 0) : now,
        deletedAt: null,
        dayKey,
      };

      dayTasks.push(task);
      tasks.push(task);
    }

    byDay.set(dayKey, dayTasks);
  }

  return { tasks, byDay };
}

export function tasksToDocs(tasks: TaskRecord[]): Record<string, unknown>[] {
  return tasks.map(({ dayKey: _dayKey, ...task }) => task);
}

export function skippedTasks(tasks: TaskRecord[]): TaskRecord[] {
  return tasks.filter((t) => t.status === 'SKIPPED');
}

export function taskDayKey(task: TaskRecord | Record<string, unknown>): string {
  const scheduledAt = task.scheduledAt as Date | null;
  if (!scheduledAt) return '';
  return localDateFromInstant(scheduledAt);
}
