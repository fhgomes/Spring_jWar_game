# Feature Specification: Docker & Compose Packaging for Local Dev and CI

**Feature Branch**: `011-docker-and-compose`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Container the app for local dev and CI. Single multi-stage Dockerfile builds frontend then jar; docker-compose orchestrates Postgres + app for one-command local startup."

> **Context sources**
>
> - Analysis: `docs/analysis/03-infra-auth-rest-state.md` §5 (existing
>   Dockerfile is single-stage, single-deployable image not produced today;
>   compose stack mounts bind-paths that only exist on the deploy host;
>   `JWARSV_APP_DB_*` env vars are set by compose but ignored by
>   `application.yml`).
> - Constitution: `.specify/memory/constitution.md` — Principle IV
>   (modular architecture), Principle V (Simplicity & YAGNI), Technology
>   Constraints (Postgres prod / H2 dev, Java 17, Undertow, no Tomcat).
> - Adjacent specs (assumed): the React + Vite frontend lives under
>   `frontend/` at repo root; Spring Boot serves the compiled bundle as
>   static assets; STOMP over WebSocket runs at `/ws`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One-image build of frontend + backend (Priority: P1)

A developer or CI runner produces a single deployable container image by
running `docker build` from the repository root. The build stages are
self-contained: stage one builds the React/Vite bundle, stage two builds
the Spring Boot fat jar and embeds the bundle as static resources, and
the runtime stage carries only the JRE and the jar.

**Why this priority**: Without a working multi-stage image build there
is no portable artifact; every other story (compose orchestration, CI
publish, native image) depends on it. The existing single-stage
`Dockerfile` (`jwar-server/Dockerfile:1-15`) only copies a pre-built jar
and assumes a Gradle build already ran on the host — that is not
portable for CI or new contributors.

**Independent Test**: Run `docker build -t jwar:local .` from the repo
root on a clean machine that has only Docker installed. Verify the
build completes, the final image runs, the `/actuator/health` endpoint
returns `200`, and the served `/index.html` is the production
frontend bundle.

**Acceptance Scenarios**:

1. **Given** a clean machine with Docker installed and the repository
   checked out, **When** the developer runs `docker build -t jwar:local
   .` from the repository root, **Then** the build completes
   successfully and produces a runnable image tagged `jwar:local`
   smaller than 350 MB.
2. **Given** the image `jwar:local` has been built, **When** the
   developer runs the container exposing port 8080 with a reachable
   Postgres, **Then** `GET http://localhost:8080/actuator/health`
   returns HTTP 200 with body `{"status":"UP"}` within 60 seconds.
3. **Given** the image is running, **When** the developer requests
   `GET http://localhost:8080/`, **Then** the response is the
   production-built React `index.html` containing hashed asset
   references and not a Vite dev server placeholder.
4. **Given** the image is built, **When** the developer inspects the
   running container's process list, **Then** the JVM is started by
   a non-root user and the JAR file is owned by that user.
5. **Given** the image is running, **When** Docker performs its
   configured `HEALTHCHECK`, **Then** the check probes
   `/actuator/health` and reports `healthy` once the service is
   responsive.

---

### User Story 2 - One-command local startup of Postgres + app (Priority: P1)

A developer brings up the full local stack — Postgres database and the
Spring Boot app — with a single `docker compose up` command. The app
waits for Postgres to be healthy before starting, picks up its
configuration from environment variables, and is reachable on
`http://localhost:8080`.

**Why this priority**: First-run friction is the single biggest barrier
to new contributors. The existing compose stack at
`others/docker/compose.yaml` requires bind-mount paths
(`/var/postgresql/jwarsv-data`, `/var/logs/jwarsv-*`,
`docs/analysis/03-infra-auth-rest-state.md` §5.2) that exist only on the
maintainer's deploy host, so it does not work for new developers
out of the box. A portable compose at the repo root unblocks
everyone.

**Independent Test**: On a clean machine with Docker installed and a
populated `.env`, run `docker compose up -d`; within 60 seconds the app
container reports `healthy` and `curl http://localhost:8080/actuator/health`
returns 200.

**Acceptance Scenarios**:

