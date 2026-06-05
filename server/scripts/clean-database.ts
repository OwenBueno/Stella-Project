import {
  STELLA_COLLECTIONS,
  connectDatabase,
  disconnectDatabase,
  getDatabaseUrl,
} from './collections';

function hasConfirmFlag(): boolean {
  return process.argv.includes('--confirm') || process.env.DB_CLEAN_CONFIRM === '1';
}

async function main(): Promise<void> {
  if (!hasConfirmFlag()) {
    console.error(
      'Refusing to clean database without confirmation.\n' +
        'Re-run with:  npm run db:clean -- --confirm\n' +
        'Or set env:   DB_CLEAN_CONFIRM=1 npm run db:clean',
    );
    process.exit(1);
  }

  const url = getDatabaseUrl();
  console.log(`Connecting to ${maskUrl(url)} …`);
  await connectDatabase();

  const db = (await import('mongoose')).default.connection.db;
  if (!db) {
    throw new Error('MongoDB connection not ready');
  }

  const results: Array<{ collection: string; deleted: number }> = [];

  for (const name of STELLA_COLLECTIONS) {
    const result = await db.collection(name).deleteMany({});
    results.push({ collection: name, deleted: result.deletedCount ?? 0 });
  }

  console.log('Stella collections cleaned:');
  for (const row of results) {
    console.log(`  ${row.collection.padEnd(20)} ${row.deleted} document(s) removed`);
  }

  const total = results.reduce((sum, row) => sum + row.deleted, 0);
  console.log(`Done — ${total} document(s) removed across ${STELLA_COLLECTIONS.length} collections.`);

  await disconnectDatabase();
}

function maskUrl(url: string): string {
  return url.replace(/\/\/([^:@/]+):([^@/]+)@/, '//$1:***@');
}

main().catch((err: unknown) => {
  console.error(err);
  process.exit(1);
});
