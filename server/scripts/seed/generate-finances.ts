import { addDays, atLocalTime, uuid } from '../collections';
import type { SeedContext, TaskRecord } from './types';
import { chance, eachDay, parseDateKey, pickOne, randomInt } from './utils';

const EGRESS_CATEGORIES = ['Food', 'Transport', 'Utilities', 'Entertainment', 'Health', 'Shopping'];

const DEBT_CONTACTS = ['Alex', 'Sam', 'Jordan', 'Taylor', 'Casey', 'Riley', 'Morgan', 'Quinn'];

export function generateFinances(
  ctx: SeedContext,
  skippedTaskList: TaskRecord[],
): { transactions: Record<string, unknown>[]; debts: Record<string, unknown>[] } {
  const { now, today, range, rng } = ctx;
  const transactions: Record<string, unknown>[] = [];
  const debts: Record<string, unknown>[] = [];

  const monthsSeen = new Set<string>();
  for (const dayKey of eachDay(range.fromKey, range.toKey)) {
    if (dayKey === today) continue;
    const dayDate = parseDateKey(dayKey);
    const monthKey = dayKey.slice(0, 7);

    if (!monthsSeen.has(monthKey) && dayDate.getDate() <= 5) {
      monthsSeen.add(monthKey);
      transactions.push({
        _id: uuid(),
        type: 'ingress',
        amount: 3000 + randomInt(rng, 0, 400),
        category: 'Salary',
        description: `Monthly salary (${monthKey})`,
        date: atLocalTime(dayDate, 9, 0),
        linkedTaskId: null,
        createdAt: atLocalTime(dayDate, 9, 0),
        updatedAt: atLocalTime(dayDate, 9, 0),
        deletedAt: null,
      });
    }

    if (chance(rng, 0.45)) {
      transactions.push({
        _id: uuid(),
        type: 'egress',
        amount: Math.round((8 + rng() * 120) * 100) / 100,
        category: pickOne(rng, EGRESS_CATEGORIES),
        description: pickOne(rng, ['Groceries', 'Coffee', 'Gas', 'Subscription', 'Dinner out']),
        date: atLocalTime(dayDate, 12 + Math.floor(rng() * 8), 0),
        linkedTaskId: null,
        createdAt: atLocalTime(dayDate, 12, 0),
        updatedAt: atLocalTime(dayDate, 12, 0),
        deletedAt: null,
      });
    }
  }

  for (const task of skippedTaskList) {
    if (!chance(rng, 0.08)) continue;
    const dayDate = task.scheduledAt ?? now;
    transactions.push({
      _id: uuid(),
      type: 'egress',
      amount: pickOne(rng, [10, 15, 20, 25]),
      category: 'Penalty',
      description: 'Skipped task penalty (ledger only)',
      date: dayDate,
      linkedTaskId: task._id,
      createdAt: dayDate,
      updatedAt: dayDate,
      deletedAt: null,
    });
  }

  for (let i = 0; i < 10; i++) {
    const total = randomInt(rng, 40, 500);
    const resolved = chance(rng, 0.35);
    const remaining = resolved ? 0 : Math.round(total * (0.2 + rng() * 0.7));
    const createdAt = addDays(range.from, randomInt(rng, 0, 300));

    debts.push({
      _id: uuid(),
      contactName: pickOne(rng, DEBT_CONTACTS),
      direction: pickOne(rng, ['owed_to_me', 'owed_by_me']),
      totalAmount: total,
      remainingAmount: remaining,
      dueDate: resolved ? null : addDays(now, randomInt(rng, -30, 45)),
      notes: chance(rng, 0.5) ? 'Seed debt entry' : null,
      isResolved: resolved,
      createdAt,
      updatedAt: now,
      deletedAt: null,
    });
  }

  return { transactions, debts };
}