1. **Given** a developer has just cloned the repository and copied
   `.env.example` to `.env`, **When** they run `docker compose up -d`,
   **Then** both the `postgres` and `app` services start, the `app`
   service reaches `healthy` status, and the API is reachable on
   `http://localhost:8080`.
2. **Given** the compose stack is running, **When** the developer runs
   `docker compose down` followed by `docker compose up -d`,
   **Then** the Postgres data persists between restarts via a named
   volume (no bind mount to host paths).
3. **Given** the `app` service is starting, **When** Postgres is not
   yet accepting connections, **Then** the `app` container waits for
   the Postgres `pg_isready` healthcheck to succeed before starting
   the JVM.
4. **Given** the developer activates the `dev-tools` compose profile,
   **When** they run `docker compose --profile dev-tools up -d`,
   **Then** an optional `pgadmin` service is started in addition to
   the base services.
5. **Given** the app reads its datasource and Firebase configuration
   from environment variables, **When** the compose stack starts the
   app with values from `.env`, **Then** the Spring Boot logs show
   the resolved JDBC URL host/port and the Firebase service-account
   file is mounted into the container at the expected path.

---

### User Story 3 - Developer ergonomics: env template, shortcuts, docs (Priority: P2)

A new contributor can complete first-run setup in under 10 minutes by
following a single documented checklist. A committed `.env.example`
lists every required variable with explanations, a `Makefile` provides
common shortcuts, and a `docs/docker.md` walks through the workflow
and the most common errors.

**Why this priority**: The build artifacts from US1 and US2 are
worthless if contributors cannot find or remember the commands and
variables. Reducing setup friction has high leverage but is not on
the critical path of the runtime itself, hence P2.

**Independent Test**: Hand the repository to a teammate who has never
seen it; they read `docs/docker.md`, follow its instructions, and
have a healthy app running locally without asking for help.

**Acceptance Scenarios**:

1. **Given** a fresh clone, **When** the developer copies
   `.env.example` to `.env`, **Then** every variable referenced by
   the compose stack or `application-docker.yml` is present in the
   template with a comment describing its purpose and a safe default
   (or an explicit "REQUIRED" marker for secrets like the Firebase
   key).
2. **Given** the repository contains a `Makefile` at the root,
   **When** the developer runs `make up`, **Then** the compose stack
   starts in detached mode; `make down` stops it; `make logs` tails
   the app logs; `make rebuild` rebuilds the image without cache;
   `make psql` opens a `psql` shell against the running database;
   `make shell` opens an interactive shell inside the app container.
3. **Given** the developer encounters a Firebase-key-missing error,
   **When** they consult `docs/docker.md` troubleshooting section,
   **Then** they find a specific entry describing the symptom, the
   root cause, and the fix.

---

### User Story 4 - Publish images to GitHub Container Registry (Priority: P2)

When the maintainer pushes a Git tag matching `v*` (e.g. `v0.1.0`), a
GitHub Actions workflow builds the multi-stage image and pushes it to
`ghcr.io/fhgomes/jwar:<tag>`. The same workflow optionally builds a
GraalVM native-image variant when explicitly requested.

**Why this priority**: Required for any deployment story beyond the
maintainer's laptop, but not blocking for local development. The
native-image path is optional and disabled by default (P3 sub-story).

**Independent Test**: Push a test tag `v0.0.1-test` from a fork; the
workflow runs to completion and the image appears under the GHCR
package list with the matching tag.

**Acceptance Scenarios**:

1. **Given** the workflow `.github/workflows/docker.yml` is in place,
   **When** a Git tag `v0.1.0` is pushed to the repository,
   **Then** the workflow builds the multi-stage image and pushes it
   to `ghcr.io/fhgomes/jwar:v0.1.0` and `ghcr.io/fhgomes/jwar:latest`.
2. **Given** the workflow is invoked manually via `workflow_dispatch`
   with `build_native: true`, **When** it runs, **Then** it also
   builds and pushes the `runtime-native` Dockerfile target to a
   `ghcr.io/fhgomes/jwar:v0.1.0-native` tag.
3. **Given** a Git push to a branch other than a tag, **When** the
   workflow's tag-trigger condition is evaluated, **Then** no
   registry push occurs.

---

### Edge Cases

- What happens when the developer runs `docker compose up` without
  having copied `.env.example` to `.env`? Compose should fail fast
  with a clear message identifying the missing variables; documented
  in `docs/docker.md`.
