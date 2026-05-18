# ADR 003: Offline-First Room with Last-Write-Wins Sync

## Status

Accepted

## Context

Enforcement features (morning lock, alarms) must work without network. The solo user still wants cloud backup and reinstall recovery.

## Decision

1. **Room is the source of truth** for all user actions.
2. Sync is asynchronous via WorkManager + on-demand triggers.
3. Entity ids are **client-generated UUIDs**.
4. Conflicts resolve by **last-write-wins** using `updatedAt`.

## Consequences

**Positive**

- App usable in airplane mode
- Simple sync protocol for solo use
- Idempotent upserts on server

**Negative**

- Rare conflicts if multiple devices added later may silently overwrite
- Requires `needsSync` flags or outbox on client

## Alternatives considered

- **Server-authoritative:** Rejected; breaks offline enforcement
- **CRDT / operational transform:** Rejected; overkill for solo v1

## Follow-up

If multi-device is added, consider per-entity version vectors or user-visible conflict UI.
