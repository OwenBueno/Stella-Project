# Executive Summary: Project Stella

A custom, aggressive productivity and accountability assistant — a **Life OS** combining agenda, habit tracker, life record, and an uncompromising virtual coach.

> **Canonical docs:** See [README.md](README.md) for the full documentation index. Stack details live in [stack.md](stack.md); phased delivery in [implementation-roadmap.md](implementation-roadmap.md).

## 1. Project objective

Eliminate morning procrastination, enforce daily planning, and prevent critical tasks from sliding into the evening. The app uses **high-friction UX** (device-level interventions) instead of passive notifications.

## 2. Core problems

- **Morning mess:** No strict structure after waking; intentions are never set.
- **Evening rush:** Avoided daytime work piles up and runs reactively at night.
- **App fatigue:** Standard apps allow snooze/ignore.

## 3. Solution (features)

| Feature | Behavior |
|---------|----------|
| **Morning hostage** | Overlay locks distractions until NFC scan + Top 3 daily intent + calendar blocks |
| **Aggressive assistant** | Scheduled tasks trigger full-screen takeover + repeating alarm until focus starts |
| **Habit matrix** | Green = done, red = missed — no neutral gray on cells |
| **Life record** | Tasks, events, evening reviews in one searchable history |

## 4. Technical stack (current)

| Layer | Choice |
|-------|--------|
| **Android** | Kotlin, Jetpack Compose, **MVI**, Hilt, Room, minSdk **31** |
| **Backend** | NestJS (TypeScript) on **GCP VM** |
| **Database** | **MongoDB Atlas** via Mongoose (`@nestjs/mongoose`) |
| **Auth (solo)** | Static API key (`X-Api-Key`) |
| **Push (Phase 3)** | Firebase Cloud Messaging |
| **AI (later)** | OpenAI via Nest cron for evening coaching |

Calendar is **Stella-owned only** (no Google Calendar sync in v1). Financial penalties (Stripe) are **deferred** — see [adr/006-penalties-deferred.md](adr/006-penalties-deferred.md).

## 5. Architecture (high level)

- **Android:** Clean Architecture + MVI; offline-first Room; sync to server.
- **Server:** NestJS modules per domain; Mongoose models; API key guard.
- **Sync:** Client-generated UUIDs; last-write-wins by `updatedAt`.

See [architecture/overview.md](architecture/overview.md).

## 6. Execution roadmap

| Phase | Focus | Gate |
|-------|-------|------|
| **1** | Habit grid, tasks, calendar CRUD, sync | Offline grid + sync round-trip |
| **2** | Morning lock, NFC, daily intent, evening review | Cannot skip morning flow |
| **3** | Exact alarms, takeover UI, FCM | Alarm fires on schedule; push works |

Details: [implementation-roadmap.md](implementation-roadmap.md).

## 7. Repository

- Android: `Stella/`
- API: `server/`
- Docs: `docs/`
