# Stella Documentation

Stella is a solo-use aggressive Life OS: habit grid, internal calendar, morning hostage flow (NFC unlock), and scheduled task enforcement.

## Quick links

| Document | Description |
|----------|-------------|
| [summary.md](summary.md) | Executive summary (product + stack overview) |
| [stack.md](stack.md) | Technology choices and conventions |
| [implementation-roadmap.md](implementation-roadmap.md) | Phased delivery plan with exit gates |
| [data-model.md](data-model.md) | Entities, Room/Mongoose mapping, sync fields |
| [deployment.md](deployment.md) | GCP VM, MongoDB Atlas, secrets |

## Architecture

| Document | Description |
|----------|-------------|
| [architecture/overview.md](architecture/overview.md) | System context, containers, risks |
| [architecture/android.md](architecture/android.md) | Packages, MVI, layers |
| [architecture/backend.md](architecture/backend.md) | NestJS modules and layers |
| [architecture/sync.md](architecture/sync.md) | Offline-first sync protocol |

## API & Android platform

| Document | Description |
|----------|-------------|
| [api/rest-api.md](api/rest-api.md) | REST contract (`/api/v1`) |
| [android/permissions-and-apis.md](android/permissions-and-apis.md) | Permissions, alarms, overlay, NFC |

## Design

| Document | Description |
|----------|-------------|
| [design/design-system.md](design/design-system.md) | Colors, typography, components |
| [design/screens.md](design/screens.md) | Screen inventory and layout notes |
| [design/user-flows.md](design/user-flows.md) | Morning, daytime, evening flows |

## Architecture Decision Records

| ADR | Topic |
|-----|-------|
| [adr/001-mvi.md](adr/001-mvi.md) | MVI presentation pattern |
| [adr/002-mongodb-atlas.md](adr/002-mongodb-atlas.md) | MongoDB Atlas over PostgreSQL |
| [adr/003-offline-first-sync.md](adr/003-offline-first-sync.md) | Room offline-first, LWW sync |
| [adr/004-api-key-auth.md](adr/004-api-key-auth.md) | Solo API key authentication |
| [adr/005-nfc-morning-unlock.md](adr/005-nfc-morning-unlock.md) | NFC-only morning unlock |
| [adr/006-penalties-deferred.md](adr/006-penalties-deferred.md) | Financial penalties deferred |

## Historical

| Document | Description |
|----------|-------------|
| [Initial.md](Initial.md) | Original brainstorm (superseded by structured docs above) |

## Repository layout

```text
Stella-Project/
├── Stella/          # Android app (Kotlin, Compose, MVI)
├── server/          # NestJS API
├── browser-design/  # UI prototype reference (React; not shipped)
├── docs/            # This directory
├── infra/           # Docker, nginx (deployment)
└── README.md        # Project quick start
```
