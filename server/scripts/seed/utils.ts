import type { Db } from 'mongodb';
import type { DateRange, GeneratedSeedData, SeedOptions } from './types';

export function maskUrl(url: string): string {
  return url.replace(/\/\/([^:@/]+):([^@/]+)@/, '//$1:***@');
}

/** Local calendar date YYYY-MM-DD — matches Android TimeService.dateKey. */
export function localDateKey(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function localDateFromInstant(d: Date): string {
  return localDateKey(d);
}

export function parseDateKey(key: string): Date {
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d, 12, 0, 0, 0);
}

export function addLocalDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function offsetDateKey(dayKey: string, days: number): string {
  return localDateKey(addLocalDays(parseDateKey(dayKey), days));
}

export function startOfPreviousYear(now: Date): string {
  return `${now.getFullYear() - 1}-01-01`;
}

export function defaultDateRange(now: Date): DateRange {
  const fromKey = startOfPreviousYear(now);
  const toKey = localDateKey(now);
  return {
    from: parseDateKey(fromKey),
    to: parseDateKey(toKey),
    fromKey,
    toKey,
  };
}

export function* eachDay(fromKey: string, toKey: string): Generator<string> {
  let current = parseDateKey(fromKey);
  const end = parseDateKey(toKey);
  while (current <= end) {
    yield localDateKey(current);
    current = addLocalDays(current, 1);
  }
}

/**
 * Sync pull filters by updatedAt. Historical logical dates stay on date/scheduledAt fields;
 * bump updatedAt so re-seeded data is always returned on the next pull.
 */
export function stampForSync(data: GeneratedSeedData, syncedAt: Date): GeneratedSeedData {
  const stamp = (docs: Record<string, unknown>[]) =>
    docs.map((doc) => ({ ...doc, updatedAt: syncedAt }));

  return {
    habits: stamp(data.habits),
    habitCheckIns: stamp(data.habitCheckIns),
    tasks: stamp(data.tasks),
    calendarEvents: stamp(data.calendarEvents),
    dailyIntents: stamp(data.dailyIntents),
    eveningReviews: stamp(data.eveningReviews),
    transactions: stamp(data.transactions),
    debts: stamp(data.debts),
    assistantThreads: stamp(data.assistantThreads),
    assistantMessages: stamp(data.assistantMessages),
    lifeLogs: stamp(data.lifeLogs),
  };
}

export function mulberry32(seed: number): () => number {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function randomInt(rng: () => number, min: number, max: number): number {
  return min + Math.floor(rng() * (max - min + 1));
}

export function pickOne<T>(rng: () => number, items: T[]): T {
  return items[Math.floor(rng() * items.length)]!;
}

export function pickWeighted<T>(rng: () => number, items: Array<{ value: T; weight: number }>): T {
  const total = items.reduce((sum, item) => sum + item.weight, 0);
  let roll = rng() * total;
  for (const item of items) {
    roll -= item.weight;
    if (roll <= 0) return item.value;
  }
  return items[items.length - 1]!.value;
}

export function chance(rng: () => number, probability: number): boolean {
  return rng() < probability;
}

export function parseArgs(now: Date): SeedOptions {
  const argv = process.argv.slice(2);
  const hasFlag = (flag: string) => argv.includes(flag);

  const readValue = (flag: string): string | undefined => {
    const index = argv.indexOf(flag);
    if (index === -1 || index + 1 >= argv.length) return undefined;
    return argv[index + 1];
  };

  const defaultRange = defaultDateRange(now);
  const fromKey = readValue('--from') ?? defaultRange.fromKey;
  const toKey = readValue('--to') ?? defaultRange.toKey;

  if (fromKey > toKey) {
    throw new Error(`Invalid date range: --from ${fromKey} is after --to ${toKey}`);
  }

  return {
    cleanFirst: hasFlag('--clean-first'),
    seed: Number(readValue('--seed') ?? '42'),
    fromKey,
    toKey,
    minDocs: Number(readValue('--min-docs') ?? '10000'),
  };
}

export async function insertBatched(
  db: Db,
  collection: string,
  docs: Record<string, unknown>[],
  batchSize = 1000,
): Promise<number> {
  if (docs.length === 0) return 0;
  let inserted = 0;
  for (let i = 0; i < docs.length; i += batchSize) {
    const batch = docs.slice(i, i + batchSize);
    await db.collection(collection).insertMany(batch);
    inserted += batch.length;
  }
  return inserted;
}

export function dedupeByKey<T extends Record<string, unknown>>(
  items: T[],
  keyFn: (item: T) => string,
): T[] {
  const map = new Map<string, T>();
  for (const item of items) {
    map.set(keyFn(item), item);
  }
  return [...map.values()];
}

export function omitDay<T extends { date?: string; dayKey?: string }>(
  items: T[],
  day: string,
  dayField: 'date' | 'dayKey' = 'date',
): T[] {
  return items.filter((item) => item[dayField] !== day);
}

export function stripInternalFields<T extends Record<string, unknown>>(items: T[]): T[] {
  return items.map((item) => {
    const { dayKey: _dayKey, ...rest } = item as T & { dayKey?: string };
    return rest as T;
  });
}

export function countDocs(data: Record<string, Record<string, unknown>[]>): number {
  return Object.values(data).reduce((sum, docs) => sum + docs.length, 0);
}
