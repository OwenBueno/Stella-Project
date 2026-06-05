import {
  STELLA_COLLECTIONS,
  connectDatabase,
  disconnectDatabase,
  getDatabaseUrl,
} from './collections';
import { buildBulkSeed } from './seed/build-bulk';
import { buildTodayFixtures } from './seed/fixtures-today';
import { mergeSeedData, toInserts } from './seed/merge';
import type { SeedContext } from './seed/types';
import {
  countDocs,
  insertBatched,
  localDateKey,
  maskUrl,
  mulberry32,
  parseArgs,
  parseDateKey,
  stampForSync,
} from './seed/utils';

async function main(): Promise<void> {
  const now = new Date();
  const options = parseArgs(now);
  const range = {
    from: parseDateKey(options.fromKey),
    to: parseDateKey(options.toKey),
    fromKey: options.fromKey,
    toKey: options.toKey,
  };

  const ctx: SeedContext = {
    now,
    today: localDateKey(now),
    range,
    rng: mulberry32(options.seed),
    options,
  };

  const url = getDatabaseUrl();
  console.log(`Connecting to ${maskUrl(url)} …`);
  await connectDatabase();

  const mongoose = await import('mongoose');
  const db = mongoose.default.connection.db;
  if (!db) {
    throw new Error('MongoDB connection not ready');
  }

  if (options.cleanFirst) {
    console.log('Cleaning existing Stella data (--clean-first) …');
    for (const name of STELLA_COLLECTIONS) {
      await db.collection(name).deleteMany({});
    }
  }

  console.log(
    `Generating seed data (seed=${options.seed}, range=${options.fromKey} → ${options.toKey}, min=${options.minDocs}) …`,
  );

  const fixtures = buildTodayFixtures(ctx);
  const bulk = buildBulkSeed(ctx, fixtures);
  const merged = stampForSync(mergeSeedData(bulk, fixtures, ctx.today), now);
  const total = countDocs(merged as unknown as Record<string, Record<string, unknown>[]>);

  if (total < options.minDocs) {
    console.warn(
      `Warning: generated ${total} documents, below --min-docs ${options.minDocs}. Re-run with a wider range or lower min-docs.`,
    );
  }

  const inserts = toInserts(merged);

  console.log('Inserting seed data …');
  let insertedTotal = 0;
  for (const { collection, docs } of inserts) {
    if (docs.length === 0) continue;
    const count = await insertBatched(db, collection, docs);
    insertedTotal += count;
    console.log(`  ${collection.padEnd(20)} ${count} document(s)`);
  }

  console.log(`\nTotal inserted: ${insertedTotal} document(s)`);
  console.log('\nSeed summary (for manual testing):');
  console.log(`  Date range:         ${options.fromKey} → ${options.toKey}`);
  console.log(`  Today (local):      ${ctx.today} (no today-keyed seed rows)`);
  console.log(`  QA anchor day:      yesterday — fixtures use stable IDs below`);
  console.log(`  Overdue task:       ${fixtures.ids.taskIds.overdue} — Submit expense report (yesterday)`);
  console.log(`  Upcoming task:      ${fixtures.ids.taskIds.upcoming} — Team standup prep (tomorrow)`);
  console.log(`  Assistant thread:   ${fixtures.ids.threadId} (session yesterday)`);
  console.log(`  Coach message:      ${fixtures.ids.coachMessageId}`);
  console.log(`\nPull from Android with GET /sync/pull or use sync after pointing device at this server.`);
  console.log('Note: all documents use updatedAt=now so the next sync pull returns the full seed.');
  console.log('      Use Settings → Purge local data first if Room still has stale rows from a prior sync.');
  console.log('      Task takeover after seed: use Settings → Diagnostics → Task takeover (10s).');

  await disconnectDatabase();
}

main().catch((err: unknown) => {
  console.error(err);
  process.exit(1);
});
