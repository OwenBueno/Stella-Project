# Design System

Dark, high-contrast, coach aesthetic. Stella should feel **serious and unavoidable** — not friendly or playful.

## Design principles

1. **Binary accountability** — habit cells are green or red; no neutral "pending" gray on past days.
2. **Urgency without clutter** — one primary action per enforcement screen.
3. **Readable at a glance** — large touch targets, high contrast, minimal decoration.
4. **No dynamic color in v1** — fixed palette for consistent habit grid meaning.

## Color tokens

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#0D0D0F` | App scaffold, full-screen takeover |
| `surface` | `#1A1A1E` | Cards, bottom sheets |
| `surfaceVariant` | `#252529` | Grid empty/future cells |
| `primary` | `#E53935` | Missed habit, destructive, alarm accent |
| `onPrimary` | `#FFFFFF` | Text on primary buttons |
| `success` | `#43A047` | Done habit, completed task |
| `warning` | `#FB8C00` | Snooze, time running out |
| `textPrimary` | `#F5F5F5` | Headlines, body |
| `textMuted` | `#9E9E9E` | Secondary labels, hints |
| `divider` | `#2E2E32` | Grid lines, list separators |
| `overlayScrim` | `#CC000000` | Morning lock backdrop |

### Habit grid cell colors

| State | Color | Token |
|-------|-------|-------|
| Done | `#43A047` | `success` |
| Missed (explicit or auto) | `#E53935` | `primary` |
| Future / not yet due | `#252529` | `surfaceVariant` |
| Today, not checked (before EOD) | `#252529` border `#FB8C00` | warning outline |

## Typography

Material 3 type scale with these overrides:

| Role | Style | Notes |
|------|-------|-------|
| Display | `headlineLarge`, bold | "Good morning" lock screen |
| Title | `titleLarge` | Screen titles |
| Body | `bodyLarge` | Forms, reviews |
| Label | `labelMedium` | Habit names in grid rows |
| Grid date | `labelSmall`, **monospace** | Column headers (Mon, Tue, …) |
| Timer | `displayMedium`, monospace | Focus session countdown |

## Spacing

| Token | dp |
|-------|-----|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 16 |
| `lg` | 24 |
| `xl` | 32 |

Screen horizontal padding: `md` (16dp).

## Shape

| Element | Radius |
|---------|--------|
| Cards | 12dp |
| Buttons | 8dp |
| Habit cell | 4dp |
| Full-screen takeover | 0dp (edge-to-edge) |

## Components

### HabitGrid

- `LazyVerticalGrid` or custom `Row`/`Column` layout
- Fixed row height: 48dp; cell: 32×32dp
- Row label: habit name, ellipsized, `labelMedium`
- Column headers: day abbreviation + date number (monospace)
- Tap cell: toggle `DONE` / `MISSED` for that day (today and past only)
- Long-press row: reorder (Phase 1.5) or edit habit

### TaskRow

- Leading: priority indicator (vertical bar: HIGH = red, MEDIUM = orange, LOW = muted)
- Title: `bodyLarge`
- Trailing: scheduled time or status chip
- Swipe: mark done (optional)

### Top3IntentCard

- Numbered slots 1–3
- Empty slot: dashed border, "Pick a frog"
- Filled: task title + estimated block time

### EnforcementBanner

- Full-width, `primary` background
- Copy: direct imperative ("Start focus session now")
- Single CTA button, full width

### FullScreenTakeover

- Black/scrim background
- Task title centered
- Countdown: "5:00 to start"
- Actions: **Start Focus** (filled), **Snooze** (text, muted — limited uses)

### EveningReviewForm

- Section 1: Planned vs actual (multiline)
- Section 2: Reflection (multiline)
- Embedded mini habit grid (read-only snapshot)
- Submit: "Close the day" (disabled until both sections touched)

### StellaButton

| Variant | Style |
|---------|-------|
| Primary | Filled, `primary` or `success` |
| Secondary | Outlined, `textPrimary` border |
| Danger | Filled `primary` for destructive confirm |

## Icons

Material Symbols Outlined:

- Habits: `grid_view`
- Tasks: `check_circle`
- Calendar: `calendar_today`
- Settings: `settings`
- NFC: `nfc`
- Alarm: `notifications_active`

## Motion

- Screen transitions: 200ms fade
- Habit cell toggle: 150ms color crossfade
- Takeover entrance: none (instant — feels abrupt by design)
- Focus timer: no playful animations

## Accessibility

- Minimum touch target: 48dp
- Color is not sole indicator: DONE/MISSED also use content description
- TalkBack labels on grid cells: "Drink Water, Tuesday, done"

## Compose theme mapping

Implement in `com.stella.core.ui.theme`:

- `StellaColors` object with tokens above
- `StellaTheme` wraps Material3 `darkColorScheme` with custom colors
- `MaterialTheme.colorScheme` — do not use dynamic color

## Related

- [screens.md](screens.md)
- [user-flows.md](user-flows.md)
