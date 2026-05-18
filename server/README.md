# Stella Server

NestJS API for Stella Life OS.

## Setup

```bash
cp .env.example .env
# Edit DATABASE_URL and API_KEY
npm install
npx prisma generate
npx prisma db push
npm run start:dev
```

## Endpoints

- `GET /api/v1/health` — public health check
- `GET /api/v1/habits` — list habits (requires `X-Api-Key`)

See [../docs/api/rest-api.md](../docs/api/rest-api.md).
