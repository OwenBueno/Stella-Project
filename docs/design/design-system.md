# Design System

Dark, high-contrast, coach aesthetic. Stella should feel **serious and unavoidable** — not friendly or playful.

## Design principles

1. **Binary accountability** — habit cells are green or red; no neutral "pending" gray on past days.
2. **Urgency without clutter** — one primary action per enforcement screen.
3. **Readable at a glance** — large touch targets, high contrast, minimal decoration.
4. **No dynamic color in v1** — fixed palette for consistent habit grid meaning.

## Color tokens

Implemented in `com.stella.core.ui.theme.Color`:

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#0A0A0A` | Nav host / drawer fallback |
| `dawnGradientTop` | `#12141A` | Standard screen background (top) |
| `dawnGradientBottom` | `#1A1F35` | Standard screen background (bottom) |
| `dawnCardSurface` | `#1E2433` | Glass cards on dawn screens (`StellaCard`, panels) |
| `dawnCardBorder` | `#4DFFFFFF` | Card borders on dawn screens |
| `surface` | `#141414` | Elevated surfaces |
| `surfaceCard` | `#1C1C1C` | Legacy flat cards (prefer dawn tokens) |
| `surfaceVariant` | `#252529` | Grid empty/future cells |
| `primary` | `#FF4F00` | Brand accent, CTAs, today outline |
| `onPrimary` | `#FFFFFF` | Text on primary buttons |
| `success` | `#43A047` | Done habit, completed task |
| `error` | `#DC2626` | Missed habit, destructive |
| `textPrimary` | `#F5F5F5` | Headlines, body |
| `textSecondary` | `#D0D0D0` | Metadata, completed tasks, drawer unselected labels |
| `textMuted` | `#B8B8B8` | Eyebrow labels (`onSurfaceVariant`) |
| `border` | `#33FFFFFF` | Card outlines |
| `divider` | `#2E2E32` | Section rules, field borders |

### Dawn screen background (Home, Frontline, Calendar, Settings, Review)

Main surfaces use a vertical gradient (`DawnGradientTop` → `DawnGradientBottom`), not flat `#0A0A0A`. Wrap content in `DawnScreenBackground` from `core/ui/components`.

Group cards on Settings and Evening Review use `StellaCard`, which maps to `DawnCardSurface` + `DawnCardBorder` at 12dp radius.

### Habit grid cell colors (Habits screen)

| State | Color | Token |
|-------|-------|-------|
| Incomplete | `#1E2433` fill + subtle border | `DawnCardSurface` / `DawnCardBorder` |
| Done | `#43A047` @ ~35% + check | `success` |
| Today (incomplete) | orange border | `primary` |
| Today (done) | emerald fill + `PrimaryGlow` ring | `success` + `primary` |

Legacy `HabitGrid` on evening review may still show red MISSED until migrated.

### Habit grid cell colors (legacy review grid)

| State | Color | Token |
|-------|-------|-------|
| Done | `#43A047` | `success` |
| Missed | `#DC2626` | `error` |

## Typography

Material 3 type scale (`Type.kt`):

| Role | Style | Notes |
|------|-------|-------|
| Display | `headlineLarge`, bold italic | Section titles (`StellaDisplayTitle`) |
| Title | `titleMedium` / `titleLarge` | Top bar, dialogs |
| Body | `bodyLarge` / `bodyMedium` | Primary content |
| Label | `labelMedium` | Habit row names |
| Eyebrow | `labelSmall`, **12sp monospace** | `StellaLabel` — minimum readable size |

## Navigation

- **Modal drawer** (phone): hamburger in top bar opens `StellaNavigationDrawer` (~280dp)
- Six destinations: Control, Habits, Frontline, Calendar, Review, System
- Drawer items: 24dp icon + 16sp label; selected uses `primary` icon + `onSurface` text
- **Contextual top bar**: each main destination shows eyebrow + title in `StellaTopBar` (not a duplicate in-screen hero). Route → copy is centralized in `ScreenHeaders.kt` (`stellaScreenHeaderForRoute`).
- Settings subgraph (`settings/main`, `settings/advanced`, `settings/diagnostics`): back arrow in top bar; drawer still highlights `settings` on all sub-routes

## Spacing

| Token | dp |
|-------|-----|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 16 |
| `lg` | 24 |
| `xl` | 32 |

Screen horizontal padding: 24dp.

## Shape

| Element | Radius |
|---------|--------|
| Cards | 4dp |
| Drawer rows | 0dp (full-width highlight) |
| Habit cell | 0dp (square) |

## Components

### The Frontline (tasks)

