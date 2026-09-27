# jWar — Infrastructure / Auth / REST Current-State Analysis

> **Purpose.** Snapshot the *actual* state of the Spring Boot module
> (`jwarsv-sboot`) and its supporting infra (Docker, nginx, Postgres,
> Firebase) to inform upcoming specs for REST controllers, Firebase
> authentication, Google OAuth, game rooms, realtime gameplay over
> WebSocket, Docker compose, and persistence.
>
> **Scope.** What exists today, what does not, and where the gaps are.
> Every concrete claim is cited as `file:line` against the repo at
> `/var/opt/workspaces/Spring_jWar_game/`.
>
> **Audience.** Spec authors for the next phase of work. This is *not*
> a design document — it is the input to one.
>
> **Date.** 2026-05-15 · **Branch.** `feat/first-version-rules`.

---

## 1. Executive summary

The Spring Boot module is essentially a **greenfield shell**: the
classpath has every dependency the game will eventually need (Spring
Web on Undertow, Spring Data JPA, Flyway 10, Firebase Admin SDK 9.4.1,
Spring Modulith, Stripe, StarkBank, Sentry, Micrometer, OpenFeign,
Quartz), but **almost none of it is wired**. Concretely:

- `jwarsv-sboot/src/main/` contains exactly **2 files**: one
  application class and `application.yml`. Source listing produced by
  `find` confirms there are no other files under that tree.
- The application class
  (`jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java:6`)
  has a **broken `scanBasePackages` value**: it points at
  `br.com.jwar.server`, but the actual package is
  `br.com.bnuuy.jwar.server` (note `bnuuy` is missing). This means
  component scanning would find nothing — but it doesn't matter yet,
  because there are no `@Component`s, `@RestController`s,
  `@Configuration`s, or repositories anywhere in `jwarsv-sboot` (grep
  for `@RestController|@Controller|@RequestMapping|SecurityFilterChain|EnableWebSocket|@MessageMapping`
  returns zero hits in `jwar-server/`).
- `jwarsv-sboot/src/test/` does **not exist**.
- No Flyway migrations exist — there is no `db/migration/` directory
  anywhere under `jwar-server/`.
- No `@Entity`, `@Repository`, `JpaRepository` declaration is present
  in the codebase. `User.java` in `jwarsv-core` is a literal
  three-line empty class.
- The `application.yml` Flyway location is set to
  `classpath:db.migration` (note the **dot**, not slash); Flyway
  expects path-style locations. This is almost certainly a typo.
- The Docker compose stack exists with Postgres + nginx + dev + prod
  images, but the env vars it passes (`JWARSV_APP_DB_HOST`,
  `JWARSV_APP_DB_USER`, `JWARSV_APP_DB_PW`, `JWARSV_APP_FB_KEY`) are
  **not referenced anywhere inside `application.yml`** — the
  datasource section has no `url` / `username` / `password`.
- Firebase Admin SDK is declared as a dependency
  (`jwar-server/jwarsv-sboot/build.gradle:38`), but there is no
  `FirebaseApp.initializeApp(...)` config bean, no
  `firebase-service-account.json` expectation, and no security filter
  to verify ID tokens.

In other words: **the platform is shaped for the right things, but
none of the platform code has been written yet**. This is good — it
means we can spec it cleanly without ripping out half-built
infrastructure.

---

## 2. Inventory: what exists in `jwar-server/jwarsv-sboot/`

### 2.1 Files actually present

Exhaustive listing (verified by `find jwarsv-sboot/src -type f`):

```
jwarsv-sboot/build.gradle
jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java
jwarsv-sboot/src/main/resources/application.yml
```

Three files. That is the entire Spring Boot module. There is no
`controller/`, no `config/`, no `service/`, no `repository/`, no
`security/`, no `dto/`, no `test/`, no `db/migration/`, no `static/`.

### 2.2 `build.gradle` — what's on the classpath

`jwar-server/jwarsv-sboot/build.gradle:16-55` declares the runtime
shape of the Spring Boot module. Notable lines:

- `jwarsv-sboot/build.gradle:17` — depends on `jwarsv-core` (the game
  engine).
- `jwarsv-sboot/build.gradle:25-33` — Spring Boot starters present:
  `actuator`, `data-jpa`, `data-rest`, `mail`, `quartz`, `undertow`,
  `web` (Tomcat excluded inline at `:32`, in addition to the global
  exclusion at `jwar-server/build.gradle:16-20`).
- `jwarsv-sboot/build.gradle:34` — `micrometer-tracing-bridge-brave`.
- `jwarsv-sboot/build.gradle:35` — Sentry starter (Jakarta).
- `jwarsv-sboot/build.gradle:36-37` — Flyway 10.21.0 with the
  Postgres dialect plugin (version from `gradle.properties:35`).
- `jwarsv-sboot/build.gradle:38` — `com.google.firebase:firebase-admin`
  (version 9.4.1 declared at `gradle.properties:34`, with Netty
  exclusion at `jwar-server/build.gradle:66-69`).
