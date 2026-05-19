# ADR 007: Mongoose ORM (replacing Prisma)

## Status

Accepted

## Context

The server originally used **Prisma** with the MongoDB provider. Local development on **standalone** MongoDB (`mongodb://localhost:27017/...`) failed with Prisma error **P2031** because Prisma enables retryable writes that require a replica set.

Workarounds (`retryWrites=false`, find-then-update instead of `upsert`, URL normalization) were fragile and still blocked reliable sync during Phase 1 development.

## Decision

- Replace Prisma with **Mongoose** and **`@nestjs/mongoose`**
- Define 8 document schemas under `server/src/database/schemas/`
- Use **String UUID** for `_id` (not ObjectId) to match Android client ids
- Collection names match existing Prisma defaults (`Habit`, `Task`, `HabitCheckIn`, etc.) so existing Mongo data remains readable
- Sync indexes via `syncIndexes()` on application startup (`IndexSyncService`)
- Map `_id` → `id` in API JSON responses (`document.util.ts`)

## Consequences

**Positive**

- Works on standalone MongoDB and Atlas without replica-set requirements
- Native MongoDB driver behavior for upserts and bulk sync
- NestJS-first integration (`@InjectModel`, feature modules)

**Negative**

- No generated client types from a single schema file (schemas live in TypeScript)
- Less compile-time query safety than Prisma
- Index management is application-owned (`syncIndexes`) rather than `prisma db push`

## Alternatives considered

- **Keep Prisma + local replica set:** Rejected; too heavy for solo dev
- **TypeORM MongoDB:** Rejected; weaker MongoDB support than Mongoose in Nest ecosystem

## Supersedes

Prisma as the server ORM (see amended [002-mongodb-atlas.md](002-mongodb-atlas.md)).
