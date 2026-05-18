# ADR 002: MongoDB Atlas over PostgreSQL

## Status

Accepted

## Context

Original brainstorm specified PostgreSQL + Prisma. The operator chose **MongoDB Atlas** (managed) and **GCP VM** hosting for the solo deployment.

Stella stores flexible `LifeLog.payload` documents and may evolve evening review snapshots without rigid relational migrations.

## Decision

- Database: **MongoDB Atlas**
- ORM: **Prisma** with MongoDB provider
- Entity ids: **String UUID** (not ObjectId) for alignment with Android client

## Consequences

**Positive**

- Atlas handles backups, scaling, and ops for solo developer
- Document model fits `LifeLog` and embedded snapshots
- Prisma keeps type safety in NestJS

**Negative**

- No relational joins; application enforces references
- Prisma MongoDB feature set differs from SQL (no migrations in same way — use `db push` carefully)
- Original docs referencing PostgreSQL are obsolete

## Alternatives considered

- **PostgreSQL on Atlas or VM:** Rejected per operator preference
- **Mongoose without Prisma:** Rejected; Prisma + NestJS TypeScript synergy