- Dawn gradient + `dawnPanel` composer
- Tabs: text-only with `Primary` 2dp underline
- Task card: monospace sequence badge (`01`), `DragHandle`, schedule chip (`labelMedium`), glass card border; tap body opens edit sheet
- Completed section: muted, strikethrough, no drag handle
- Reorder via `sh.calvin.reorderable` on active list only; optimistic UI during drag, DB commit on `onDragStopped`

### Temporal Grid (calendar)

- Dawn gradient; month chevrons; borderless day cells
- Today: `Primary` filled circle + `PrimaryGlow` halo
- Status dots: `Success` (completions), `Primary` (events), `MorningLockPulseCore` (linked tasks)
- Day sheet + event editor bottom sheets; time steppers (no dropdown-in-sheet)

### Habits tracker grid

- Mon–Sun week; headers `M T W T F S S`; seven columns fit screen width (no horizontal scroll)
- `HabitsTrackerGrid` in `feature/habits`; tap toggles done ↔ incomplete; long-press shows `completedAt` tooltip

### HabitGrid (legacy)

- Used on evening review snapshot; 48×48dp cells; horizontal scroll

### StellaTopBar

- **Left:** menu (root tabs) or back arrow (settings advanced/diagnostics)
- **Center-left:** `StellaLabel` eyebrow + bold `headlineLarge` title (`StellaScreenHeader` from route)
- **Background:** `DawnGradientTop` blend; 2dp `Primary` accent rule underneath
- Screen body starts at first functional block — no second title row

| Route | Eyebrow | Title |
|-------|---------|-------|
| `home` | CONTROL | Control Center |
| `habits` | DISCIPLINE | Habits |
| `tasks` | OPERATIONS | The Frontline |
| `calendar` | TEMPORAL | Temporal Grid |
| `review` | PROTOCOL | Evening Review |
| `finances` | TREASURY | Finances |
| `settings/main` | USER CONFIG | Settings |
| `settings/advanced` | SYSTEM | Developer Options |
| `settings/diagnostics` | DEVELOPER | Diagnostics Console |

`StellaSectionHeader` / `StellaDisplayTitle` remain for rare standalone screens (e.g. morning lock, NFC enrollment), not main tabs.

### OutlinedTextField

Use `stellaTextFieldColors()` for focused `primary` border and readable labels.

## Morning flow (lock + daily intent)

Exception to the standard `#0A0A0A` scaffold for morning enforcement screens. Calm, premium dawn aesthetic.

| Token | Hex / value | Usage |
|-------|-------------|-------|
| `DawnGradientTop` / `MorningLockGradientTop` | `#12141A` | Gradient top, status bar |
| `DawnGradientBottom` / `MorningLockGradientBottom` | `#1A1F35` | Gradient bottom |
| `DawnCardSurface` | `#1E2433` | Daily intent panels (high contrast on gradient) |
| `DawnCardBorder` | white @ ~30% alpha | Panel outline |
| `DawnCardSurfaceSubtle` | `#252D42` | Planned task row highlight |
| `GlassBorder` / `GlassBorderEmpty` | white @ 12–20% alpha | Optional card borders |
| `PrimaryGradientEnd` | `#FF7A3D` | CTA gradient end |
| `MorningLockPulseRing` | white @ ~12% alpha | NFC scan rings (lock only) |
| `MorningLockPulseCore` | `#6B7FD7` @ ~30% alpha | NFC icon tint (lock only) |

**Morning lock:** eyebrow `StellaLabel`; hero `headlineLarge` bold without coach italic.

**Daily intent:** 12–16dp radii; compact today's plan list; browse list scroll only; `Start My Day` uses `Primary` → `PrimaryGradientEnd` when at least 3 tasks planned.

### Control Center (home)

Reuses dawn gradient + `DawnCardSurface` / `DawnCardBorder` panels. No coach italic on title.

| Element | Rule |
|---------|------|
| Clock | Monospace (`FontFamily.Monospace`), pattern `hh:mm:ss a`; ticks every second |
| Week pills | `Primary` / `PrimaryGlow` for today and selected day |
| Completion ring | Canvas arc on metric card; center percent + "Complete" label |
| Task rows | `PlayArrow` (in progress), outlined circle (todo); tap → task detail |
| Bottom actions | Outlined / glass chips (`DawnCardSurface` @ ~60% alpha), not solid orange buttons |

## Accessibility

- Minimum touch target: 48dp (drawer rows, cells)
- Eyebrow labels: 12sp minimum (not 10sp)
- Color is not sole indicator on habit cells (icons for DONE/MISSED)

## Compose theme mapping

- `StellaTheme` wraps Material3 `darkColorScheme` with tokens above
- API JSON uses `id`; maps from MongoDB `_id` on server
- Do not use dynamic color

## Related

- [screens.md](screens.md)
- [user-flows.md](user-flows.md)
