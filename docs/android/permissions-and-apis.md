# Android Permissions & Platform APIs

Reference for Stella's enforcement features. Targets **API 31+** (minSdk 31).

## Permission matrix

| Permission | Phase | Purpose |
|------------|-------|---------|
| `SYSTEM_ALERT_WINDOW` | 2 | Draw morning lock over other apps |
| `NFC` | 2 | Read bathroom tag |
| `SCHEDULE_EXACT_ALARM` | 3 | Fire task reminders on time |
| `USE_FULL_SCREEN_INTENT` | 3 | Show takeover on lock screen |
| `POST_NOTIFICATIONS` | 2+ | Evening review + focus notification (API 33+) |
| `FOREGROUND_SERVICE` | 3 | Focus timer |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 3 | Declare focus timer type (API 34+) |
| `RECEIVE_BOOT_COMPLETED` | 2 | Reschedule alarms after reboot |
| `VIBRATE` | 3 | Alarm vibration |
| `WAKE_LOCK` | 3 | Keep alarm visible |

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

## Feature: Morning hostage (Phase 2)

### APIs

- `Settings.canDrawOverlays()` / `ACTION_MANAGE_OVERLAY_PERMISSION`
- Full-screen `Activity` with `showWhenLocked` + `turnScreenOn`, or `TYPE_APPLICATION_OVERLAY` window

### Implementation notes

```kotlin
// Activity flags for lock screen
setShowWhenLocked(true)
setTurnScreenOn(true)
```

- Launch from alarm `BroadcastReceiver` or `BootReceiver`
- Back button disabled while locked
- No production bypass — debug builds only may expose skip via `BuildConfig.DEBUG`

### Onboarding

Settings screen explains why overlay is required, links to system settings.

---

## Feature: NFC unlock (Phase 2)

### APIs

- `NfcAdapter.enableForegroundDispatch()` in `MorningLockActivity`
- `NfcAdapter.ACTION_TAG_DISCOVERED` intent filter
- Store enrolled tag id (e.g. tag serial) in EncryptedSharedPreferences

### Enrollment flow (Settings)

1. User taps "Register bathroom tag".
2. Activity listens for tag.
3. Save identifier; show success.

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
| NFC wrong tag rejected | 2 |
| NFC correct tag unlocks | 2 |
| Exact alarm fires at scheduled minute | 3 |
| Takeover shows on locked phone | 3 |
| Focus notification persists | 3 |
| Alarms survive reboot | 3 |

## Related

- [../design/user-flows.md](../design/user-flows.md)
- [../architecture/android.md](../architecture/android.md)
