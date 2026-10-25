# Feature Specification: REST API Foundation (jwarsv-sboot)

**Feature Branch**: `003-server-rest-foundation`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Lay down the REST API foundation in `jwarsv-sboot` — health/info endpoints, a global exception handler that translates `GameRulesException` to PT-BR error payloads, DTOs with MapStruct mappers, OpenAPI/Swagger documentation, Bean Validation, a correlation-ID middleware, environment-aware CORS, per-endpoint rate limiting, and the cleanup of three known wiring bugs that block the rest of the platform from booting."

This is the prerequisite spec for everything else in the platform (auth — Spec 004, rooms — Spec 005, realtime — Spec 006). Until the REST surface and its plumbing exist, no other server-side feature can be exposed to the UI. The decisions here are anchored in `docs/analysis/03-infra-auth-rest-state.md` §1–§2 (current state) and §6.1, §6.6, §6.7 (gaps).

Constitution alignment:
- **Principle I** — the REST layer lives strictly in `jwarsv-sboot`; `jwarsv-core` MUST NOT gain any Spring Web import as a result of this work.
- **Principle IV** — `sboot` depends on `core`, never the reverse; mappers translate core domain objects into HTTP-facing DTOs.
- **Principle V** — no speculative versioning scheme, no HATEOAS, no Spring Data REST exposure. Plain `@RestController` + DTO + MapStruct.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Operable, observable HTTP surface (Priority: P1)

As an operator deploying jWar, I need a running Spring Boot application that exposes a small set of health/info endpoints, returns uniform JSON error envelopes, and never leaks `jwarsv-core` domain types over the wire — so that the platform is operable, debuggable, and ready to host the auth, rooms, and realtime features that follow.

**Why this priority**: Nothing else can ship without this. The application as it exists today (per `docs/analysis/03-infra-auth-rest-state.md` §2.1) is a three-file shell with no controllers, no exception handler, no DTOs, no mappers. Auth (Spec 004), rooms (Spec 005), and realtime (Spec 006) all assume the foundation here is in place.

**Independent Test**: Boot the app locally (`./gradlew bootRun -Dspring.profiles.active=dev`), hit `GET /api/health`, see a 200 with `{ "status": "UP", "version": "...", "commit": "..." }`; deliberately trigger a `GameRulesException` from a stub controller and observe a 400 with PT-BR `error.message`; verify no field of any DTO references a `jwarsv-core` class (FQN audit at the package boundary).

**Acceptance Scenarios**:

1. **Given** the app is running, **When** a client calls `GET /api/health`, **Then** the response is 200 with a JSON body containing `status`, `version`, and `gitCommit` fields.
2. **Given** the app is running, **When** a client calls `GET /actuator/health` from an allowed source, **Then** the response is 200 and includes liveness/readiness details (configured via `application.yml:102-114`, already in place per analysis §2.4).
3. **Given** a controller throws `GameRulesException("Não é póssível entrar, a sala está cheia")` (real message from `ClassicGameLobby.java:36`), **When** the global handler intercepts it, **Then** the response is 400 with `{ "error": { "code": "GAME_RULES_VIOLATION", "message": "Não é póssível entrar, a sala está cheia", "correlationId": "..." } }`.
4. **Given** any unhandled exception escapes a controller, **When** the global handler intercepts it, **Then** the response is 500 with a generic PT-BR message and a `correlationId` matching the MDC value logged server-side.
5. **Given** a `@RestController` method returns a `ClassicGamePlayer` directly, **When** the build runs the architecture check, **Then** the build fails because core domain types must not leak across the boundary.

---

### User Story 2 - Documented, validated API (Priority: P1)

As a frontend developer integrating against the jWar server, I need every public endpoint to be documented via OpenAPI, every request body to be validated with consistent field-level error reporting, and a stable correlation-ID I can echo in bug reports — so that I can build and debug the UI without reading server source code.

**Why this priority**: Without OpenAPI the UI team has to read controller source to know what to call. Without validation, malformed payloads reach the engine and produce confusing PT-BR `GameRulesException` messages that aren't the right shape for "you sent the wrong JSON." Correlation IDs are the cheapest possible piece of operability and are required for any meaningful production support.

