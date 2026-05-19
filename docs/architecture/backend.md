# Backend Architecture

NestJS API deployed on a GCP VM, persisting to MongoDB via Mongoose.

## Module layout

```text
server/src/
├── main.ts
├── app.module.ts
├── database/
│   ├── database.module.ts       # MongooseModule.forRootAsync
│   ├── schemas/                 # 8 document schemas
│   ├── document.util.ts         # _id → id JSON mapping
│   └── index-sync.service.ts    # syncIndexes on boot
├── common/
│   ├── guards/api-key.guard.ts
│   └── filters/http-exception.filter.ts
├── habits/
│   ├── habits.module.ts
│   ├── habits.controller.ts
│   └── habits.service.ts
├── tasks/
├── calendar/                      # events CRUD
├── sync/
│   ├── sync.controller.ts
│   └── sync.service.ts
├── daily-intents/                 # POST + GET by date
├── evening-reviews/               # POST + GET by date
├── notifications/                 # Phase 3 — FCM
└── ai/                            # later — OpenAI cron
```

## Layering (per module)

```mermaid
flowchart TB
  Controller --> Service
  Service --> Model[Mongoose Model]
  Model --> MongoDB[(MongoDB)]
```

| Layer | Role |
|-------|------|
| **Controller** | HTTP mapping, DTO validation (`class-validator`), status codes |
| **Service** | Business rules, orchestration; injects `@InjectModel()` |
| **Mongoose model** | Queries, indexes, schema definitions |

Controllers stay thin; no direct MongoDB calls in controllers.

## Global concerns

### Authentication

All `/api/v1/*` routes use `ApiKeyGuard` comparing header `X-Api-Key` to `process.env.API_KEY`.

### Validation

- DTOs with `class-validator` + `ValidationPipe` (whitelist, forbidNonWhitelisted)
- UUID format for entity ids

### Errors

```json
{
  "code": "HABIT_NOT_FOUND",
  "message": "Habit with id ... not found"
}
```

Map Nest `HttpException` through a global filter for consistent shape.

## Domain modules

| Module | Phase | Responsibility |
|--------|-------|----------------|
| `habits` | 1 | Habit CRUD, check-in upserts |
| `tasks` | 1 | Task CRUD |
| `calendar` | 1 | Calendar event CRUD |
| `sync` | 1 | Bulk push/pull |
| `daily-intents` | 2 | Morning Top 3 persistence |
| `evening-reviews` | 2 | Evening review + snapshot |
| `life-logs` | 2 | Append-only life events |
| `notifications` | 3 | FCM send, device token store |
| `ai` | later | Cron → OpenAI → notification |

## Mongoose + MongoDB

- Single database; collection names match prior Prisma defaults (`Habit`, `Task`, `HabitCheckIn`, `CalendarEvent`, `DailyIntent`, `EveningReview`, `LifeLog`, `DeviceToken`)
- `_id` is **String UUID** (client-generated), exposed as `id` in JSON
- Index on `updatedAt` for sync pull queries; compound uniques on check-ins and daily records
- Indexes ensured via `IndexSyncService.syncIndexes()` on startup

## Scheduled jobs (Phase 3+)

| Job | Schedule | Action |
|-----|----------|--------|
| Task reminder | Per-task (or pre-computed) | FCM high-priority data message |
| Evening evaluation | 20:00 user TZ | OpenAI summary → push (later) |

Use `@nestjs/schedule` on the VM process.

## Local development

```bash
cd server
cp .env.example .env
npm install
npm run start:dev
```

Local MongoDB example:

```text
mongodb://127.0.0.1:27017/stella
```

No replica set required.

## Deployment

See [../deployment.md](../deployment.md). API listens on internal port; nginx terminates TLS.

## Related

- [sync.md](sync.md)
- [../api/rest-api.md](../api/rest-api.md)
- [../data-model.md](../data-model.md)
- [../adr/007-mongoose-orm.md](../adr/007-mongoose-orm.md)
