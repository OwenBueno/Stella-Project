# Technology Stack

Single source of truth for Stella. Supersedes PostgreSQL mentions in [Initial.md](Initial.md).

## Android client

| Concern | Technology | Notes |
|---------|------------|-------|
| Language | Kotlin | JVM 11+ |
| UI | Jetpack Compose + Material 3 | Fixed dark theme tokens; no dynamic color in v1 |
| Architecture | Clean Architecture + **MVI** | `UiState`, `UiEvent`, `ViewModel` per feature |
| DI | Hilt | `@HiltAndroidApp`, feature modules |
| Local DB | Room | Offline-first source of truth |
| Async | Coroutines + Flow | Repositories expose `Flow` |
| Navigation | Navigation Compose | Single-activity |
| HTTP | Retrofit + OkHttp + kotlinx.serialization | Base URL via `BuildConfig` |
| Secure storage | EncryptedSharedPreferences | API key, device id |
| Background | WorkManager, AlarmManager, Foreground Service | See [android/permissions-and-apis.md](android/permissions-and-apis.md) |

### Build tooling

| Tool | Version | Notes |
|------|---------|-------|
| Android Gradle Plugin | 8.9.1 | AGP 9.x + Hilt plugin incompatible as of initial scaffold |
| Gradle | 9.4.1 | Wrapper in `Stella/` |

### SDK targets

| Setting | Value |
|---------|-------|
| minSdk | 31 (Android 12+) |
| targetSdk | 36 |
| compileSdk | 36 |

## Backend

| Concern | Technology | Notes |
|---------|------------|-------|
| Runtime | Node.js 20 LTS | |
| Framework | NestJS | TypeScript, modular |
| ODM | Mongoose + `@nestjs/mongoose` | Schemas in `server/src/database/schemas/` |
| Database | MongoDB Atlas | Managed cluster; local standalone OK for dev |
| Auth | API key header | `X-Api-Key`; see [adr/004-api-key-auth.md](adr/004-api-key-auth.md) |
| Push (Phase 3) | Firebase Admin SDK | FCM to device |
| AI (later) | OpenAI API | Nest `@Cron` evening job |
| Hosting | GCP VM | Docker Compose: API + nginx |

## Monorepo layout

```text
Stella-Project/
├── Stella/                 # Android application module
├── server/                 # NestJS API
├── docs/
├── infra/                  # docker-compose, nginx configs
└── .env.example            # Server env template (no secrets committed)
```

## Conventions

### Android packages

- Root: `com.stella`
- Feature modules: `presentation`, `domain`, `data` subpackages

### API

- Base path: `/api/v1`
- JSON request/response bodies
- Errors: `{ "code": string, "message": string }`
- Timestamps: ISO-8601 UTC strings
- IDs: client-generated UUID v4 for all entities created on device

### Git

- Conventional commits encouraged (`feat:`, `fix:`, `docs:`)
- No secrets in repository

### Testing (when implemented)

| Layer | Tool |
|-------|------|
| Android unit | JUnit 5, Turbine (Flow), MockK |
| Android UI | Compose UI Test |
| Server unit | Jest |
| Server e2e | Supertest |

## Explicitly out of scope (v1)

- Google Calendar sync
- Multi-user / OAuth
- Stripe / financial penalties (documented for future — [adr/006-penalties-deferred.md](adr/006-penalties-deferred.md))
- Play Store distribution requirements (solo/sideload assumed)

## Upgrade paths (documented, not built)

| Need | Path |
|------|------|
| Multi-device | Keep UUID + sync; add JWT per user later |
| Public release | Privacy policy, Play policy review for overlay apps |
| Rich analytics | LifeLog aggregation module on server |
