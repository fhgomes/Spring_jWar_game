# Docker & Compose — Local Dev Guide

This document covers running jWar locally via the multi-stage `Dockerfile`
and the `docker-compose.yml` at the repository root. For the design
rationale, see [`specs/011-docker-and-compose/spec.md`](../specs/011-docker-and-compose/spec.md).

---

## Prerequisites

- **Docker 24+** (Engine + CLI)
- **Docker Compose v2** (`docker compose ...`, not the deprecated
  `docker-compose`)
- **GNU make** (optional; the `Makefile` is a thin wrapper around
  `docker compose`)
- A **Firebase service-account JSON** (see *First-run setup* below).
  Without it, the app container will fail health-checks and exit.

On macOS / Windows, Docker Desktop ships with all the above. On Linux,
install via your package manager and ensure your user is in the
`docker` group.

---

## First-run setup

1. **Clone the repo** and `cd` into it.

2. **Create your local `.env`**:

   ```bash
   cp .env.example .env
   ```

   Open `.env` and at minimum set `DB_PASSWORD` to anything strong. The
   compose stack will refuse to start if `DB_PASSWORD` is empty.

3. **Place the Firebase service-account key**:

   ```bash
   mkdir -p secrets
   # Drop the JSON downloaded from Firebase Console → Project Settings →
   # Service accounts → Generate new private key:
   cp ~/Downloads/your-firebase-creds.json secrets/firebase-service-account.json
   chmod 600 secrets/firebase-service-account.json
   ```

   The `secrets/` directory is `.gitignored`. Never commit it.

4. **Bring up the stack**:

   ```bash
   make up        # (or: docker compose up -d)
   ```

   First boot builds the multi-stage image — expect 3-5 minutes on a
   warm machine, 8-10 on a cold one. Subsequent boots are seconds.

5. **Verify** by browsing to <http://localhost:8080>. The
   `/api/health` endpoint should return `{"status":"UP"}`.

---

## Service ports

| Service    | Container | Host       | Override env var       |
|------------|-----------|------------|------------------------|
| app (REST) | 8080      | 8080       | `APP_HOST_PORT`        |
| postgres   | 5432      | 5432       | `POSTGRES_HOST_PORT`   |
| pgadmin*   | 80        | 5050       | `PGADMIN_HOST_PORT`    |

\* Only when running with `--profile dev-tools`.

---

## Common workflows

### Tail logs

```bash
make logs            # follows the app logs
docker compose logs -f postgres   # follows the database logs
```

### Connect to the database

```bash
make psql            # opens psql inside the postgres container
```

### Shell into the app container

```bash
make shell           # /bin/sh inside the running app container
```

### Rebuild after code changes

The Dockerfile is layered so:

- Frontend source changes invalidate the `frontend-builder` stage only;
  Gradle dependencies stay cached.
- Backend source changes reuse the npm cache + the Gradle dependency
  layer.

```bash
make rebuild         # docker compose build --no-cache && up
# or, for an incremental build:
docker compose build
docker compose up -d
```

### Start optional tools

```bash
make dev-tools-up    # adds pgAdmin at http://localhost:5050
make dev-tools-down  # stops the dev-tools profile
```

### Reset everything (DANGEROUS — wipes the database)

```bash
make clean           # docker compose down -v
```

---

## Troubleshooting

### Firebase service account file missing

**Symptom**: app container exits seconds after startup with a log line
mentioning `FB_SERVICE_ACCOUNT_PATH` or
`com.google.firebase.IllegalStateException: FirebaseApp ... is not
initialized`.

**Cause**: `./secrets/firebase-service-account.json` is missing on the
host, so the bind mount inside the container is empty / a directory.

**Fix**: follow step 3 of *First-run setup* above and re-run
`make up`.

---

### Postgres connection refused

**Symptom**: app log shows
`org.postgresql.util.PSQLException: Connection refused`.

**Cause(s)**:

1. The Postgres healthcheck has not yet succeeded but the app started
   anyway (rare — the compose stack uses `condition: service_healthy`).
2. You changed `DB_USERNAME` / `DB_PASSWORD` after the Postgres volume
   was already initialized; Postgres only reads `POSTGRES_*` env vars on
   *first* boot of a fresh volume.

**Fix for (2)**:

```bash
make clean           # WARNING: deletes all DB data
make up
```

---

### Port 8080 (or 5432) already in use

**Symptom**: `docker compose up` fails with
`bind: address already in use`.

**Cause**: another process on the host owns the port.

**Fix**: override the host-side port in `.env`:

```bash
APP_HOST_PORT=8081
POSTGRES_HOST_PORT=55432
```

Then `make rebuild` (or simply `make down && make up`).

---

### Image cache is stale / changes not picked up

**Symptom**: a code edit doesn't show up after `make up`.

**Cause**: Docker reused a cached image layer.

**Fix**:

```bash
make rebuild         # docker compose build --no-cache && up
```

If even that fails, prune dangling images:

```bash
docker image prune -f
```

---

### Frontend bundle missing (`/` returns a JSON 404)

**Symptom**: `GET http://localhost:8080/` returns
`{"timestamp":"...","status":404,"error":"Not Found","path":"/"}`.

**Cause**: the `frontend-builder` stage failed silently or the
`frontend/` directory is empty.

**Fix**: build with verbose output:

```bash
docker compose build --no-cache app
```

Look for the `frontend-builder` stage errors near the top of the output.

---

### Apple Silicon (`linux/arm64`) — `exec format error`

**Symptom**: the app container exits immediately with
`exec format error` in the logs.

**Cause**: the JAR was built for `amd64` on a CI runner and pulled on an
`arm64` Mac (or vice versa).

**Fix**: rebuild locally with `make rebuild`. The base images
(`node:20-alpine`, `eclipse-temurin:*`, `postgres:16-alpine`) are
multi-arch, so a fresh build picks the right architecture.

---

## How rebuilds are cached

The Dockerfile is organized so each layer changes only when a relevant
file changes:

| Layer                                  | Invalidated by                              |
|----------------------------------------|---------------------------------------------|
| `frontend-builder` — npm install       | `frontend/package*.json`                    |
| `frontend-builder` — `vite build`      | anything under `frontend/`                  |
| `backend-builder` — gradle deps        | `*.gradle`, `gradle.properties`             |
| `backend-builder` — copy source        | anything under `jwar-server/`               |
| `backend-builder` — embed bundle       | always (cheap)                              |
| `backend-builder` — `bootJar`          | source changes above                        |
| `runtime` — copy jar                   | jar content changes                         |

In practice: front-end-only changes don't trigger a Gradle rebuild;
backend-only changes don't trigger an `npm ci`.

---

## See also

- `Makefile` — convenience targets.
- `docker-compose.yml` — service definitions.
- `Dockerfile` — multi-stage build.
- `.env.example` — full list of supported variables.
- `specs/011-docker-and-compose/spec.md` — the feature spec this
  document operationalizes.
