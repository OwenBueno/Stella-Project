# Stella Project

Aggressive personal Life OS — Android client + NestJS API.

## Structure

| Path | Description |
|------|-------------|
| [Stella/](Stella/) | Android app (Kotlin, Compose, MVI) |
| [server/](server/) | NestJS API |
| [browser-design/](browser-design/) | UI prototype reference (React; not shipped) |
| [docs/](docs/) | Architecture, design, API, roadmap |
| [infra/](infra/) | Docker Compose + nginx |

## Quick start

### Documentation

Start at [docs/README.md](docs/README.md).

### Server

```bash
cd server
cp .env.example .env   # set DATABASE_URL (MongoDB Atlas or local), API_KEY
npm install
npm run start:dev
```

Health: `GET http://localhost:3000/api/v1/health`  
Habits: `GET http://localhost:3000/api/v1/habits` with header `X-Api-Key: <API_KEY>`

### Android

```bash
cd Stella
./gradlew assembleDebug
```

Requires JDK 11+ and Android SDK 31+. Emulator uses `http://10.0.2.2:3000` by default. Set API key in **System** settings before sync.

### Docker (API on VM or local)

```bash
# Configure server/.env first
docker compose -f infra/docker-compose.yml up -d --build
```

See [docs/deployment.md](docs/deployment.md) for GCP VM, Atlas IP allowlist, and TLS (certbot + `infra/nginx.conf` HTTPS block).

## Phase 1 exit gates (manual)

| Gate | How to verify |
|------|----------------|
| G1.1 | Create 3 habits, check in 7 days — green/red cells on Matrix |
| G1.2 | Add tasks (Frontline) and calendar events offline |
| G1.3 | Settings → Sync now; reinstall app → Sync now with same API key restores data |
| G1.4 | App runs on API 31+ emulator or device |