- `jwarsv-sboot/build.gradle:39-41` — Spring Cloud OpenFeign with
  `commons-io` excluded to avoid the collision with the
  subproject-wide `commons-io` (`jwar-server/build.gradle:41`).
- `jwarsv-sboot/build.gradle:42-43` — Spring Modulith starters
  (`core` + `jpa`).
- `jwarsv-sboot/build.gradle:47-48` — H2 + PostgreSQL runtimeOnly.
- `jwarsv-sboot/build.gradle:49` — Micrometer Prometheus registry.
- `jwarsv-sboot/build.gradle:50-51` — Modulith actuator +
  observability.
- `jwarsv-sboot/build.gradle:53-54` — `spring-boot-starter-test` and
  `spring-modulith-starter-test`.

**What is *not* declared:**

- No `spring-boot-starter-security` — there is no Spring Security on
  the classpath at all.
- No `spring-boot-starter-oauth2-client` or `oauth2-resource-server`.
- No `spring-boot-starter-websocket` and no STOMP broker.
- No frontend tooling (no `frontend-maven-plugin`, no `node` Gradle
  plugin, no Vite/Webpack).
- The `developmentOnly` `spring-boot-devtools` and
  `spring-boot-docker-compose` are **commented out** at
  `jwarsv-sboot/build.gradle:19-20`.

### 2.3 `JWarBackCoreApplication.java` — entry point

All 13 lines of it
(`jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java:1-13`):

```java
@SpringBootApplication(scanBasePackages = "br.com.jwar.server")
public class JWarBackCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(JWarBackCoreApplication.class, args);
    }
}
```