- What happens when Postgres is healthy but the Firebase service
  account file is missing from the host mount? The app should log a
  precise error and exit non-zero (so compose marks it `unhealthy`)
  rather than starting in a broken state.
- What happens when the developer rebuilds the image after changing
  only frontend files? The frontend-builder stage cache layer is
  invalidated, but the backend-builder stage's Gradle cache layer is
  reused, keeping the rebuild fast.
- What happens when two contributors pick the same Postgres port
  (5432) on their machines? `.env.example` exposes
  `POSTGRES_HOST_PORT` so each developer can override the host-side
  port without touching tracked files.
- What happens when the static asset bundle is missing because the
  frontend build failed? The backend-builder stage should fail with
  a clear error pointing at the failed frontend step rather than
  producing a jar without the bundle.
- What happens on Apple Silicon (`linux/arm64`) hosts? The
  base images must be multi-arch or the build fails with a clear
  `--platform` mismatch message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a single multi-stage `Dockerfile` at
  the repository root with three named stages: `frontend-builder`,
  `backend-builder`, and `runtime`.
- **FR-002**: The `frontend-builder` stage MUST use `node:20-alpine`,
  copy the `frontend/` directory, run `npm ci` followed by
  `npm run build`, and emit the built bundle to a location reachable
  by the next stage (e.g. `/app/static`).
- **FR-003**: The `backend-builder` stage MUST use
  `eclipse-temurin:17-jdk-alpine`, copy `jwar-server/`, copy the
  static output from the `frontend-builder` stage into
  `jwarsv-sboot/src/main/resources/static/`, and run
  `./gradlew :jwarsv-sboot:bootJar`.
- **FR-004**: The `runtime` stage MUST use
  `eclipse-temurin:17-jre-alpine`, copy only the resulting boot jar
  from the `backend-builder` stage, run as a non-root user (e.g.
  `appuser`), `EXPOSE 8080`, define an `ENTRYPOINT` that launches
  the JVM with sensible heap flags and
  `-Dspring.profiles.active=docker`, and declare a `HEALTHCHECK`
  that probes `/actuator/health`.
- **FR-005**: System MUST provide a `.dockerignore` at the repository
  root that excludes at minimum `.git`, `.gradle`, `node_modules`,
  `build/`, `out/`, `*.iml`, `.idea/`, `frontend/dist`, and
  `frontend/node_modules`.
- **FR-006**: System MUST provide a `docker-compose.yml` at the
  repository root defining a `postgres` service using
  `postgres:16-alpine` with a named volume for `/var/lib/postgresql/data`,
  a `pg_isready` healthcheck, and environment values populated from
  an `.env` file.
- **FR-007**: The compose `app` service MUST depend on the `postgres`
  service with `condition: service_healthy`, read its datasource
  credentials and Firebase service-account path from `.env`-sourced
  environment variables, mount the Firebase service-account JSON
  file as a read-only volume, and publish port `8080` to the host.
- **FR-008**: The compose stack MUST include a commented-out (or
  profile-gated, `dev-tools`) `pgadmin` service for optional
  database inspection.
