# Implementation Roadmap

Phased delivery with **exit gates**. Do not start the next phase until the current gate passes.

## Timeline overview

| Phase | Weeks | Theme |
|-------|-------|-------|
| 1 | 1–2 | Foundation: CRUD, habit grid, sync |
| 2 | 3–4 | Friction: morning lock, NFC, evening review |
| 3 | 5–6 | Aggression: alarms, takeover, FCM |
| Future | — | AI coach, Stripe penalties |

---

## Phase 1 — Foundation

### Goals

- Offline habit grid with green/red cells
- Tasks and calendar CRUD (Stella-owned)
- Server backup via sync push/pull
- Dark theme and MVI structure in place

### Server tasks

- [x] NestJS project in `server/` with `ApiKeyGuard`
- [x] Mongoose schemas + MongoDB connection
- [x] Modules: `habits`, `tasks`, `calendar`, `sync`
- [x] CRUD endpoints per [api/rest-api.md](api/rest-api.md)
- [ ] Deploy to GCP VM (or local Docker for dev)

### Android tasks

- [x] `minSdk = 31`, package `com.stella`
- [x] Hilt, Room, Navigation Compose
- [x] `StellaTheme` with design tokens
- [x] Feature modules: `habits`, `tasks`, `calendar` (MVI slices)
- [x] `HabitGrid` composable
- [x] `SyncWorker` + Retrofit client
- [x] Settings: API URL + key

### Exit gate

| # | Criterion |
|---|-----------|
| G1.1 | Create 3 habits, check in for 7 days, colors correct |
| G1.2 | Create tasks and calendar events offline |
| G1.3 | Push + pull sync; reinstall + pull restores data |
| G1.4 | App builds and runs on API 31+ device/emulator |

---

## Phase 2 — Friction layer

### Goals

- Morning cannot be skipped without NFC + daily intent (min 3 planned tasks)
- Evening review persisted to life record
- Life logs for key events

### Server tasks

- [x] `daily-intents` module
- [x] `evening-reviews` module
- [x] `life-logs` via sync only (no standalone CRUD)

### Android tasks

- [ ] Overlay permission onboarding (deferred — Phase 2 uses first-open day gate, not overlay)
- [x] `MorningLockActivity` + evening review WorkManager reminder
- [x] NFC enrollment + verification in Settings
- [x] `DailyIntentActivity` with flexible plan (min 3 tasks) + calendar blocks
- [x] `ReviewScreen` evening review form + local notification trigger
- [x] `LifeLog` writes on unlock and review

### Exit gate

| # | Criterion |
|---|-----------|
| G2.1 | Morning alarm → lock → wrong NFC fails → correct NFC proceeds |
| G2.2 | Cannot reach Home without 3 tasks + time blocks |
| G2.3 | Evening review saves and appears in history/sync |
| G2.4 | LifeLog entries for morning unlock and evening review |

---

## Phase 3 — Aggression layer

### Goals

- Scheduled tasks fire full-screen takeover at exact time
- Repeating alarm until action
- Focus session foreground service
- Server can send FCM push

### Server tasks

- [ ] `notifications` module + Firebase Admin SDK
- [ ] `POST /notifications/register` for FCM token
- [ ] Scheduled job stub for task reminders
- [ ] `POST /notifications/test`

### Android tasks

- [ ] `SCHEDULE_EXACT_ALARM` permission flow
- [ ] `AlarmManager` scheduling per task
- [ ] `TaskTakeoverActivity` + alarm sound channel
- [ ] `FocusForegroundService`
- [ ] FCM `FirebaseMessagingService`
- [ ] Snooze limits enforced

### Exit gate

| # | Criterion |
|---|-----------|
| G3.1 | Task scheduled +2 min fires takeover on time |
| G3.2 | Ignoring takeover repeats alarm every 2 min |
| G3.3 | Start Focus runs timer with foreground notification |
| G3.4 | Server test push received on device |

---

## Future (documented, not scheduled)

| Item | Notes |
|------|-------|
| OpenAI evening coach | Nest cron → evaluation → FCM |
| Stripe penalties | [adr/006-penalties-deferred.md](adr/006-penalties-deferred.md) |
| DeviceAdmin anti-uninstall | Optional extreme measure |
| Week calendar view | UX enhancement |
| Habit reorder drag | UX enhancement |

---

## Suggested week-by-week breakdown

### Week 1

| Day | Focus |
|-----|-------|
| Mon–Tue | Server scaffold, Mongoose, habits API |
| Wed–Thu | Android Hilt/Room/Nav, theme, habits MVI |
| Fri | HabitGrid UI, habit check-ins |

### Week 2

| Day | Focus |
|-----|-------|
| Mon–Tue | Tasks + calendar features |
| Wed–Thu | Sync push/pull both sides |
| Fri | Gate G1 testing, deploy server to VM |

### Week 3–4

Morning lock, NFC, daily intent, evening review (Phase 2).

### Week 5–6

Alarms, takeover, focus service, FCM (Phase 3).

---

## Definition of done (per task)

- Code merged to main branch
- Lint passes (`./gradlew lint`, `npm run lint`)
- Manual test noted in PR or commit message
- Docs updated if API or behavior changed

## Related

- [stack.md](stack.md)
- [deployment.md](deployment.md)
- [design/user-flows.md](design/user-flows.md)
