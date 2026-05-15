# Feature Specification: Authentication & User Accounts (Firebase + Google OAuth)

**Feature Branch**: `004-auth-and-users`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Wire Firebase Authentication into `jwarsv-sboot`: verify Firebase ID tokens in a Spring filter, bootstrap a Postgres `User` row on first verified token, support email/password sign-up server-side, support Google sign-in by trusting the client-completed Firebase OAuth flow, expose `GET/PATCH/DELETE /api/me`, and ship the Firebase service-account loading contract."

This spec depends on **Spec 003** (REST foundation, error envelope, correlation IDs, validation). It is the prerequisite for **Spec 005** (rooms — every room operation needs an authenticated user) and **Spec 006** (realtime — the WebSocket handshake authenticates with the same ID token).

Background and decisions are anchored in `docs/analysis/03-infra-auth-rest-state.md`:
- §6.3 — Firebase Admin SDK is on the classpath (`jwar-server/jwarsv-sboot/build.gradle:38`, version pinned at `gradle.properties:34`) but no `FirebaseApp.initializeApp(...)` configuration, no token-verification filter, no service-account loading contract exists today.
- §7.1 — chosen approach: pure Firebase ID-token verification in a Spring filter, plus `spring-boot-starter-security` purely for filter-chain plumbing (no form login, no OAuth2 client).
- §8 decision 2 — `JWARSV_APP_FB_KEY` is a **file path**, mirroring `GOOGLE_APPLICATION_CREDENTIALS`.
- §8 decision 3 — Google OAuth is handled entirely client-side by Firebase JS SDK; the server only ever sees a Firebase ID token in `Authorization: Bearer`.

Constitution alignment:
- **Principle I** — `jwarsv-core` MUST NOT gain any Spring Security or `firebase-admin` import. The existing empty `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java` (4 lines, see analysis §4) MUST be deleted as part of this spec; the new persistent `User` entity lives in `jwarsv-sboot`.
- **Principle IV** — sboot depends on core; the auth filter calls into `jwarsv-core` only via existing public APIs (none required for v1).
- **Principle V** — no Spring Security OAuth2 client, no custom session store, no JWT signing of our own; let Firebase do everything it already does well.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticate every protected request (Priority: P1)

As the platform owner, I need every request to `/api/**` (except an explicit allow-list) to require a valid Firebase ID token in the `Authorization: Bearer` header — so that game rooms, gameplay actions, and user profile data cannot be accessed anonymously.

**Why this priority**: P1 because every other spec from here on assumes "the request has an authenticated user." Without this filter, Spec 005's room ownership checks and Spec 006's match-participant authorization checks have nothing to read.

**Independent Test**: Boot the app with a valid `firebase-service-account.json`, call any protected endpoint without a header → 401; with an invalid token → 401; with a valid token → reaches the controller and the `SecurityContext` carries the Firebase UID.

**Acceptance Scenarios**:

