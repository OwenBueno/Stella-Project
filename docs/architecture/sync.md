# Sync Architecture

Offline-first: **Room is the source of truth** for user interactions. The server is backup and future multi-device support.

## Goals

- App fully usable in airplane mode
- No duplicate entities after retry
- Simple conflict model for solo user

## Identity

- Every entity created on device gets a **client-generated UUID v4** as `id`
- Same `id` used in Room and MongoDB
- Upserts are idempotent: `PUT` semantics via `POST /sync/push` with full document

## Timestamps

Each syncable entity includes:

| Field | Set by | Purpose |
|-------|--------|---------|
| `createdAt` | Creator | Audit |
| `updatedAt` | Last writer | Conflict resolution |
| `deletedAt` | Optional | Soft delete tombstone |

Clock skew: server may bump `updatedAt` on receive but must not change `id`.

## Conflict resolution (v1)

**Last-write-wins (LWW)** by `updatedAt`:

1. On push, server compares incoming `updatedAt` with stored document.
2. If incoming is newer or equal, replace.
3. If stored is newer, reject with `409` and return server copy in error body (client applies server version).

Solo use makes conflicts rare; LWW is sufficient until multi-device.

## Sync triggers (Android)

| Trigger | When |
|---------|------|
| App foreground | `SyncWorker` one-time |
| After evening review save | Immediate push |
| Periodic | WorkManager every 6 hours |
| Manual | Settings → "Sync now" |

## Protocol

### Push — `POST /api/v1/sync/push`

Client sends batches of changed entities since last successful push.

```json
{
  "deviceId": "uuid",
  "pushedAt": "2026-05-18T12:00:00Z",
  "habits": [],
  "habitCheckIns": [],
  "tasks": [],
  "events": [],
  "dailyIntents": [],
  "eveningReviews": [],
  "lifeLogs": [],
  "transactions": [],
  "debts": []
}
```

Response:

```json
{
  "accepted": 42,
  "conflicts": [
    {
      "entity": "task",
      "id": "...",
      "serverDocument": { }
    }
  ]
}
```

Client applies `conflicts` to Room (overwrite local).

### Pull — `GET /api/v1/sync/pull?since=ISO8601`

Returns all entities with `updatedAt > since` (excluding soft-deleted unless `includeDeleted=true`).

```json
{
  "serverTime": "2026-05-18T12:00:00Z",
  "habits": [],
  "habitCheckIns": [],
  "tasks": [],
  "events": [],
  "dailyIntents": [],
  "eveningReviews": [],
  "lifeLogs": [],
  "transactions": [],
  "debts": []
}
```

Client merges into Room inside a single transaction.

## Local sync metadata (Room)

`SyncMetaEntity`:

| Field | Description |
|-------|-------------|
| `deviceId` | Generated once per install |
| `lastPushedAt` | Last successful push watermark |
| `lastPulledAt` | Last successful pull watermark |

## Outbox pattern (optional enhancement)

Phase 1 may use "dirty" flags on entities (`needsSync: Boolean`). Phase 2+ can add `sync_outbox` table for failed pushes.

## Failure handling

| Failure | Behavior |
|---------|----------|
| No network | Skip; retry on next trigger |
| 401 | Surface invalid API key in Settings |
| 409 conflict | Apply server document; optionally notify user |
| 5xx | Exponential backoff in WorkManager |

## Fresh install / reinstall

1. User enters API key and base URL.
2. Pull all data from server (`since` omitted or epoch).
3. Populate Room.
4. Habit grid renders from local data.

**Gate (Phase 1):** Uninstall → reinstall → pull restores habits and check-ins.

## Related

- [../api/rest-api.md](../api/rest-api.md)
- [../data-model.md](../data-model.md)
- [../adr/003-offline-first-sync.md](../adr/003-offline-first-sync.md)