**Independent Test**: Open `/swagger-ui.html`, confirm every controller in the project is listed with PT-BR descriptions on game-domain types; POST an invalid payload (e.g. missing required field) and observe a 400 with field-level details; capture the `X-Correlation-Id` response header on any request and grep for it in server logs.

**Acceptance Scenarios**:

1. **Given** the app is running, **When** a developer opens `/swagger-ui.html`, **Then** the UI renders all `/api/**` endpoints with method, path, request schema, response schema, and example values.
2. **Given** the app is running, **When** a developer fetches `/v3/api-docs`, **Then** the response is a valid OpenAPI 3.x JSON document.
3. **Given** a request body fails Bean Validation (e.g. `@NotBlank` field is empty), **When** the controller is invoked, **Then** the response is 400 with `{ "error": { "code": "VALIDATION_FAILED", "fields": [{ "field": "name", "message": "must not be blank" }], "correlationId": "..." } }`.
4. **Given** a request arrives with no `X-Correlation-Id` header, **When** the middleware processes it, **Then** the server generates a UUID, sets it in MDC for the duration of the request, includes it on the response as `X-Correlation-Id`, and includes it on every log line emitted during request processing.
5. **Given** a request arrives with an existing `X-Correlation-Id` header, **When** the middleware processes it, **Then** the server reuses the inbound value (after sanity-checking it as a UUID; otherwise generates a fresh one).

---

### User Story 3 - Safe cross-origin and rate-limited access (Priority: P2)

As the security/operations owner, I need CORS configured tightly so the dev Vite server can talk to the local Spring app without exposing the prod API to arbitrary origins, and I need lightweight per-endpoint rate limits on auth and gameplay endpoints so a single misbehaving (or hostile) client cannot exhaust server resources — so that the platform is safe to deploy publicly.

**Why this priority**: P2 because in v1 the platform is not yet public; however, by the time auth ships (Spec 004) we need CORS resolved and a basic rate-limit safety net in place. Bucket4j is the chosen mechanism per analysis §6.6.

**Independent Test**: From `http://localhost:5173` (Vite dev origin), make a `fetch` to `http://localhost:8080/api/health` and confirm CORS headers permit it; from any non-dev origin in prod, confirm the same call is rejected. Hammer `POST /api/auth/register` past its configured threshold and observe 429 responses.

**Acceptance Scenarios**:

1. **Given** the `dev` profile is active, **When** an XHR originates from `http://localhost:5173`, **Then** the response includes `Access-Control-Allow-Origin: http://localhost:5173`.
2. **Given** the `prod` profile is active, **When** an XHR originates from `https://app.bnuuywar.com` (same origin as the API per analysis §5.4 nginx config), **Then** the response succeeds; **When** it originates from `https://evil.example`, **Then** the response is rejected.
3. **Given** an endpoint configured at `5 req/min/IP`, **When** the 6th request from a single IP arrives within a minute, **Then** the response is 429 with `Retry-After` header and `{ "error": { "code": "RATE_LIMITED", "message": "Limite de requisições excedido", "correlationId": "..." } }`.
4. **Given** any `/api/**` endpoint, **When** the rate-limit decision is made, **Then** the bucket key combines `IP` and (if authenticated) Firebase UID to prevent IP-based DOS from co-NATed users.

---

### User Story 4 - Fix existing wiring bugs that block the rest of the platform (Priority: P3)

As a maintainer, I want the three known startup-blocking typos in the current `jwarsv-sboot` to be fixed inside this spec — not deferred — so that no downstream feature has to work around them.

**Why this priority**: P3 in user-impact terms (no user can see these directly) but a P1 dependency for every other spec. Per analysis §8 decision 15, these typos are intentionally folded into this first infra spec rather than carrying a standalone cleanup branch.

**Independent Test**: Inspect the three affected lines after this spec ships and confirm the values match the corrections below; boot the app against Postgres and confirm Flyway picks up a placeholder migration under `src/main/resources/db/migration/`.

**Acceptance Scenarios**:

