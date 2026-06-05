# Stella API

NestJS backend for Stella Life OS. Persistence via **Mongoose** + MongoDB.

## Setup

```bash
cp .env.example .env
# Set DATABASE_URL (MongoDB Atlas or local standalone) and API_KEY
# Optional: REQUEST_BODY_LIMIT=512kb (default; raise if sync push batches grow)
npm install
npm run start:dev
```

## Endpoints

Base: `http://localhost:3000/api/v1`

Header: `X-Api-Key: <API_KEY>`

| Method | Path | Auth |
|--------|------|------|
| GET | `/health` | Public |
| GET/POST/PATCH/DELETE | `/habits` | API key |
| GET/POST | `/habits/:habitId/check-ins` | API key |
| GET/POST/PATCH/DELETE | `/tasks` | API key |
| GET/POST/PATCH/DELETE | `/events` | API key |
| POST | `/sync/push` | API key |
| GET | `/sync/pull?since=ISO8601` | API key (monolithic or `?entity=&limit=&cursor=`) |
| GET | `/habits/check-ins?from=&to=` | API key |
| GET | `/life-logs?since=&limit=&cursor=` | API key |

## MongoDB

**Local standalone (dev):**

```text
mongodb://127.0.0.1:27017/stella
```

Optional query params: `?directConnection=true` if connecting through a tunnel.

**Atlas (production):**

```text
mongodb+srv://USER:PASSWORD@CLUSTER.mongodb.net/stella?retryWrites=true&w=majority
```

Indexes are created on startup via Mongoose `syncIndexes()` — no migration CLI required.

Existing data from the Prisma era remains compatible (same collection names and String UUID `_id` values).

## Database seeding (dev)

Populate MongoDB with **10,000+** sync-valid fake documents spanning the previous calendar year through today’s date in the range (habits, check-ins, tasks, intents, reviews, calendar, finances, assistant threads/messages, life logs). **No documents are keyed to the run’s local “today”** — bulk generators skip today; QA fixtures anchor on **yesterday** so sync does not trigger task takeover.

**Requires local MongoDB** running at `DATABASE_URL`.

```bash
npm run db:seed                              # ~10k docs, seed=42, 2025-01-01 → today
npm run db:seed -- --clean-first             # wipe Stella collections first
npm run db:seed -- --seed 123 --min-docs 15000
npm run db:seed -- --from 2025-01-01 --to 2025-12-31
npm run db:clean -- --confirm                # remove all Stella data (no seed)
```

| Flag | Default | Description |
|------|---------|-------------|
| `--clean-first` | off | `deleteMany` on all Stella collections before insert |
| `--seed` | `42` | PRNG seed for reproducible runs |
| `--from` / `--to` | prev-year Jan 1 → today | Inclusive `YYYY-MM-DD` range |
| `--min-docs` | `10000` | Target minimum total documents (auto-scales life logs if short) |

**QA fixtures** (yesterday-anchored) are always included: overdue task on yesterday, upcoming on tomorrow, coach nudge thread, habit check-ins for the last three past days. Stable fixture IDs are printed in the seed summary.

All seeded documents get **`updatedAt = now`** so the next sync pull returns the full dataset (sync filters by `updatedAt > lastPulledAt`). If the app still shows partial history, use **Settings → Purge local data**, then **Sync now**.

**Task takeover after seed:** use **Settings → Diagnostics → Task takeover (10s)** or create a task with `scheduledAt` on local today — stale TODO tasks from other calendar days do not schedule alarms.

**First Android sync** after seeding may be slow — `GET /sync/pull` returns all changed entities with no pagination yet.

Scripts live in `scripts/seed-fake-data.ts` and `scripts/seed/`.

## Build

```bash
npm run build
npm run start:prod
```

Docker: see [../docs/deployment.md](../docs/deployment.md).