- **FR-009**: System MUST add a Spring Boot profile-specific config
  file `application-docker.yml` under
  `jwarsv-sboot/src/main/resources/`, resolving the datasource URL,
  username, password, Firebase service-account path, and the
  application base URL from environment variables
  (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FB_SERVICE_ACCOUNT_PATH`,
  `APP_BASE_URL`) with sane defaults appropriate for compose.
- **FR-010**: System MUST commit `.env.example` at the repository
  root documenting every required variable with comments and safe
  defaults or `REQUIRED` markers, and MUST add `.env` to
  `.gitignore`.
- **FR-011**: System MUST provide a `Makefile` at the repository root
  with at least the following targets: `up` (compose up detached),
  `down` (compose down), `logs` (tail app logs), `rebuild`
  (compose build --no-cache then up), `psql` (open psql in the
  database container), `shell` (open a shell in the app container).
- **FR-012**: System MUST provide developer documentation at
  `docs/docker.md` covering first-run steps, port assignments, the
  variables in `.env`, and a troubleshooting section with at minimum
  entries for: missing Firebase service-account file, Postgres
  connection refused, port 8080 already in use, and stale image
  cache.
- **FR-013**: System MUST provide a GitHub Actions workflow at
  `.github/workflows/docker.yml` that, on Git tags matching `v*`,
  builds the multi-stage image and pushes it to
  `ghcr.io/fhgomes/jwar:<tag>` and `ghcr.io/fhgomes/jwar:latest`.
- **FR-014**: The Dockerfile SHOULD provide an additional optional
  stage `runtime-native` that builds a GraalVM native-image variant
  via the existing `bootBuildImage` Gradle task. This stage MUST be
  off by default and build only when explicitly requested
  (`--target runtime-native` or workflow input).
- **FR-015**: System MUST fix the existing infrastructure typos that
  block the new flow: `scanBasePackages` in
  `JWarBackCoreApplication.java:6` (`br.com.jwar.server` →
  `br.com.bnuuy.jwar`) and Flyway `locations` in `application.yml:41`
  (`classpath:db.migration` → `classpath:db/migration`). See
  `docs/analysis/03-infra-auth-rest-state.md` §2.3 and §2.4.
- **FR-016**: The compose stack MUST NOT depend on host bind-mount
  paths outside the repository (e.g. `/var/postgresql/...`,
  `/var/logs/...`). Persistent data MUST use named Docker volumes.
- **FR-017**: The final runtime image MUST run as a non-root user and
  MUST NOT bundle build tools (Node, Gradle, JDK) — only the JRE
  and the jar.

### Key Entities

- **ContainerImage**: The deployable artifact produced by the
  Dockerfile. Attributes: name (`jwar`), tag (semver or `latest`),
  size, layers, exposed ports, entrypoint, healthcheck.
- **ComposeService**: A service definition inside
  `docker-compose.yml`. Attributes: name (`postgres`, `app`,
  `pgadmin`), image, ports, environment, volumes, depends_on,
  healthcheck, profiles.
- **EnvVar**: A single key documented in `.env.example`. Attributes:
  name, default value (or `REQUIRED` marker), human-readable
  description, scope (compose-only vs. read by the JVM at runtime).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new contributor with only Docker installed can
  produce a running app via
  `cp .env.example .env && docker compose up -d` in fewer than 5
  shell commands, including the clone and the env-file copy.
- **SC-002**: From a cold `docker compose up`, the `app` service
  reports `healthy` in fewer than 60 seconds on a developer-grade
  machine (4-core, 16 GB RAM).
- **SC-003**: A full clean rebuild (`docker build --no-cache`)
  completes in fewer than 4 minutes on the same reference machine.
- **SC-004**: The runtime image is smaller than 350 MB
  (compressed registry size).
- **SC-005**: The compose stack survives `docker compose down`
  followed by `docker compose up -d` without losing Postgres data
  (named volume persistence).
- **SC-006**: 100% of variables referenced by `docker-compose.yml`
  and `application-docker.yml` are documented in `.env.example`.
- **SC-007**: When a Git tag `v*` is pushed, the published image
  appears under `ghcr.io/fhgomes/jwar` within 10 minutes of the
  workflow start.

## Assumptions

- The React + Vite frontend lives under `frontend/` at the
  repository root (decision recorded in the adjacent frontend spec).
- Spring Boot serves the production frontend bundle as static
  resources from `src/main/resources/static/` (analysis §6.8
  recommendation A).
- The runtime image targets `linux/amd64`; multi-arch builds are
  out of scope for v1 but base images are chosen to support adding
  `linux/arm64` later without rewriting the Dockerfile.
- Postgres 16 is acceptable as the local-dev database engine.
  Production may use the same version or a managed equivalent.
- The Firebase service-account JSON file is provided by the
  developer out-of-band and mounted via a path documented in
  `.env.example` (decision recorded in
  `docs/analysis/03-infra-auth-rest-state.md` §8 Q2).
- The repository name on GHCR is `fhgomes/jwar`; image visibility
  is private by default and made public when the maintainer
  promotes a release.
- GraalVM native-image build remains experimental and is not
  required for any user-facing functionality.
- The existing `others/docker/` stack stays for now as a historical
  deploy artifact but is no longer the recommended local-dev path.
- The `.env` filename is `.env` and is read by Docker Compose
  automatically without additional flags.