1. **Given** the application class at `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java:6`, **When** this spec is implemented, **Then** the `scanBasePackages` value is `"br.com.bnuuy.jwar"` (not `"br.com.jwar.server"`), so component scanning covers both `core` and `sboot`.
2. **Given** the Flyway configuration at `jwar-server/jwarsv-sboot/src/main/resources/application.yml:41`, **When** this spec is implemented, **Then** the `locations` value is `classpath:db/migration` (slash, not dot), so a migration placed at the conventional `src/main/resources/db/migration/V1__init.sql` is discovered.
3. **Given** the datasource block at `application.yml:16-22` (which today, per analysis §2.4, has no `url`, no `username`, no `password`), **When** this spec is implemented, **Then** the block reads from `JWARSV_APP_DB_HOST`, `JWARSV_APP_DB_PORT`, `JWARSV_APP_DB_NAME`, `JWARSV_APP_DB_USER`, `JWARSV_APP_DB_PW` (the env vars compose already passes per analysis table §5.5) with sensible local defaults so `./gradlew bootRun` works on a fresh checkout.

---

### Edge Cases

- A controller method throws `GameRulesException` whose message contains characters outside Basic Latin-1 (PT-BR accented characters in `ClassicGameLobby.java:36` like `"póssível"`): the response MUST preserve the UTF-8 bytes; charset header MUST be `application/json;charset=UTF-8`.
- A client sends an extremely large JSON payload: Spring Boot's default `spring.mvc.async.request-timeout` and request size limits MUST be set explicitly (default 1 MB on inbound JSON; configurable in `application.yml`).
- A request arrives during shutdown (`server.shutdown: graceful` is already set at `application.yml:3`): in-flight requests complete; new requests get 503.
- A request arrives with a malformed `X-Correlation-Id` (e.g. SQL injection attempt): server discards it and generates a fresh UUID; never logs the raw inbound value at higher than DEBUG.
- A bucket4j store is in-memory only for v1: a server restart resets all rate-limit counters; acceptable per Principle V.
- Swagger UI MUST NOT be reachable on a production deployment without authentication once auth ships (Spec 004 will gate it).
- An endpoint annotated with `@Valid` receives an empty body where a body was required: handler returns 400 `VALIDATION_FAILED`, not 500.

---

## Requirements *(mandatory)*

### Functional Requirements

**Health & info**

- **FR-001**: The system MUST expose `GET /api/health` returning `{ status: "UP" | "DOWN", version: string, gitCommit: string, builtAt: ISO-8601 string }`. The `version` and `gitCommit` MUST be sourced at build time from Gradle (write a `git.properties` via `org.springframework.boot` or `com.gorylenko.gradle-git-properties`).
- **FR-002**: The system MUST expose `GET /actuator/health` with the existing configuration at `application.yml:107` (`health,info` exposed). Liveness and readiness probes MUST be enabled.
- **FR-003**: The system MUST NOT expose `/actuator/env`, `/actuator/heapdump`, `/actuator/metrics`, or any other actuator endpoint publicly in v1. (Future internal Prometheus scrape will be addressed separately.)

**Global error handling**

