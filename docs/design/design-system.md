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
| `background` | `#0A0A0A` | App scaffold |
| `surface` | `#141414` | Elevated surfaces |
| `surfaceCard` | `#1C1C1C` | Cards, drawer sheet |
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

### Habit grid cell colors

| State | Color | Token |
|-------|-------|-------|
| Done | `#43A047` | `success` |
| Missed (explicit or auto) | `#DC2626` | `error` |
| Future / not yet due | transparent | — |
| Today, not checked | border `#FF4F00` | `primary` |
| Past, no check-in | border `error` @ 65% alpha | auto-missed |

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
- Six destinations: Control, Matrix, Frontline, Calendar, Review, System
- Drawer items: 24dp icon + 16sp label; selected uses `primary` icon + `onSurface` text
- Task detail: back arrow only; drawer disabled

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

### HabitGrid

- Row label: `bodyMedium`, `onSurface`
- Column headers: `StellaLabel` (12sp monospace)
- Cell: 48×48dp; tap toggles DONE/MISSED

### StellaTopBar

- Menu icon (root screens) or back arrow (task detail)
- Brand title + status eyebrow
- 2dp `primary` accent rule

### OutlinedTextField

Use `stellaTextFieldColors()` for focused `primary` border and readable labels.

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
