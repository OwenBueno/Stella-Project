# Stella Project

Aggressive personal Life OS — Android client + NestJS API.

## Structure

| Path | Description |
|------|-------------|
| [Stella/](Stella/) | Android app (Kotlin, Compose, MVI) |
| [server/](server/) | NestJS API |
| [docs/](docs/) | Architecture, design, API, roadmap |

## Quick start

### Documentation

Start at [docs/README.md](docs/README.md).

### Android

```bash
cd Stella
./gradlew assembleDebug
```

Requires Android Studio with SDK 31+. Configure `API_BASE_URL` and API key in app settings (Phase 1).

### Server

```bash
cd server
cp .env.example .env   # set DATABASE_URL, API_KEY
npm install
npx prisma generate
npm run start:dev
```

See [docs/deployment.md](docs/deployment.md) for GCP VM and MongoDB Atlas setup.