- **FR-004**: The system MUST register exactly one `@RestControllerAdvice` that handles:
  - `GameRulesException` → 400, `error.code = "GAME_RULES_VIOLATION"`, `error.message` = the exception's message (PT-BR, preserved as-is from `core`).
  - `MethodArgumentNotValidException` and `ConstraintViolationException` → 400, `error.code = "VALIDATION_FAILED"`, `error.fields` = array of `{ field, message }`.
  - `HttpMessageNotReadableException` → 400, `error.code = "MALFORMED_REQUEST"`.
  - `NoHandlerFoundException` → 404, `error.code = "NOT_FOUND"`.
  - `AccessDeniedException` → 403, `error.code = "FORBIDDEN"`. (Auth-related 401s come from Spec 004's filter.)
  - Any other `Exception` → 500, `error.code = "INTERNAL_ERROR"`, generic message (never leak stack traces).
- **FR-005**: Every error response MUST include the correlation ID currently in MDC.
- **FR-006**: Error responses MUST follow a single envelope shape: `{ "error": { "code": string, "message": string, "fields"?: [...], "correlationId": string, "timestamp": ISO-8601 string } }`.

**DTO + mapping**

- **FR-007**: Controllers MUST NOT have method signatures (parameters or return types) that reference any class under `br.com.bnuuy.jwar.core.*` except for plain enums explicitly approved for wire use (none in v1).
- **FR-008**: The system MUST use MapStruct to translate between core types (e.g. `ClassicGamePlayer` at `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/ClassicGamePlayer.java:13-124`) and HTTP DTOs. Each mapper lives in `jwarsv-sboot` and is a Spring bean.
- **FR-009**: An ArchUnit test (or equivalent) MUST verify FR-007 mechanically; build fails on violation.

**OpenAPI**

- **FR-010**: The system MUST publish OpenAPI 3.x JSON at `/v3/api-docs` and a Swagger UI at `/swagger-ui.html` via `springdoc-openapi-starter-webmvc-ui`.
- **FR-011**: Every game-domain DTO (anything that maps from core) MUST carry `@Schema(description = "...")` annotations with PT-BR text matching the terminology in the Manual de Regras / `ClassicGame*` classes.
- **FR-012**: The OpenAPI document MUST be tagged per controller (`Auth`, `Rooms`, `Matches`, `Users`, `System`) and MUST list `Authorization: Bearer <Firebase ID token>` as the security scheme (the actual enforcement comes in Spec 004; the documentation belongs here).

**Validation**

- **FR-013**: Every request DTO MUST use Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Min`, `@Max`, `@Email`, `@Pattern`). Controllers MUST annotate the corresponding parameter with `@Valid`.
- **FR-014**: Validation error messages MUST be in Brazilian Portuguese (override the bundled defaults via `messages_pt_BR.properties`).

**Correlation ID & logging**

- **FR-015**: The system MUST install a servlet filter that runs before any other application filter, reads or generates `X-Correlation-Id`, sets it in `org.slf4j.MDC` under key `correlationId`, sets it on the response header, and clears MDC at the end of the request (including error paths).
- **FR-016**: The Logback pattern MUST include `%X{correlationId}` so every log line during a request is searchable by the same ID.

**CORS**

- **FR-017**: In the `dev` profile, CORS MUST allow origin `http://localhost:5173` (the Vite dev server, per analysis §7.4) and `http://127.0.0.1:5173`, methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers including `Authorization`, `Content-Type`, `X-Correlation-Id`, and credentials true.
- **FR-018**: In the `prod` profile, CORS MUST be same-origin (allow no cross-origin requests). Configurable via `app.cors.allowed-origins` to permit a future split-host deployment without a code change.

**Rate limiting**

- **FR-019**: The system MUST integrate Bucket4j (in-memory storage) and apply rate limits to:
  - `POST /api/auth/register` — 5 / min / IP.
  - `POST /api/auth/login` — 10 / min / IP.
  - Gameplay command endpoints under `/api/matches/{id}/*` (defined in Spec 006) — 30 / min / (IP + UID).
- **FR-020**: When a bucket is exhausted, the response MUST be 429 with `Retry-After` header (seconds until refill) and the standard error envelope (FR-006).

**Wiring fixes**

- **FR-021**: Fix the `scanBasePackages` value at `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java:6` from `"br.com.jwar.server"` to `"br.com.bnuuy.jwar"`. (See analysis §2.3 for the original bug.)
- **FR-022**: Fix the Flyway `locations` value at `jwar-server/jwarsv-sboot/src/main/resources/application.yml:41` from `classpath:db.migration` to `classpath:db/migration`. (See analysis §2.4 / §6.7.)
- **FR-023**: Add `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` to `application.yml` reading from the env vars `JWARSV_APP_DB_HOST`, `JWARSV_APP_DB_PORT`, `JWARSV_APP_DB_NAME`, `JWARSV_APP_DB_USER`, `JWARSV_APP_DB_PW` (per analysis §6.5) with local defaults that work against `others/docker/compose_dev_local.yaml`.
- **FR-024**: Add an `application-test.yml` profile that flips the datasource to H2 in-memory and disables Flyway clean — supports fast slice tests per analysis §8 decision 13.

### Key Entities *(include if feature involves data)*

- **HealthResponse**: `{ status: "UP" | "DOWN", version: string, gitCommit: string, builtAt: ISO-8601 string }`. Pure HTTP DTO; lives in `jwarsv-sboot`.
- **ApiVersionInfo**: build-time metadata produced by the Gradle git-properties plugin; consumed by `HealthResponse`.
- **ErrorResponse**: `{ error: { code: string, message: string, fields?: FieldError[], correlationId: string, timestamp: ISO-8601 string } }`. Single envelope for all error responses.
- **FieldError**: `{ field: string, message: string }`. Used only inside `VALIDATION_FAILED` errors.
- **CorrelationIdFilter**: not a data entity but the contract is: read `X-Correlation-Id` header, validate as UUID, generate if absent, set on MDC under key `correlationId`, echo on response header, clear on completion.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `GET /api/health` responds in under 50ms p95 on a local machine, under 200ms p95 in prod, when the database is reachable.
- **SC-002**: 100% of `/api/**` endpoints documented in code (controllers + DTOs) appear in the generated OpenAPI document. Verified by a test that diffs the OpenAPI JSON against the discovered controllers.
- **SC-003**: 100% of error responses across the application share the single envelope shape (FR-006). Verified by an integration test that triggers every handled exception type.
- **SC-004**: Zero references to `br.com.bnuuy.jwar.core.*` types appear in any `@RestController` method signature. Verified by ArchUnit.
- **SC-005**: A request with no `X-Correlation-Id` and a request with one MUST both result in exactly one `correlationId` field on the response and on every log line — never zero, never two different values. Verified by a controller integration test.
- **SC-006**: The app boots cleanly with `./gradlew bootRun -Dspring.profiles.active=dev` against the local compose stack from `others/docker/compose_dev_local.yaml`. (Today, per analysis §2.4, it does not.)
- **SC-007**: A placeholder Flyway migration at `src/main/resources/db/migration/V1__init.sql` is applied on startup (proves the typo fix in FR-022 works).
- **SC-008**: Rate-limited endpoints emit 429 responses within 5ms of the bucket-exhausted decision and never block the request thread waiting for refill.

## Assumptions

- Spec 004 (auth) will deliver the `OncePerRequestFilter` that verifies Firebase ID tokens and populates the `SecurityContext`. This spec only stages the security-scheme documentation in OpenAPI and the CORS handling that interacts with `Authorization` headers. The actual enforcement is out of scope here.
- Spec 005 (rooms) and Spec 006 (matches/realtime) will deliver the controllers under `/api/rooms/**` and `/api/matches/**`. This spec only sets up the validation/error-handling/OpenAPI plumbing those controllers will inherit.
- The Gradle build is the source of truth for `version` and `gitCommit`. The `gorylenko-gradle-git-properties` plugin (or equivalent Spring Boot `git.properties` generation) will be added in `jwarsv-sboot/build.gradle`.
- Bucket4j in-memory storage is acceptable for v1; horizontal scaling to multiple JVMs is out of scope (matches analysis §7.3 reasoning).
- `spring-boot-starter-security` is added in **Spec 004** (the auth spec), not here. This spec uses plain Servlet filters and `@ControllerAdvice` only.
- The build adds `org.springdoc:springdoc-openapi-starter-webmvc-ui` to `jwar-server/jwarsv-sboot/build.gradle`. Version centralized in `jwar-server/gradle.properties`.
- The Vite UI build is delivered in a separate spec (not numbered here); this spec's CORS configuration is forward-compatible with it.
- The PostgreSQL connection works against the existing init scripts at `jwar-server/others/docker/postgresql/init-database.sh:1-39` (which create `ujwarsvdev` / `jwarsv_db_dev`).
- The Spring Modulith starters at `jwar-server/jwarsv-sboot/build.gradle:42-43` remain on the classpath. Module verification is out of scope for this spec but the new packages introduced (`controller`, `dto`, `mapper`, `config`, `web.error`, `web.filter`) MUST sit under `br.com.bnuuy.jwar.server` so the module boundary work in Spec 005 has a stable shape.
- All new HTTP-layer code lives under `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/web/**`. No code is added to `jwarsv-core` (Constitution Principle I).