**Bug:** `scanBasePackages = "br.com.jwar.server"`
(`JWarBackCoreApplication.java:6`). The class itself lives in
`br.com.bnuuy.jwar.server` (line 1). The `bnuuy` segment is dropped
in the scan. Component scanning will currently find **nothing under
the intended package**. This needs to be either fixed to
`br.com.bnuuy.jwar` (covering both core and sboot, which the upcoming
controllers/configs will need) or simply removed (Spring Boot will
default to the application class's own package).

There is no `@EnableJpaRepositories`, no `@EntityScan`, no
`@EnableWebSocket`, no `@EnableScheduling`, no `@Import`. Nothing.

### 2.4 `application.yml` — current configuration

File: `jwar-server/jwarsv-sboot/src/main/resources/application.yml`
(120 lines total).

**Server (`application.yml:1-7`).**
- Port 8080 (`:2`), graceful shutdown (`:3`),
  `error.include-message: always` (`:5`),
  `forward-headers-strategy: framework` (`:7`) — Undertow respects
  `X-Forwarded-*` so nginx termination will work.

**Logging (`application.yml:9-11`).** Root level `INFO`. Nothing
fancy.

**Spring profiles.** There is **no `spring.profiles.active`** in the
file. The default profile name is set via the Gradle property
`springProfile = dev` (`gradle.properties:14`) which `bootRun`
injects as a system property (`jwarsv-sboot/build.gradle:73-75`). So
`./gradlew bootRun` runs as `dev`, but there are no
`application-dev.yml`, `application-prod.yml`, `application-test.yml`
files — only the single base `application.yml`. Compose passes
`SPRING_PROFILES_ACTIVE=dev` / `prod`
(`others/docker/compose.yaml:30,63`), but those profiles have no
matching property files, so no profile-specific overrides apply.

**Datasource (`application.yml:16-22`).**

```yaml
spring:
  datasource:
    driverClassName: org.postgresql.Driver
    hikari:
      maximumPoolSize: 2
      maxLifetime: 180000
      idleTimeout: 50000
```

- **No `url`. No `username`. No `password`.** Compose passes
  `JWARSV_APP_DB_HOST`, `JWARSV_APP_DB_PORT`, `JWARSV_APP_DB_NAME`,
  `JWARSV_APP_DB_USER`, `JWARSV_APP_DB_PW`
  (`others/docker/compose.yaml:33-37` for dev, `:66-70` for prod) but
  **none of those env vars are referenced anywhere inside
  `application.yml`** (confirmed by `grep -n JWARSV application.yml`).
- The pool is tiny (`maximumPoolSize: 2`) — fine for a hobby project,
  may need to be raised once real REST traffic exists.
- `driverClassName: org.postgresql.Driver` forces Postgres even in
  dev. With H2 on the classpath (`build.gradle:47`) Spring would
  otherwise default to H2 in dev. This is *intentional inconsistency*
  with `CLAUDE.md`'s claim of "H2 for dev/test" — currently the app
  will not boot in dev without a real Postgres, because there is no
  URL and the driver class is pinned to Postgres.

**JPA (`application.yml:24-30`).**
- `ddl-auto: none` (`:27`) — schema must come from Flyway. Good
  default; agrees with the constitution's intent.
- `show-sql: true` and `format_sql: true` (`:25, :29-30`) — verbose,
  probably fine in dev, should be off in prod.

**Jackson (`application.yml:32-38`).** Sensible: ignore unknown
properties, write dates as ISO strings.

**Flyway (`application.yml:40-42`).**

```yaml
flyway:
  locations: classpath:db.migration
  enabled: true
```

**Bug:** `db.migration` should be `db/migration` (Flyway resolves the
location as a classpath resource path). With the dot form, Flyway
won't find migrations under `src/main/resources/db/migration/` if
they're ever added. This bug doesn't currently break anything because
there are no migrations to find — but as soon as one is added under
the conventional path, it'll silently be ignored.

**Cache (`application.yml:44-47`).** Caffeine, 500 entries, 120s
TTL. Not yet used anywhere.

**Thymeleaf (`application.yml:49-50`).** HTML mode. Surprising —
nothing in the code consumes Thymeleaf. Likely vestigial or
anticipatory (email templates?).

**Mail (`application.yml:52-65`).** SMTP host
`mail.bnuuywar.com:465`, env-driven password
`JWARSV_APP_EMAIL_PW`. Not used yet, but starter is on the classpath.

**App-level config (`application.yml:67-97`).** Contains
non-Spring-namespaced config under `app.*`:
- `app.backoffice.allowed_users` (`:69`) hardcoded to `fgomes`.
- Migration flags for Firebase user sync (`:71-73`).
- Stripe (`:84-92`) and StarkBank (`:93-97`) credentials. None of
  these is referenced by code yet.

**Actuator (`application.yml:102-114`).** Web exposure limited to
`health,info` (`:107`); health details always shown (`:110-111`);
JMX disabled (`:113-114`). Cloud config health check disabled
(`:118-120`).

### 2.5 What's *not* in `jwarsv-sboot/`

| Concern                       | Status               | Evidence                                                    |
|-------------------------------|----------------------|-------------------------------------------------------------|
| REST controllers              | None                 | grep for `@RestController` returns zero hits                |
| Spring Security config        | None                 | grep for `SecurityFilterChain` / `@EnableWebSecurity` empty |
| Firebase initialization bean  | None                 | only the dependency at `build.gradle:38`                    |
| WebSocket / STOMP config      | None                 | grep for `@EnableWebSocket` / `@MessageMapping` empty       |
| JPA entities                  | None                 | grep for `@Entity` empty in `jwar-server/`                  |
| JPA repositories              | None                 | grep for `JpaRepository` / `@Repository` empty              |
| Flyway migrations             | None                 | no `db/migration/` directory anywhere                       |
| Static assets (UI bundle)     | None                 | no `static/` or `public/` under `resources/`                |
| Test sources                  | None                 | `jwarsv-sboot/src/test/` does not exist                     |
| `@ConfigurationProperties`    | None                 | annotation processor is present (`build.gradle:23`) but unused |
| Profile-specific YAMLs        | None                 | only the base `application.yml`                             |

---

## 3. Inventory: existing test infrastructure under `jwarsv-sboot/`

**None.** `jwarsv-sboot/src/test/` does not exist on disk.

There is no example of `@SpringBootTest`, `@DataJpaTest`,
`@WebMvcTest`, `@TestRestTemplate`, or Testcontainers anywhere in
the repository.

The only tests in the project are pure-JUnit-5 tests living in
`jwarsv-core/src/test/java/.../core/game/`:

- `ClassicGameTest.java`
- `ClassicGameDistSeqColorTest.java`
- `ClassicGameDistObjectivesTest.java`
- `ClassicGameDistCountriesTest.java`
- `utils/ExchangeCardsEvaluatorTest.java`

These are unit tests over the engine. They have no Spring context.
The `spring-modulith-starter-test` dependency at
`jwarsv-sboot/build.gradle:54` is declared but not yet exercised.

**Implication for upcoming specs.** Every new layer (REST,
WebSocket, JPA, security) starts from zero test coverage in
`jwarsv-sboot`. The specs should explicitly mandate slice tests
(`@WebMvcTest` for controllers, `@DataJpaTest` for repositories, a
single `@SpringBootTest` smoke test for the application context, and
an integration test for the Firebase token-verification filter using
a stubbed `FirebaseAuth`).

---

## 4. The `User` domain — what's there

File: `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java`

Entire contents (4 lines):

```java
package br.com.bnuuy.jwar.core.domain;

public class User {
}
```

- **Not** a JPA entity.
- No fields, no methods, no annotations.
- No `UserRepository` exists anywhere.
- Not referenced by any other class. (`grep -r "import.*core.domain.User"` confirms zero usages.)
- The constitution's Principle I (game logic isolation) means that
  even when a real `User` JPA entity is needed, it should probably
  *not* live in `jwarsv-core` — it should live in `jwarsv-sboot`
  with the rest of the persistence layer. The current `User.java` in
  `jwarsv-core` is **dead code** and should be removed (or moved to
  `jwarsv-sboot`) as part of the persistence spec.

For contrast, the actual "player in a match" object —
`ClassicGamePlayer`
(`jwarsv-core/.../core/game/domain/ClassicGamePlayer.java:13-124`) —
is a plain Java class that carries a `String userId` (`:15`) and
`String nickName` (`:16`). It has no JPA mapping and is created
in-memory by the lobby flow (`ClassicGameLobby.joinLobby`,
`jwarsv-core/.../core/game/ClassicGameLobby.java:23-28`). The
`userId` string is the future foreign key to the (yet-to-be-built)
persistent `User` entity, presumably the Firebase UID.

---

## 5. Docker / Compose / nginx — what's there

The `others/` tree under `jwar-server/` contains the deployment
artifacts.

### 5.1 Dockerfile (`jwar-server/Dockerfile:1-15`)

- Base image `openjdk:17-jdk-slim` (`:1`).
- Takes the pre-built `jwarsv-sboot-${VERSION}.jar` (`:8`) — so the
  Gradle build must run before docker build.
- Exposes 8080 (HTTP) and 8081 (`:11-12`). 8081 is the debug
  port in dev (compose maps it at `compose.yaml:50`).
- Hardcoded JDWP agent on `*:5083`, timezone `-03:00`
  (`:14`). The JDWP port is exposed via the agent string but not via
  `EXPOSE`, which would need fixing for remote debugging.
- No multi-stage build; no GraalVM native build path (despite
  `bootBuildImage` being available per `gradle.properties:17`).

### 5.2 Compose stacks

There are two:

1. **`others/docker/compose.yaml`** (the "full" stack) —
   `compose.yaml:1-140`. Defines:
   - `postgres-jwarsv-db-prod` on `15432:5432`
     (`compose.yaml:3-19`).
   - `jwarsv-backend-app-dev` (port 8081, env profile `dev`,
     `compose.yaml:21-52`).
   - `jwarsv-nginx-app-prod` (misnamed — it's actually a Spring Boot
     image, see `:54-86`) on port 8080.
   - `jwarsv-nginx-server` (real nginx, ports 80 + 443,
     `compose.yaml:88-103`).
   - Bind-mounted volumes to `/var/postgresql/jwarsv-data` and
     `/var/logs/jwarsv-*` (`:104-134`) — these paths only exist on
     Fernando's deploy host, not in dev environments.
2. **`others/docker/compose_dev_local.yaml`** (`compose_dev_local.yaml:1-39`).
   Only Postgres + nginx, no Spring Boot app — expectation is the
   app runs on the host JVM and nginx proxies to
   `host.docker.internal:8081` (commented placeholder at
   `nginx/default.conf:48`).

### 5.3 Postgres init scripts

`others/docker/postgresql/init-database.sh:1-39` creates two users
(`ujwarsvdev`, `ujwarsvprod`) and two databases (`jwarsv_db_dev`,
`jwarsv_db_prod`) with schema ownership transferred to the
respective app user. Same shape, hardcoded password version exists
at `init-database_local.sh:1-39`.

### 5.4 nginx config

`others/docker/nginx/default.conf:1-137` defines two virtual hosts
(`qa-api.bnuuywar.com` → dev backend, `api.bnuuywar.com` → prod
backend), TLS via mounted Cloudflare-signed certs (`:12-13, :72-73`),
HTTP-to-HTTPS redirect (`:131-136`), gzip enabled, static asset
caching, and `location /api/` → `proxy_pass` to the upstream
(`:47-50, :108-111`).

**Critical for WebSocket spec:** the current nginx config has **no
`Upgrade` / `Connection` headers** for `/api/`, so WebSocket
connections through nginx would be rejected. This must be fixed
when the realtime layer ships.

### 5.5 Compose-passed env vars vs. application.yml

The compose file
(`others/docker/compose.yaml:29-48` for dev; `:62-82` for prod)
exports these variables that `application.yml` either references or
should reference:

| Env var                               | Used in application.yml?                | Should be used? |
|---------------------------------------|------------------------------------------|-----------------|
| `SPRING_PROFILES_ACTIVE`              | implicit (no per-profile yml exists)     | yes             |
| `JWARSV_APP_DB_HOST`                  | **no**                                   | **yes**         |
| `JWARSV_APP_DB_PORT`                  | **no**                                   | **yes**         |
| `JWARSV_APP_DB_NAME`                  | **no**                                   | **yes**         |
| `JWARSV_APP_DB_USER`                  | **no**                                   | **yes**         |
| `JWARSV_APP_DB_PW`                    | **no**                                   | **yes**         |
| `JWARSV_APP_FB_KEY`                   | **no**                                   | **yes** (Firebase service account path or inline JSON) |
| `JWARSV_APP_STARK_BANK_ENV`           | yes (`application.yml:94`)               | yes             |
| `JWARSV_APP_STARK_PROJECT_ID`         | yes (`:95`)                              | yes             |
| `JWARSV_APP_STARK_BANK_CENTER_ID`     | yes (`:96`)                              | yes             |
| `JWARSV_APP_STARK_BANK_KEY`           | yes (`:97`)                              | yes             |
| `JWARSV_APP_STRIPE_*`                 | yes (`:84-92`)                           | yes             |
| `JWARSV_APP_RETRIEVE_FB_USER_WHEN_CONTACTS` | yes (`:72`)                        | yes             |
| `JWARSV_APP_FB_FORCE_USER_SYNC_IN_CREATION` | yes (`:73`)                        | yes             |
| `JWARSV_APP_BACKOFFICE_MASTER_PW`     | yes (`:70`)                              | yes             |

The DB and FB-key env vars are **the most important gap**: compose
will set them, but the Spring app currently ignores them.

---

## 6. Infrastructure deltas — what needs to be added

This section answers "what is missing to take the project from
'engine works' to 'multiplayer Risk game served over HTTP and
WebSocket with Firebase auth, Postgres persistence, and a UI'?"

### 6.1 REST controllers (none today; all needed)

The game engine's player-facing API is already enumerated in
`ClassicGamePActions.java:24-123`. The REST layer must surface:

- **Auth / session.** `POST /api/auth/login` (exchange Firebase ID
  token for an app session), `POST /api/auth/logout`,
  `GET /api/auth/me` (current user). Possibly an OAuth callback if
  Spring's OAuth2 client is used; otherwise Firebase handles the
  full flow client-side and the server only verifies tokens.
- **Users.** `GET /api/users/me` (profile),
  `PATCH /api/users/me` (nickname, avatar), and possibly
  `GET /api/users/{id}` if public profiles are a thing.
- **Game rooms.** `POST /api/rooms` (create), `GET /api/rooms`
  (list public rooms), `GET /api/rooms/{id}` (room state),
  `POST /api/rooms/{id}/join` (join), `POST /api/rooms/{id}/leave`,
  `POST /api/rooms/{id}/start` (host-only, calls
  `ClassicGameLobby.startMatch`, `ClassicGameLobby.java:30-32`).
- **Gameplay.** Wrappers over `ClassicGamePActions`:
  `POST /api/matches/{matchId}/add-troops`,
  `POST /api/matches/{matchId}/add-continent-troops`,
  `POST /api/matches/{matchId}/exchange-cards`,
  `POST /api/matches/{matchId}/end-add-phase`,
  `POST /api/matches/{matchId}/attack`,
  `POST /api/matches/{matchId}/end-attack-phase`,
  `POST /api/matches/{matchId}/move-troops` (engine doesn't have
  this method yet — see Action dispatcher gap in
  `ClassicGamePActions`),
  `POST /api/matches/{matchId}/end-turn`,
  `GET /api/matches/{matchId}` (full state snapshot for late
  joiners / reconnects).

None of these endpoints exists today.

### 6.2 Realtime push (WebSocket / SSE / polling)

The engine already anticipates push notification: there are TODO
comments at `ClassicGamePActions.java:79, :93, :120` ("`TODO SPRINT2
- COMNS - send update to other players …`"), the lobby has the
comment "when joining the lob, will register some id/connection to
callback for updates" (`ClassicGameLobby.java:27`), and
`ClassicGame.startMatch` ends with `//send update to all players …
//let all players know its first player turn`
(`ClassicGame.java:139-140`).

There is **no transport** for any of these notifications today —
nothing on the websocket side, nothing on the SSE side, no polling
endpoint. The classpath has neither `spring-boot-starter-websocket`
nor any SSE-specific support (Spring MVC SSE works without an extra
starter, but no `SseEmitter` usage exists).

**Undertow native WebSocket.** Undertow is the embedded server
(`build.gradle:30`). Undertow has first-class WebSocket support and
performs well; Spring's `spring-boot-starter-websocket` builds on
top of it transparently. There is no JBoss-specific configuration
needed.

### 6.3 Firebase Auth — not wired

Present:
- Dependency at `jwar-server/jwarsv-sboot/build.gradle:38`.
- Version pinned at `gradle.properties:34` (9.4.1).
- Netty exclusion at `jwar-server/build.gradle:66-69`.
- Env var `JWARSV_APP_FB_KEY` passed by compose
  (`compose.yaml:38, :71`).
- App-level config flags at `application.yml:71-73`
  (`retrieve_fb_user_when_contacts`,
  `force_user_sync_in_creation`).

Missing:
- No `FirebaseApp.initializeApp(...)` `@Configuration`.
- No `firebase-service-account.json` referenced anywhere (no grep
  hit on `service-account` or `GOOGLE_APPLICATION_CREDENTIALS`). The
  contract for `JWARSV_APP_FB_KEY` is unclear — is it a path to a
  mounted JSON file, an inline base64-encoded JSON, or the entire
  JSON document escaped? **Spec needs to define this explicitly.**
- No `OncePerRequestFilter` that calls
  `FirebaseAuth.verifyIdToken(token)` and populates the
  `SecurityContext`.
- No `@RestControllerAdvice` that translates Firebase auth
  exceptions into 401 JSON responses.

### 6.4 Google OAuth

There is no `spring-security-oauth2-client` on the classpath. The
two viable paths are:

1. **Firebase handles Google OAuth.** The browser uses Firebase
   Auth's `GoogleAuthProvider` (or `signInWithPopup`) to log in. The
   client gets a Firebase ID token, sends it with each REST call in
   `Authorization: Bearer <id-token>`, and the server verifies it
   via `FirebaseAuth.verifyIdToken`. **No Spring Security OAuth2
   client needed.**
2. **Spring Security OAuth2 client.** The server handles the full
   redirect dance with Google directly. This duplicates
   functionality Firebase already provides and would require running
   *two* identity systems (Firebase for email/password and
   anonymous, Spring for Google) — unless we drop Firebase entirely.

The Firebase Admin SDK is already a project dependency and
`gradle.properties` documents it as the chosen AuthN
(`gradle.properties:34`). Path **(1)** is the natural choice.

### 6.5 Postgres connection

Current state: `driverClassName: org.postgresql.Driver`
(`application.yml:17`), no `url` / `username` / `password`. App
will not boot connecting to Postgres in either compose stack.

What's needed in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${JWARSV_APP_DB_HOST:localhost}:${JWARSV_APP_DB_PORT:15432}/${JWARSV_APP_DB_NAME:jwarsv_db_dev}
    username: ${JWARSV_APP_DB_USER:ujwarsvdev}
    password: ${JWARSV_APP_DB_PW:jwarsvdev@123}
```

…plus a profile-specific `application-test.yml` that flips to H2
in-memory for fast slice tests (the H2 driver is already
runtimeOnly at `build.gradle:47`).

### 6.6 CORS

There is no CORS config in the codebase. With the planned split
between a Vite dev server (likely `http://localhost:5173`) and the
Spring backend on `http://localhost:8080`, dev-mode CORS is
required. Production CORS depends on whether nginx serves the UI
from the same origin (then `Same-Origin` is fine) or a different
origin (then explicit allow-list needed).

### 6.7 Flyway migrations

None exist. The `db.migration` typo at `application.yml:41` means
that even if migrations are added under the conventional
`src/main/resources/db/migration/`, Flyway won't find them. **The
yml must be changed to `classpath:db/migration` or
`classpath:/db/migration`.**

### 6.8 Static asset serving

There is no `src/main/resources/static/` or `public/` today. For a
single-deployable artifact (Spring Boot serves the React UI bundle),
the build needs to either:

- Copy the Vite `dist/` into
  `jwarsv-sboot/src/main/resources/static/` at build time, or
- Use a separate Gradle subproject (e.g. `jwarsv-ui`) that produces
  a jar with the static resources and is depended on by
  `jwarsv-sboot`.

There is no frontend tooling (no `node`, `npm`, or
`frontend-maven-plugin`) configured anywhere yet.

---

## 7. Tech recommendations (opinionated, anchored in YAGNI)

### 7.1 Auth: **pure Firebase ID-token verification in a Spring filter**

**Recommendation:** Drop in `spring-boot-starter-security` (only for
the filter chain plumbing — we won't use form login, OAuth2 client,
or any of its higher-level features), write one
`OncePerRequestFilter` that reads `Authorization: Bearer <token>`
and calls
`FirebaseAuth.getInstance().verifyIdToken(token)`, set an
`Authentication` containing the Firebase UID + custom claims, and
make `/api/**` require authentication while leaving `/actuator/health`
and the static asset paths open.

**Rationale.**
- The dependency is already on the classpath and pinned
  (`build.gradle:38`, `gradle.properties:34`).
- The client (browser) does all the heavy lifting: email/password,
  Google, GitHub, anonymous sessions, password reset, email
  verification. None of that is server code we have to maintain.
- Spring Security OAuth2 client would duplicate Firebase. YAGNI
  (Principle V).
- The Spring app stays stateless — no session storage, no CSRF
  worries, just bearer tokens on each request. Plays well with
  WebSocket (handshake carries the token in a query param or
  custom subprotocol, server verifies once, attaches UID to the
  WebSocket session).
- `JWARSV_APP_FB_KEY` becomes the path to (or content of) the
  service-account JSON.

### 7.2 Realtime: **STOMP over WebSocket**

**Recommendation:** `spring-boot-starter-websocket` + STOMP, with
Spring's in-memory broker.

Topology:
- `/ws` is the WebSocket endpoint (handshake authenticates via
  Firebase ID token in a query parameter — turn it into an `Authentication` via a `HandshakeInterceptor`).
- `/topic/rooms/{roomId}` — room-level updates (player joined,
  player left, host started match).
- `/topic/matches/{matchId}` — match-level state updates (attack
  result, turn changed, troops added, cards exchanged, game ended).
- `/user/queue/private` — per-user private messages (your
  objective card, your hand, error responses).

**Rationale.**
- Bi-directional needs are real: client must send actions and
  receive events. SSE is one-way (server → client); we'd still need
  a second channel for client actions (which exists as REST, but
  doubling the channels complicates state). One channel beats two.
- STOMP frames are typed (`MESSAGE`, `SUBSCRIBE`, `ACK`), trivially
  routable, and have great Spring integration
  (`@MessageMapping`, `SimpMessagingTemplate`,
  `convertAndSendToUser`).
- The in-memory broker is sufficient: one game match lives on one
  JVM (the engine state is already in-memory — see
  `ClassicGame.players`, `:33`). We don't need a Redis-backed
  external broker until we horizontally scale, which is way past
  YAGNI today.
- Raw WebSocket would force us to hand-roll a frame protocol; not
  worth it.
- SSE is great for one-way feeds but breaks the symmetry of
  "client sends action → server broadcasts result."

### 7.3 Persistence: **hybrid — in-memory match state, Postgres for everything else**

**Recommendation:**
- **Postgres-persisted:** `users` (Firebase UID, nickname, avatar,
  created_at, last_login), `rooms` (id, host_user_id, status,
  created_at, max_players, is_public), `room_memberships`
  (room_id, user_id, joined_at), `matches` (id, room_id,
  started_at, ended_at, winner_user_id), `match_participants`
  (match_id, user_id, color, final_status). Just enough to support
  lobby listing, reconnect-to-game, history, leaderboards.
- **In-memory:** the live `ClassicGame` object per active match, in
  a `ConcurrentHashMap<UUID, ClassicGame>` keyed by `matchId`
  (which already exists at `ClassicGame.matchId`,
  `ClassicGame.java:59`). Match state is *not* persisted between
  rounds — if the JVM restarts, in-flight matches are lost.

**Rationale.**
- Principle V (YAGNI). Serializing every troop addition into
  Postgres is a massive engineering tax for a feature
  (server-restart survival) we don't need on day one.
- The enum-driven board design (`AGENTS.md:117` calls out
  "Enum-driven type system. Game state is in-memory per match") was
  built with this assumption.
- Adding persistence later is straightforward: add an
  `@EventListener` on Spring application events
  ("`MatchStateChanged`") that snapshots the current `ClassicGame`
  into Postgres. We can defer that until we actually see a
  restart-loss problem.
- Persisting users/rooms is *not* deferrable — we need them to find
  matches, prevent duplicate joins, and show "your past games."

Migration plan:
- V1: `users`, `rooms`, `room_memberships`.
- V2: `matches`, `match_participants`. (Only metadata, no board
  state.)

### 7.4 UI build/deploy: **separate Vite dev + copy `dist/` into `static/` for prod**

**Recommendation:**
- A sibling top-level directory `jwar-ui/` (Vite + React).
- Dev: Vite dev server on `:5173` proxies `/api` and `/ws` to
  Spring on `:8080` via Vite's `server.proxy`. Spring has CORS
  config that only matters in dev.
- Prod: a Gradle task in `jwarsv-sboot/build.gradle` shells out to
  `npm run build` in `../../jwar-ui` and copies the resulting
  `dist/` into `src/main/resources/static/` before the
  `bootJar` task. Single fat jar artifact. nginx is then optional
  — Undertow can serve the bundle directly. nginx remains useful
  for TLS termination and HTTP-to-HTTPS redirect (which the
  existing `default.conf` already does at `:131-136`).

**Rationale.**
- `frontend-maven-plugin`-style integration would force a Node
  install inside the Gradle build. That's fine for CI but slow
  locally and adds a dependency that doesn't pay for itself for a
  small project. Direct `npm run build` invocation is simpler.
- Keeping the UI as a separate Gradle subproject (option 3) is
  over-engineered for a single-page React app. Principle V.
- Same-origin in prod eliminates a class of CORS bugs.

---

## 8. Open decisions — with chosen defaults

These are points where reasonable engineers could disagree. Per the
operating context (autonomous sleep-time run, no clarifying
questions allowed), each has a chosen default. The downstream specs
should restate these decisions explicitly so they can be revisited.

1. **Q: Should `User` be a JPA entity in `jwarsv-core` or in
   `jwarsv-sboot`?**
   **Decision:** `jwarsv-sboot`.
   **Rationale:** Constitution Principle I — `jwarsv-core` must
   stay free of Spring Web *and* persistence framework coupling.
   The existing empty `User.java` in `jwarsv-core` should be deleted.

2. **Q: Does the `JWARSV_APP_FB_KEY` env var hold a file path, a
   raw JSON document, or a base64-encoded JSON document?**
   **Decision:** File path. Mount the JSON file into the container
   at `/etc/jwar/firebase-service-account.json` and set
   `JWARSV_APP_FB_KEY=/etc/jwar/firebase-service-account.json`.
   **Rationale:** Mirrors Google's
   `GOOGLE_APPLICATION_CREDENTIALS` convention; keeps the secret
   out of `docker inspect` output and environment dumps; allows
   the file to be permission-locked (`chmod 400`).

3. **Q: Where does Firebase return after Google OAuth — does the
   backend ever participate in the redirect dance?**
   **Decision:** No. Firebase JS SDK handles the entire OAuth flow
   in the browser. The backend only ever sees the resulting
   Firebase ID token in `Authorization: Bearer`.

4. **Q: Should rooms be discoverable (public listing) or
   invite-only?**
   **Decision:** Both, with a `is_public` boolean on the `rooms`
   table. Default to public for the first cut.

5. **Q: What's the room → match transition? Are they 1:1 (each
   room hosts exactly one match then dies) or 1:many (a room is a
   persistent "table" you can play multiple matches in)?**
   **Decision:** 1:1 for v1. The room is created, fills up, host
   starts the match, when the match ends the room is closed.
   **Rationale:** Mirrors the engine's current model
   (`ClassicGameLobby` builds *one* `ClassicGame`,
   `ClassicGameLobby.java:18-32`). Multi-match rooms are a v2
   feature.

6. **Q: When a player disconnects mid-match, what happens to their
   turn?**
   **Decision:** Their turn auto-ends after a configurable timeout
   (default 90 seconds) and the engine advances. The player can
   reconnect and resume controlling their pieces.
   **Rationale:** Avoids matches stalling forever. Implementation
   detail for the gameplay spec, not the infra spec.

7. **Q: Is match state ever persisted (so a server restart
   survives in-flight games)?**
   **Decision:** No, not in v1. In-memory only. (See §7.3.)

8. **Q: Are spectators a thing?**
   **Decision:** No, not in v1. Joining a match requires being
   one of the 3-6 seats at that table.

9. **Q: How are matches authorized — can any authenticated user
   call any `/api/matches/{matchId}` endpoint, or only the
   participants?**
   **Decision:** Only the participants. Enforced in a
   `@PreAuthorize` (or hand-rolled check) inside the controller
   that consults the in-memory match registry.

10. **Q: STOMP topic naming — `/topic/rooms/{roomId}` vs.
    `/rooms/{roomId}` vs. `/v1/rooms/{roomId}`?**
    **Decision:** `/topic/rooms/{roomId}` and
    `/topic/matches/{matchId}` (Spring convention, broker prefix
    `/topic`). Private messages via `/user/queue/private`.

11. **Q: What does the WebSocket handshake authenticate against —
    a query parameter, a cookie, or a custom subprotocol?**
    **Decision:** Query parameter `?token=<firebase-id-token>`,
    verified once in the `HandshakeInterceptor`, then the
    `Principal` is attached to the WebSocket session.
    **Rationale:** Browsers can't set custom headers on a
    WebSocket handshake. Cookies would re-introduce CSRF concerns.
    Query parameters in a TLS-terminated connection are fine for
    this risk profile.

12. **Q: Test database — Postgres in a Testcontainer or H2 in
    memory?**
    **Decision:** H2 in memory for slice tests
    (`@DataJpaTest`), Testcontainers Postgres for the single
    full-context `@SpringBootTest`.
    **Rationale:** Speed for the common case, fidelity for the
    integration smoke test. Both are zero-config dev experiences.

13. **Q: Do we need Spring profiles `dev`, `prod`, `test` as
    separate yml files, or just env-var-driven config?**
    **Decision:** Add `application-test.yml` (H2 + Flyway clean +
    debug logging) and let `dev`/`prod` differ only in env vars.
    No `application-dev.yml` or `application-prod.yml`.
    **Rationale:** Minimizes the surface area of "what changes
    between environments" to "credentials and hostnames." YAGNI.

14. **Q: Does nginx terminate TLS and forward to plain HTTP
    Undertow, or does Undertow do TLS itself?**
    **Decision:** nginx terminates TLS (the existing
    `default.conf:11-13, :72-73` already does this with the
    Cloudflare-signed cert). Undertow listens on plain
    HTTP/HTTP-2 internally. `forward-headers-strategy: framework`
    is already set (`application.yml:7`) so Spring sees the
    correct `X-Forwarded-Proto`.

15. **Q: Should the existing typo bugs
    (`scanBasePackages = "br.com.jwar.server"` and Flyway
    `classpath:db.migration`) be fixed inside one of the upcoming
    specs, or as a standalone pre-spec cleanup?**
    **Decision:** Roll them into the first infra spec (the one
    that wires the datasource and adds the Firebase config bean).
    No standalone "fix typos" PR.

---

## 9. Summary — what each upcoming spec needs to deliver

| Spec | Must add | Must touch (existing) |
|---|---|---|
| **REST controllers** | `controller/` package; DTOs; `@RestControllerAdvice` for `GameRulesException`→4xx | `JWarBackCoreApplication.java:6` (fix `scanBasePackages`) |
| **Firebase auth** | `FirebaseConfig` bean; `FirebaseAuthFilter`; `SecurityConfig` | `application.yml` (add `app.firebase.service-account-path`) |
| **Google OAuth** | Nothing on the backend (Firebase handles it client-side) | — |
| **Game rooms** | `Room`, `RoomMembership` JPA entities; repositories; in-memory `MatchRegistry`; controllers | — |
| **Realtime (WebSocket)** | `spring-boot-starter-websocket` dep; `WebSocketConfig`; `HandshakeInterceptor`; engine hooks where TODOs are (`ClassicGamePActions.java:79, :93, :120`; `ClassicGame.java:139-140`) | nginx `default.conf` (Upgrade/Connection headers for `/ws`) |
| **Docker/compose** | Healthchecks; remove the bind-mount paths (`/var/postgresql/jwarsv-data`, `/var/logs/jwarsv-*`) for portability; document compose-up workflow | `Dockerfile:14` (consider multi-stage); `compose.yaml:104-134` (named volumes) |
| **Persistence** | Migrations V1 + V2; `User`/`Room`/`Match` entities in `jwarsv-sboot`; datasource URL/user/pw bound from env | `application.yml:17, :41` (datasource URL, Flyway path typo); delete `jwarsv-core/.../core/domain/User.java` |

Each of these specs is sized to be a clean, independently
mergeable feature. The infra spec (datasource + Firebase bean +
typo fixes) should ship first — everything else depends on it.

---

**End of analysis.**
