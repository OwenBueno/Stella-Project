# Stella API

NestJS backend for Stella Life OS. Persistence via **Mongoose** + MongoDB.

## Setup

```bash
cp .env.example .env
# Set DATABASE_URL (MongoDB Atlas or local standalone) and API_KEY
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
| GET | `/sync/pull?since=ISO8601` | API key |

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

## Build

```bash
npm run build
npm run start:prod
```

Docker: see [../docs/deployment.md](../docs/deployment.md).