1. **Given** the app is running, **When** a client calls a protected endpoint (e.g. `GET /api/me`) without an `Authorization` header, **Then** the response is 401 with `{ "error": { "code": "UNAUTHENTICATED", "message": "Token de autenticação ausente", "correlationId": "..." } }` (envelope from Spec 003 FR-006).
2. **Given** a client sends `Authorization: Bearer xxx-invalid-xxx`, **When** the filter calls `FirebaseAuth.verifyIdToken(...)`, **Then** the verification throws and the filter returns 401 with `error.code = "INVALID_TOKEN"`.
3. **Given** a client sends a valid Firebase ID token, **When** the filter verifies it, **Then** the `SecurityContext` is populated with a `FirebaseAuthentication` (or equivalent) object carrying `firebaseUid`, `email`, and `provider`, and the controller executes.
4. **Given** a client sends a valid token whose `exp` is in the past, **When** the filter calls `verifyIdToken`, **Then** the response is 401 with `error.code = "TOKEN_EXPIRED"`.
5. **Given** a client calls a public endpoint (`/api/health`, `/api/auth/register`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`), **When** no token is present, **Then** the request succeeds.

---

### User Story 2 - First-login bootstrap and "who am I" (Priority: P1)

As an authenticated user logging in for the first time, I need the server to silently create a `User` row keyed by my Firebase UID and let me read my profile via `GET /api/me` — so that downstream features (rooms, matches, history) have a stable internal user identity to foreign-key against.

**Why this priority**: P1 because every room and match in Spec 005 references a `User.id` (internal UUID). The bootstrap must be idempotent and happen on the very first verified request, with no extra client-side ceremony.

**Independent Test**: Sign up via Firebase Web SDK as a brand-new user; immediately call `GET /api/me`; verify (a) the response includes the profile, (b) the `users` table now has exactly one row with that Firebase UID, (c) a second call to `GET /api/me` produces no additional row.

**Acceptance Scenarios**:

1. **Given** the user has just authenticated for the first time, **When** they call `GET /api/me`, **Then** the server upserts a row in `users` and responds 200 with `{ id, firebaseUid, email, displayName, photoUrl, provider, createdAt, updatedAt }`.
2. **Given** an existing user, **When** they call `GET /api/me` again, **Then** the server returns the existing row without inserting another (`createdAt` unchanged; `updatedAt` may bump if the Firebase profile changed).
3. **Given** two parallel first-time requests from the same user (race), **When** both hit the bootstrap path, **Then** at most one row is created and both responses see the same `User.id`. Enforced by a `UNIQUE` constraint on `firebase_uid`.
4. **Given** the Firebase token's `email_verified` claim is `false`, **When** bootstrap runs, **Then** the row is still created (we trust Firebase's identity, but we record `emailVerified = false` so downstream specs can gate features on it).

---

### User Story 3 - Email/password registration (Priority: P1)

As a new user without a Google account, I need a server-side endpoint that creates my Firebase account from an email + password, bootstraps the local `User` row, and returns a token I can immediately use to call the API — so that I do not need to integrate the Firebase JS SDK's `createUserWithEmailAndPassword` from the client and can stay on a simple HTML form during the early phases of the UI.

**Why this priority**: P1 because the dev experience for email/password sign-up via Firebase Web SDK is acceptable but unfriendly to backend-driven flows (e.g., the future admin console at `app.backoffice.allowed_users`, see `application.yml:69`). Having a server-side endpoint also lets the UI start with a plain form and add Firebase SDK later.

**Independent Test**: `POST /api/auth/register` with a valid email + 12-char password; verify (a) a Firebase user exists with that email, (b) a `users` row exists, (c) the response contains a `customToken` the UI can exchange for an ID token via `signInWithCustomToken`.

**Acceptance Scenarios**:

1. **Given** a unique email and a strong password, **When** the client POSTs `/api/auth/register` with `{ email, password, displayName }`, **Then** the server calls `FirebaseAuth.createUser(...)`, creates the local `User` row, mints a Firebase custom token via `FirebaseAuth.createCustomToken(uid)`, and responds 201 with `{ user: {...}, customToken: "..." }`.
2. **Given** the email is already in use in Firebase, **When** registration is attempted, **Then** the response is 409 with `error.code = "EMAIL_ALREADY_IN_USE"`.
3. **Given** an invalid email format, **When** registration is attempted, **Then** Bean Validation (per Spec 003 FR-013) rejects it before any Firebase call with `VALIDATION_FAILED`.
4. **Given** a weak password (less than 8 characters, or fails strength rules — see assumptions), **When** registration is attempted, **Then** the response is 400 with `error.code = "WEAK_PASSWORD"` and details about which rule failed.
5. **Given** the registration succeeds, **When** the client immediately calls `signInWithCustomToken(customToken)` on the Firebase Web SDK and uses the resulting ID token, **Then** subsequent calls to `/api/me` succeed.

---

### User Story 4 - Google sign-in via Firebase, no server-side OAuth (Priority: P2)

As a user who prefers Google sign-in, I need to authenticate with Google through Firebase's client-side popup and have the server treat the resulting Firebase ID token exactly like any other (no separate OAuth code path on the backend) — so that the server stays a single auth surface and we avoid running two identity systems.

**Why this priority**: P2 because email/password (US3) is enough to ship a playable MVP; Google sign-in is an adoption accelerant but not a launch blocker. Per analysis §6.4 decision, the chosen path is "Firebase handles Google OAuth client-side; server only verifies ID tokens."

**Independent Test**: Sign in with Google via the Firebase Web SDK (`signInWithPopup(googleProvider)`); call `GET /api/me`; observe (a) `provider = "google.com"` on the response, (b) the same `User` row whether the user previously signed in with email/password using the same verified email (Firebase's account-linking handles this).

**Acceptance Scenarios**:

1. **Given** the user completes Google sign-in via Firebase Web SDK, **When** they send the resulting ID token to `GET /api/me`, **Then** the bootstrap creates (or returns) a `User` row whose `provider = "google.com"` and `email = <Google email>`.
2. **Given** the same Google account had previously registered via `/api/auth/register` (email/password) with the same email, **When** Firebase links the providers (default behaviour for verified emails), **Then** the server sees one Firebase UID and one `User` row.
3. **Given** the ID token's `firebase.sign_in_provider` claim is `google.com`, **When** the bootstrap runs, **Then** the `provider` field on the persisted `User` is `"google.com"`. (We record only the primary sign-in provider for v1; multi-provider history is out of scope.)
4. **Given** the server has no Spring Security OAuth2 client configured, **When** auditors inspect the codebase, **Then** they find no `oauth2-client` or `oauth2-resource-server` dependency on the classpath. (Confirms analysis §7.1 decision.)

---

### User Story 5 - Manage and delete my account (Priority: P2)

As a privacy-conscious user, I need to be able to update my display name and photo URL and to delete my account entirely (Firebase user + Postgres row + any active match seats) — so that I am in control of my data on the platform.

**Why this priority**: P2 because LGPD (Brazilian data protection) compliance and the right-to-be-forgotten matter, but they do not block initial launch.

**Independent Test**: PATCH `/api/me` with a new display name → 200, profile reflects it; DELETE `/api/me` → 204; subsequent calls to any protected endpoint with the (now-revoked) ID token → 401; the `users` row is gone and any room/match the user was in is updated.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they `PATCH /api/me` with `{ displayName: "New Name" }`, **Then** the response is 200 with the updated profile and the change is mirrored to Firebase via `FirebaseAuth.updateUser(...)`.
2. **Given** an authenticated user, **When** they `DELETE /api/me`, **Then** the server (a) calls `FirebaseAuth.deleteUser(uid)`, (b) deletes the `users` row (cascade-delete handled by Spec 005's room/match foreign keys), (c) returns 204, (d) revokes any active WebSocket subscriptions (Spec 006).
3. **Given** the user is currently the host of an open room or a participant in an in-progress match, **When** they delete their account, **Then** the spec defers exact semantics to Spec 005 — but minimally, ownership transfers or the match ends — and `DELETE /api/me` MUST NOT silently leave dangling foreign keys.
4. **Given** the user's PATCH payload includes a `photoUrl` longer than 2048 characters, **When** the controller validates, **Then** Bean Validation rejects with `VALIDATION_FAILED`.

---

### User Story 6 - Service account is loaded predictably (Priority: P3)

As an operator, I need clear, fail-fast behaviour when the Firebase service-account JSON is missing or unreadable — so that I never have a running app that silently fails to verify every single token.

**Why this priority**: P3 user-visibility, but a P1 for operability. Per analysis §8 decision 2, `JWARSV_APP_FB_KEY` is a **file path** to a JSON document; the app must enforce that contract on startup.

**Independent Test**: Start the app with `JWARSV_APP_FB_KEY` unset → startup fails with a clear error; start with the env var set to a non-existent path → startup fails with a clear error; start with the env var set to a valid path → startup succeeds and the first protected request can be verified.

**Acceptance Scenarios**:

1. **Given** `JWARSV_APP_FB_KEY` is not set, **When** the app starts, **Then** the startup fails with a clear PT-BR error (`"Variável JWARSV_APP_FB_KEY não definida — chave de serviço Firebase é obrigatória"`) and exit code is non-zero.
2. **Given** `JWARSV_APP_FB_KEY` points to a non-existent or unreadable file, **When** the app starts, **Then** startup fails with a PT-BR error indicating the path and the reason.
3. **Given** `JWARSV_APP_FB_KEY` points to a valid service-account JSON, **When** the app starts, **Then** `FirebaseApp.initializeApp(...)` is invoked exactly once (singleton bean) and a startup log line confirms initialization.
4. **Given** the `test` profile is active, **When** the app starts, **Then** Firebase initialization is replaced by a mock/test double; no real service-account is required to run unit and slice tests.

---

### Edge Cases

- A user changes their email in Firebase (outside our system, e.g. through Firebase Console): the next `GET /api/me` MUST update the local `users.email` to match (server treats Firebase as the source of truth on identity attributes).
- A user signs in with email/password, then later with Google using the same email: Firebase links them by default (when the email is verified). Our bootstrap sees a single `firebase_uid` and a single `User` row.
- A token is valid but Firebase has revoked it (e.g. password change): `FirebaseAuth.verifyIdToken(token, /*checkRevoked=*/ true)` is called for protected endpoints; revoked tokens → 401 `TOKEN_REVOKED`. Decision: pay the round-trip cost for every request; cache verification result for 60s per token to soften it.
- A registration request races with another for the same email: Firebase enforces uniqueness; we surface the resulting `FirebaseAuthException` as 409.
- A user attempts `DELETE /api/me` while another tab still holds a valid ID token: subsequent calls 401 within the cache TTL (max 60s).
- The Firebase Admin SDK throws a transient network error during verification: filter returns 503 with `error.code = "AUTH_PROVIDER_UNAVAILABLE"`, not 401, so the client knows to retry.
- The `displayName` from Firebase may be null (especially for fresh email/password accounts); bootstrap MUST handle null and use the email's local-part as a fallback display name.
- `firebase_uid` is up to 128 characters per Firebase contract; the column is `VARCHAR(128) UNIQUE NOT NULL`.

---

## Requirements *(mandatory)*

### Functional Requirements

**Firebase configuration**

- **FR-001**: The system MUST load `firebase-service-account.json` from the path in env var `JWARSV_APP_FB_KEY` at startup, via a `@Configuration` bean.
- **FR-002**: The system MUST fail startup with a non-zero exit if `JWARSV_APP_FB_KEY` is missing or the file is unreadable. Error message MUST be in PT-BR (consistent with Constitution Principle III).
- **FR-003**: The system MUST initialize `FirebaseApp` exactly once (singleton bean named `firebaseAuth`); subsequent injections reuse it.
- **FR-004**: The `test` profile MUST allow tests to run without a real service-account, via a `@TestConfiguration` that supplies a mock `FirebaseAuth`.

**Token verification filter**

- **FR-005**: The system MUST register a `OncePerRequestFilter` (call it `FirebaseAuthFilter`) wired into the Spring Security filter chain immediately after `BearerTokenAuthenticationFilter`'s natural position.
- **FR-006**: The filter MUST read `Authorization: Bearer <token>`; if absent on a protected route, return 401 (`UNAUTHENTICATED`) using the Spec 003 error envelope.
- **FR-007**: The filter MUST call `FirebaseAuth.verifyIdToken(token, true)` (the `true` parameter enables revocation checking).
- **FR-008**: On successful verification, the filter MUST construct an `Authentication` whose principal carries: Firebase UID, email, `email_verified`, `firebase.sign_in_provider`, raw token (for downstream STOMP propagation in Spec 006).
- **FR-009**: On verification failure, the filter MUST distinguish the error type and return 401 with `error.code` in `{ "INVALID_TOKEN", "TOKEN_EXPIRED", "TOKEN_REVOKED" }`. Network/transient errors → 503 (`AUTH_PROVIDER_UNAVAILABLE`).
- **FR-010**: Verification result MAY be cached in-memory per token for up to 60 seconds to reduce Firebase round-trips. Cache MUST be size-bounded (default 10_000 entries) and MUST evict on `TOKEN_REVOKED` signals where possible.

**Endpoint access control**

- **FR-011**: Public routes (no authentication required): `GET /api/health`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /swagger-ui/**`, `GET /v3/api-docs/**`, `GET /actuator/health`, `GET /actuator/info`. All other `/api/**` routes require authentication.
- **FR-012**: `Authentication` must propagate to the controller through Spring Security's `@AuthenticationPrincipal` or a custom resolver returning a `CurrentUser` value object — controllers MUST NOT call `SecurityContextHolder` directly (testability).

**User bootstrap & profile**

- **FR-013**: On the first successful token verification per Firebase UID, the system MUST upsert a `User` row in Postgres: `(id UUID PK, firebase_uid VARCHAR(128) UNIQUE NOT NULL, email VARCHAR(320) NOT NULL, email_verified BOOLEAN NOT NULL, display_name VARCHAR(80), photo_url VARCHAR(2048), provider VARCHAR(64) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)`.
- **FR-014**: The upsert MUST be idempotent and concurrency-safe (DB-enforced uniqueness on `firebase_uid` + `ON CONFLICT DO UPDATE` semantics).
- **FR-015**: The system MUST expose `GET /api/me` returning `{ id, firebaseUid, email, emailVerified, displayName, photoUrl, provider, createdAt, updatedAt }`. Response MUST never include the raw Firebase token or any internal identifier other than the UUID.
- **FR-016**: The system MUST expose `PATCH /api/me` accepting `{ displayName?, photoUrl? }`. Validation: `displayName` 1–80 chars, `photoUrl` valid URI ≤ 2048 chars. Changes MUST also call `FirebaseAuth.updateUser(...)` so the Firebase profile stays consistent.
- **FR-017**: The system MUST expose `DELETE /api/me` that (a) calls `FirebaseAuth.deleteUser(uid)`, (b) deletes the `users` row, (c) returns 204. Cascading effects on rooms/matches are defined by Spec 005.

**Registration**

- **FR-018**: The system MUST expose `POST /api/auth/register` accepting `{ email, password, displayName }`. Validation: email format, displayName 1–80 chars, password ≥ 8 chars and meets strength rules (see Assumptions).
- **FR-019**: The endpoint MUST call `FirebaseAuth.createUser(...)` to create the Firebase account, then run the bootstrap path (FR-013), then mint a custom token via `FirebaseAuth.createCustomToken(uid)` and return `{ user, customToken }`.
- **FR-020**: If Firebase rejects the create with `email-already-exists`, return 409 (`EMAIL_ALREADY_IN_USE`); the local `users` table MUST NOT be touched.
- **FR-021**: The endpoint MUST be rate-limited per Spec 003 FR-019 (5/min/IP).

**Google sign-in**

- **FR-022**: The server MUST NOT participate in the Google OAuth redirect dance. Google sign-in completes entirely in the Firebase JS SDK; the server only ever verifies the resulting ID token via FR-007.
- **FR-023**: The bootstrap path (FR-013) MUST set `provider` from the token's `firebase.sign_in_provider` claim. For Google sign-in this is `"google.com"`; for email/password it is `"password"`.

**Logging & audit**

- **FR-024**: Every successful registration MUST be logged at INFO level with: correlation ID (from Spec 003 FR-015), Firebase UID, email, `provider`. PII handling: email is logged; passwords are NEVER logged at any level.
- **FR-025**: Every authentication failure MUST be logged at WARN with: correlation ID, IP, `error.code`. Raw inbound tokens MUST NOT be logged above DEBUG.

**Cleanup**

- **FR-026**: The empty `User.java` placeholder at `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java` (4 lines, see analysis §4) MUST be deleted.
- **FR-027**: A Flyway migration `V2__users.sql` MUST create the `users` table with the schema in FR-013 plus an index on `email`. (Numbering follows from the placeholder `V1__init.sql` introduced by Spec 003 FR-022 / FR-023.)

### Key Entities *(include if feature involves data)*

- **User**: persistent identity row in Postgres. Fields: `id` (UUID, primary key, server-generated), `firebaseUid` (unique, VARCHAR(128), from Firebase), `email`, `emailVerified`, `displayName`, `photoUrl`, `provider` (one of `"password"`, `"google.com"`; future-extensible), `createdAt`, `updatedAt`. Lives in `jwarsv-sboot` under `br.com.bnuuy.jwar.server.user.entity`. NOT a `jwarsv-core` class (Principle I).
- **UserRepository**: `JpaRepository<User, UUID>` with `findByFirebaseUid(String)` and an upsert helper. Lives in `jwarsv-sboot`.
- **CurrentUser**: HTTP-layer value object carried in the `SecurityContext`. Fields: `userId` (the internal UUID once bootstrap has run), `firebaseUid`, `email`, `displayName`, `provider`, `rawIdToken` (needed for STOMP handshake in Spec 006). Distinct from the `User` JPA entity; never serialized over the wire.
- **FirebaseAuthFilter**: contract — read `Authorization: Bearer`, call `FirebaseAuth.verifyIdToken(token, true)`, populate `SecurityContext` with `CurrentUser`, defer bootstrap to a separate `UserBootstrapper` bean called on cache miss.
- **RegistrationRequest**: HTTP DTO `{ email: String, password: String, displayName: String }` with Bean Validation.
- **RegistrationResponse**: HTTP DTO `{ user: UserDto, customToken: String }`.
- **UserDto**: HTTP DTO mirroring `User` minus internal-only fields; produced via MapStruct.
- **UpdateMeRequest**: HTTP DTO `{ displayName?, photoUrl? }`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of `/api/**` endpoints outside the explicit allow-list (FR-011) reject unauthenticated requests with 401. Verified by an integration test that walks the discovered controller methods.
- **SC-002**: Token verification adds less than 30ms p95 to the request path when the in-memory verification cache (FR-010) is warm; less than 200ms p95 on cold cache (real Firebase call).
- **SC-003**: `POST /api/auth/register` completes (Firebase create + DB upsert + custom token mint) in under 2s p95.
- **SC-004**: Bootstrap is provably idempotent: in a load test issuing 100 concurrent first-time requests for the same Firebase UID, the `users` table contains exactly one row for that UID at the end.
- **SC-005**: A token revoked via `FirebaseAuth.revokeRefreshTokens(uid)` is rejected by the filter within `60s + verification-cache-TTL` worst case, where the verification cache TTL is the value set in FR-010 (default 60s) — so worst case 120s, target less than 65s when invalidation is wired.
- **SC-006**: An ArchUnit test confirms no `jwarsv-core/**` class imports `firebase`, `spring-security`, or `org.springframework.web` — Principle I is mechanically enforced.
- **SC-007**: A startup smoke test fails fast (exit non-zero within 10s) when `JWARSV_APP_FB_KEY` is unset.

## Assumptions

- The frontend uses Firebase Web SDK for the actual sign-in UI (email/password form, Google popup, password reset emails). The backend is purely a resource server that trusts Firebase ID tokens. This decision is final per analysis §7.1 + §8 decision 3.
- Password strength rules (FR-018) follow Firebase's minimum (6 chars) but we tighten to: ≥ 8 chars, at least one letter, at least one digit. No special-character requirement (NIST 800-63B guidance).
- Email verification is **not** enforced at login in v1 (we record `email_verified` but do not gate access on it). Future spec may add per-feature gates.
- Firebase Console is treated as a trusted admin surface; password resets and email verification emails are sent by Firebase, not by us.
- `JWARSV_APP_FB_KEY` is a path to a JSON file mounted into the container at `/etc/jwar/firebase-service-account.json` (per analysis §8 decision 2). The deploy pipeline (out of scope here) is responsible for placing the file with `chmod 400`.
- Spring Security is added in this spec (`spring-boot-starter-security`) — it was deliberately excluded from Spec 003 to keep that spec's scope tight. The dependency is added in `jwar-server/jwarsv-sboot/build.gradle`.
- Spring Security OAuth2 client / resource server is **NOT** added; the verification flow is hand-rolled in `FirebaseAuthFilter`. This is intentional per analysis §6.4 / §7.1.
- The `users.id` UUID is the value used as the foreign key in all downstream tables (rooms, room_memberships, matches, match_participants in Spec 005). The Firebase UID is *not* used as a foreign key (it could change semantics if Firebase Auth ever migrates).
- Match-state coupling on user deletion (FR-017) is handled cooperatively with Spec 005; minimally, hosts of open rooms transfer the host role or the room closes, and participants of in-progress matches have their seat marked as "abandoned" without crashing the engine.
- The `ClassicGamePlayer` core type at `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/ClassicGamePlayer.java:13-124` already carries a `String userId` (per analysis §4); Spec 005 will populate it with the `User.id.toString()` value from this spec.
- Caffeine (already on the classpath at `application.yml:44-47`) is the chosen cache for the verification cache (FR-010). No additional dependency required.
