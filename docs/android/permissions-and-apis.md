# Android Permissions & Platform APIs

Reference for Stella's enforcement features. Targets **API 31+** (minSdk 31).

## Permission matrix

| Permission | Phase | Purpose |
|------------|-------|---------|
| `SYSTEM_ALERT_WINDOW` | 2 | Draw morning lock over other apps (implemented) |
| `NFC` | 2 | Read bathroom tag |
| `SCHEDULE_EXACT_ALARM` | 2–3 | Morning wake alarm + task reminders |
| `USE_FULL_SCREEN_INTENT` | 2–3 | Morning alarm on lock screen + task takeover |
| `POST_NOTIFICATIONS` | 2+ | Morning alarm, evening review, enforcement FGS (API 33+) |
| `FOREGROUND_SERVICE` | 2–3 | Morning enforcement + focus timer |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 2–3 | Morning lock + focus timer (API 34+) |
| `RECEIVE_BOOT_COMPLETED` | 2 | Reschedule morning + task alarms after reboot |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 2 | Reliable wake alarm (setup wizard) |
| `VIBRATE` | 3 | Alarm vibration |
| `WAKE_LOCK` | 2–3 | Keep alarm visible |

Optional (future, not v1):

| Permission | Purpose |
|------------|---------|
| `BIND_DEVICE_ADMIN` | Prevent uninstall |

## Manifest declarations (reference)

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

## Feature: Morning hostage (Phase 2) — implemented

### Components

| Piece | Location |
|-------|----------|
| Wake scheduler | `MorningAlarmScheduler` (`setAlarmClock`) |
| Alarm receiver | `MorningAlarmReceiver` → FSI notification + `MorningLockActivity` |
| Enforcement FGS | `MorningLockEnforcementService` (relaunch + overlay when user leaves) |
| Overlay | `MorningLockOverlay` (`TYPE_APPLICATION_OVERLAY`, Compose with dedicated `LifecycleOwner`) |
| State | `MorningLockController` |
| Setup wizard | `MorningLockSetupActivity` |
| Boot reschedule | `BootReceiver` |

### APIs

- `Settings.canDrawOverlays()` / `ACTION_MANAGE_OVERLAY_PERMISSION`
- `AlarmManager.setAlarmClock()` + `MorningAlarmReceiver`
- `NotificationCompat` full-screen intent (`USE_FULL_SCREEN_INTENT`; on API 34+ user must allow full-screen intents in app notification settings)
- Test flow: launch `MorningLockActivity` first; enforcement/ringer/FSI start in `onResume` after `lockSurfaceReady` is set (overlay only shows after lock surface has been visible)
- Alarm audio: `MorningAlarmRinger` with progressive volume ramp (configurable in Settings → Morning lock); FSI notification channel is silent to avoid double playback
- Test unlock from wizard returns to setup **TEST** step (no `CLEAR_TASK` to main); wizard step persisted in encrypted prefs
- `MorningLockActivity` / `DailyIntentActivity`: `setShowWhenLocked`, `setTurnScreenOn`, `requestDismissKeyguard`
- `OnBackPressedDispatcher` callback consumes back while locked
- **Not in v1:** Accessibility service (power-off / uninstall block) — deferred for Play policy

### Onboarding

**Settings → Morning lock → Set up morning lock** runs the wizard (NFC, notifications, overlay, exact alarms, battery, test alarm).

### Unlock rule

NFC → `DailyIntentActivity` → save intent → `MorningLockController.stopEnforcement()`. NFC alone does not unlock the day.

---

## Feature: NFC unlock (Phase 2)

### APIs

- `NfcAdapter.enableForegroundDispatch()` in `MorningLockActivity`
- `NfcAdapter.ACTION_TAG_DISCOVERED` intent filter
- Store enrolled tag id (e.g. tag serial) in EncryptedSharedPreferences

### Enrollment flow (Developer Options)

1. Settings → **Advanced Developer Options** → **Register New NFC Tag**.
2. `NfcEnrollmentActivity` shows scan UI (Morning Lock visual pattern).
3. Save identifier to EncryptedSharedPreferences; return to Advanced screen.

### Verification

Compare scanned tag id to enrolled id (exact match). Reject unknown tags.

---

## Feature: Exact alarms (Phase 3)

### APIs

- `AlarmManager.setAlarmClock()` or `setExactAndAllowWhileIdle()` for task reminders
- `AlarmManager.canScheduleExactAlarms()` — request `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` if false

### Scheduling

- One alarm per task with `scheduledAt`
- Cancel/reschedule on task update/delete
- `PendingIntent` request codes derived from task id hash

### Boot receiver

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule all future task alarms from Room
    }
}
```

---

## Feature: Task takeover (Phase 3)

### APIs

- Dedicated `TaskTakeoverActivity` launched from alarm `BroadcastReceiver`
- `USE_FULL_SCREEN_INTENT` for notification on lock screen
- Notification channel: `IMPORTANCE_HIGH`, custom alarm sound (looping until dismiss)

### Alarm repetition

- If no action within 2 minutes, schedule next alarm via `AlarmManager`
- Stop loop on Start Focus, Skip, or Snooze (snooze reschedules takeover)

---

## Feature: Focus foreground service (Phase 3)

### APIs

- `startForeground()` with ongoing notification
- Type: `specialUse` with manifest property explaining accountability timer

```xml
<service
    android:name=".feature.focus.FocusForegroundService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="accountability_focus_timer" />
</service>
```

### Notification

- Shows task title + remaining time
- Tap returns to `FocusSessionActivity`
- Stop action ends session early

---

## Feature: Sync (Phase 1)

### APIs

- `WorkManager` for periodic and one-time sync
- Constraints: `NetworkType.CONNECTED`

No special permissions beyond network.

---

## Feature: FCM (Phase 3)

### Setup

- Firebase project + `google-services.json` in `Stella/app/`
- `FirebaseMessagingService` subclass
- Register token via `POST /notifications/register`

### Permissions

- `POST_NOTIFICATIONS` runtime request on API 33+

---

## Battery optimization

Prompt user to exclude Stella from battery restrictions:

```kotlin
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
    data = Uri.parse("package:${context.packageName}")
}
```

Show in Settings when alarms are unreliable.

## Testing checklist

| Test | Phase |
|------|-------|
| Overlay appears over Chrome | 2 |
| Morning test alarm (setup / diagnostics) | 2 |
| NFC wrong tag rejected | 2 |
| NFC correct tag unlocks | 2 |
| Home during lock returns to Stella | 2 |
| Wake alarm fires at configured time | 2 |
| Exact alarm fires at scheduled minute | 3 |
| Takeover shows on locked phone | 3 |
| Focus notification persists | 3 |
| Alarms survive reboot | 3 |

## Related

- [../design/user-flows.md](../design/user-flows.md)
- [../architecture/android.md](../architecture/android.md)
