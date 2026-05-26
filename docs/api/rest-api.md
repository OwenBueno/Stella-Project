# REST API Contract

Base URL: `https://<your-domain>/api/v1`

All endpoints require header:

```http
X-Api-Key: <your-api-key>
```

Content-Type: `application/json`

## Error format

```json
{
  "code": "VALIDATION_ERROR",
  "message": "title must not be empty"
}
```

| HTTP | Usage |
|------|-------|
| 200 | Success with body |
| 201 | Created |
| 204 | Deleted |
| 400 | Validation error |
| 401 | Missing/invalid API key |
| 404 | Entity not found |
| 409 | Sync conflict (body includes `serverDocument`) |
| 500 | Server error |

---

## Habits (Phase 1)

### List habits

```http
GET /habits?active=true
```

Response `200`:

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Drink Water",
      "sortOrder": 0,
      "active": true,
      "createdAt": "2026-05-01T08:00:00Z",
      "updatedAt": "2026-05-01T08:00:00Z",
      "deletedAt": null
    }
  ]
}
```

### Create habit

```http
POST /habits
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Read 10 pages",
  "sortOrder": 1,
  "active": true,
  "createdAt": "2026-05-18T08:00:00Z",
  "updatedAt": "2026-05-18T08:00:00Z"
}
```

### Update habit

```http
PATCH /habits/:id
```

Partial body allowed (`name`, `sortOrder`, `active`, `updatedAt`).

### Delete habit

```http
DELETE /habits/:id
```

Soft-delete: sets `deletedAt`.

### List check-ins for habit

```http
GET /habits/:habitId/check-ins?from=2026-05-12&to=2026-05-18
```

### Upsert check-in

```http
POST /habits/:habitId/check-ins
```

```json
{
  "id": "...",
  "habitId": "550e8400-e29b-41d4-a716-446655440000",
  "date": "2026-05-18",
  "status": "DONE",
  "updatedAt": "2026-05-18T22:00:00Z"
}
```

---

## Tasks (Phase 1)

### List tasks

```http
GET /tasks?status=TODO&scheduledFrom=2026-05-18T00:00:00Z&scheduledTo=2026-05-19T00:00:00Z
```

### Create task

```http
POST /tasks
```

```json
{
  "id": "...",
  "title": "Code backend sync",
  "notes": null,
  "scheduledAt": "2026-05-18T15:00:00Z",
  "durationMinutes": 45,
  "status": "TODO",
  "priority": "HIGH",
  "createdAt": "2026-05-18T08:00:00Z",
  "updatedAt": "2026-05-18T08:00:00Z"
}
```

### Get / Update / Delete

```http
GET /tasks/:id
PATCH /tasks/:id
DELETE /tasks/:id
```

---

## Calendar events (Phase 1)

### List events

```http
GET /events?from=2026-05-18T00:00:00Z&to=2026-05-19T00:00:00Z
```

### Create event

```http
POST /events
```

```json
{
  "id": "...",
  "title": "Deep work block",
  "startAt": "2026-05-18T09:00:00Z",
  "endAt": "2026-05-18T11:00:00Z",
  "linkedTaskId": "...",
  "createdAt": "2026-05-18T08:00:00Z",
  "updatedAt": "2026-05-18T08:00:00Z"
}
```

### Get / Update / Delete

```http
GET /events/:id
PATCH /events/:id
DELETE /events/:id
```

---

## Finances

Base path: `/finances`. Solo API key auth (no `userId`).

### Transactions

```http
POST /finances/transactions
GET /finances/transactions?type=egress&category=Penalty&startDate=2026-05-01&endDate=2026-05-31
```

Create body (client-generated UUID):

```json
{
  "id": "uuid",
  "type": "egress",
  "amount": 5.0,
  "category": "Penalty",
  "description": "Task skip",
  "date": "2026-05-18T12:00:00Z",
  "linkedTaskId": "task-uuid",
  "createdAt": "2026-05-18T12:00:00Z",
  "updatedAt": "2026-05-18T12:00:00Z"
}
```

### Debts

```http
POST /finances/debts
GET /finances/debts?resolved=false
PATCH /finances/debts/:id
```

Patch body:

```json
{
  "remainingAmount": 25.0,
  "isResolved": false,
  "notes": "Partial payment",
  "updatedAt": "2026-05-18T12:00:00Z"
}
```

### Summary

```http
GET /finances/summary?year=2026&month=5
```

Response:

```json
{
  "ingress": 3200.0,
  "egress": 1450.0,
  "netBalance": 1750.0,
  "owedToMe": 100.0,
  "owedByMe": 50.0
}
```

### Penalty helper (diagnostics / future skip flow)

```http
POST /finances/penalties
```

```json
{ "taskId": "uuid", "amount": 5.0, "description": "optional" }
```

---

## Sync (Phase 1)

### Push

```http
POST /sync/push
```

```json
{
  "deviceId": "device-uuid",
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

Response `200`:

```json
{
  "accepted": 10,
  "conflicts": []
}
```

Conflict entry:

```json
{
  "entity": "task",
  "id": "...",
  "serverDocument": { }
}
```

### Pull

```http
GET /sync/pull?since=2026-05-17T00:00:00Z
```

Omit `since` for full restore.

Response `200`:

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

---

## Daily intents (Phase 2)

```http
POST /daily-intents
GET /daily-intents?date=2026-05-18
```

Create body:

```json
{
  "id": "...",
  "date": "2026-05-18",
  "plannedTaskIds": ["id1", "id2", "id3", "id4"],
  "completedAt": "2026-05-18T07:30:00Z",
  "nfcTagId": "04:ab:cd:...",
  "updatedAt": "2026-05-18T07:30:00Z"
}
```

---

## Evening reviews (Phase 2)

```http
POST /evening-reviews
GET /evening-reviews?date=2026-05-18
```

Create body:

```json
{
  "id": "...",
  "date": "2026-05-18",
  "plannedVsActual": "Planned: ship sync. Actual: shipped sync.",
  "reflectionText": "Good day. No excuses tomorrow.",
  "habitGridSnapshot": { "weekStart": "2026-05-12", "cells": [] },
  "completedAt": "2026-05-18T21:00:00Z",
  "updatedAt": "2026-05-18T21:00:00Z"
}
```

---

## Notifications (Phase 3)

### Register FCM token

```http
POST /notifications/register
```

```json
{
  "deviceId": "...",
  "fcmToken": "..."
}
```

### Test push (dev)

```http
POST /notifications/test
```

```json
{
  "title": "Stella",
  "body": "Test notification"
}
```

---

## AI (later, internal)

```http
POST /ai/evening-evaluation
```

Protected by API key; intended for cron/internal use only. Triggers OpenAI evaluation and optional FCM.

---

## OpenAPI

During server implementation, enable Nest Swagger at `/api/docs` and export to `docs/api/openapi.yaml` when stable.

## Related

- [../data-model.md](../data-model.md)
- [../architecture/sync.md](../architecture/sync.md)
