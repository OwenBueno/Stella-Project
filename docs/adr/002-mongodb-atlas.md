# ADR 002: MongoDB Atlas over PostgreSQL

## Status

Accepted (amended — ORM is Mongoose; see [007-mongoose-orm.md](007-mongoose-orm.md))

## Context

Original brainstorm specified PostgreSQL + Prisma. The operator chose **MongoDB Atlas** (managed) and **GCP VM** hosting for the solo deployment.

Stella stores flexible `LifeLog.payload` documents and may evolve evening review snapshots without rigid relational migrations.

## Decision

- Database: **MongoDB Atlas** (or local standalone MongoDB for dev)
- ORM: **Mongoose** + `@nestjs/mongoose` (supersedes Prisma — [ADR 007](007-mongoose-orm.md))
- Entity ids: **String UUID** (not ObjectId) for alignment with Android client

## Consequences

**Positive**

- Atlas handles backups, scaling, and ops for solo developer
- Document model fits `LifeLog` and embedded snapshots
- Mongoose works on standalone MongoDB without replica-set workarounds

**Negative**

- No relational joins; application enforces references
- Index changes require `syncIndexes()` or manual Atlas index management
- Original docs referencing PostgreSQL are obsolete

## Alternatives considered

- **PostgreSQL on Atlas or VM:** Rejected per operator preference
- **Prisma with MongoDB:** Used initially; replaced due to P2031 on standalone dev ([ADR 007](007-mongoose-orm.md))
