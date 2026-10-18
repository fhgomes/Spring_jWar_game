# Feature Specification: Game Rooms & Match Lifecycle

**Feature Branch**: `005-game-rooms-and-matches`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Players gather in a room (3–6 seats), pick a color, optionally chat, and the host starts a match. Once started, the room is bound to a live `ClassicGame` held in-memory keyed by match ID; match metadata is persisted to Postgres for history. A user can list their past matches and (v1) rooms close when the match ends."

This spec depends on **Spec 003** (REST foundation) and **Spec 004** (auth + `User` entity + `CurrentUser` in `SecurityContext`). It is a prerequisite for **Spec 006** (realtime), which assumes the in-memory `MatchRegistry` and the `Room`/`Match` REST surface defined here.

Anchored in:
- `docs/analysis/03-infra-auth-rest-state.md` §6.1 (REST surface enumeration), §7.3 (hybrid persistence: in-memory `ClassicGame`, Postgres for metadata), §8 decisions 4 (private rooms via `is_public` flag), 5 (room→match 1:1 for v1), 7 (no in-flight match persistence in v1), 8 (no spectators in v1), 9 (only participants can call match endpoints).
- Engine entry points: `ClassicGameLobby` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameLobby.java:13-43`) — `joinLobby` builds the player list, `startMatch` invokes `classicGame.startMatch(players)` (`ClassicGameLobby.java:30-32`).
- Engine state: each `ClassicGame` carries a `matchId` (per analysis §7.3 referencing `ClassicGame.java:59`); `ClassicGamePlayer.userId` is the Firebase-UID-derived identity (see analysis §4 referencing `ClassicGamePlayer.java:15`).
- Engine color enum: `EGameColors` (per `CLAUDE.md`).

Constitution alignment:
- **Principle I** — all persistence and HTTP code lives in `jwarsv-sboot`; the engine in `jwarsv-core` is consumed via existing methods only. No new core-level dependencies introduced.
- **Principle IV** — sboot depends on core; the `MatchRegistry` Spring bean holds references to `ClassicGame` instances but does not modify their internals.
- **Principle V** — match state stays in-memory in v1 (analysis §7.3); rooms persisted only at the metadata level needed for listing, host-transfer, and history.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create, list, join a room (Priority: P1)

As an authenticated user, I need to create a public room with a name and seat-count constraints, see other people's open rooms in a list, and join one by choosing an available color — so that I can find 2–5 other players and start a game.

**Why this priority**: P1 because without rooms there is no way to assemble the 3–6 players the engine requires (see `ClassicGameLobby.validateCanJoin` which caps at 6, `ClassicGameLobby.java:35-37`).

**Independent Test**: User A creates a room `{ name: "Sala do A", maxPlayers: 4 }`; user B calls `GET /api/rooms?status=open` and sees it; user B joins picking color `BLUE`; both users see two members in `GET /api/rooms/{id}`.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they `POST /api/rooms` with `{ name: "Sala do A", minPlayers: 3, maxPlayers: 4 }`, **Then** the response is 201 with the new `Room` payload, the user is the host and the sole member, and the room status is `OPEN`.
2. **Given** an authenticated user, **When** they call `GET /api/rooms?status=open&page=0&size=20`, **Then** they receive a paginated list of rooms not yet started, ordered by `createdAt DESC`.
3. **Given** an open room with 1 member, **When** another authenticated user `POST /api/rooms/{id}/join` with `{ color: "BLUE" }`, **Then** the response is 200, the room now has 2 members, and the joiner's color is `BLUE`.
4. **Given** a room of `maxPlayers = 3` with 3 members, **When** a 4th user attempts to join, **Then** the response is 400 with PT-BR message `"Não é póssível entrar, a sala está cheia"` (mirrors the engine message at `ClassicGameLobby.java:36`) and the room status is now `FULL`.
5. **Given** a user already a member of a room, **When** they attempt to join the same room again, **Then** the response is 400 (`error.code = "ALREADY_MEMBER"`) — mirrors engine check at `ClassicGameLobby.java:39-41`.
6. **Given** a room where colors `RED` and `BLUE` are taken, **When** a new joiner requests `RED`, **Then** the response is 400 (`error.code = "COLOR_TAKEN"`) listing the available colors.
7. **Given** any user, **When** they call `POST /api/rooms` with `maxPlayers = 7`, **Then** Bean Validation rejects with `VALIDATION_FAILED` (3 ≤ maxPlayers ≤ 6).

---

### User Story 2 - Leave a room and start a match (Priority: P1)

As a host or a member, I need to leave a room cleanly (with host transfer if I'm the host and the room hasn't started), and as the host I need to start the match when there are at least 3 members — so that the lobby can flow naturally into gameplay.

**Why this priority**: P1 because the host-transfer / room-closure path is the first place a real-world flow will encounter mid-lobby drop-outs, and "start match" is the moment the system flips from REST-only to realtime. Without it, the rest of Spec 006 cannot be exercised.

**Independent Test**: A 3-member room, host calls `POST /api/rooms/{id}/start` → 200, room status = `IN_PROGRESS`, a `Match` row exists, the engine's `ClassicGame` is in the `MatchRegistry` keyed by `match.id`. Separately: host leaves a 2-member open room → another member becomes host; host leaves a 1-member open room → room closes.

**Acceptance Scenarios**:

1. **Given** an authenticated member of an open room, **When** they `POST /api/rooms/{id}/leave`, **Then** the response is 200 and the membership row is removed.
2. **Given** the host of a 2-member open room, **When** they leave, **Then** the remaining member is promoted to host (`isHost = true`) and the room remains `OPEN`.
3. **Given** the host of a 1-member open room, **When** they leave, **Then** the room transitions to `CLOSED` and is excluded from default listings.
4. **Given** a host of an `IN_PROGRESS` room, **When** they attempt to leave, **Then** the response is 400 (`error.code = "MATCH_IN_PROGRESS"`); leaving an active match goes through the gameplay path in Spec 006.
5. **Given** the host of a room with ≥ 3 and ≤ 6 members and status `OPEN`, **When** they `POST /api/rooms/{id}/start`, **Then** the server constructs a `ClassicGame`, calls `ClassicGameLobby.joinLobby(...)` for each member (mapping `User.id.toString()` → `ClassicGamePlayer.userId`), calls `ClassicGameLobby.startMatch()` (per `ClassicGameLobby.java:30-32`), creates a `Match` row, registers the `ClassicGame` in `MatchRegistry` keyed by `match.id`, transitions room status to `IN_PROGRESS`, and broadcasts a `MATCH_STARTED` event over WebSocket (Spec 006).
6. **Given** a non-host member, **When** they call `POST /api/rooms/{id}/start`, **Then** the response is 403 (`error.code = "NOT_HOST"`).
7. **Given** a host of a 2-member room, **When** they call `start`, **Then** the response is 400 (`error.code = "NOT_ENOUGH_PLAYERS"`).
8. **Given** a freshly-started match, **When** any participant calls `GET /api/matches/{id}`, **Then** they receive the initial game-state snapshot (territories distributed, objectives assigned per Manual §2.3, currentPlayer set, phase = `ADD_TROOPS`).

---

### User Story 3 - Room chat and match history (Priority: P2)

As a player, I want to send short text messages to others in the room before the match starts (and during it), and I want to view a list of my past matches with their outcome — so that the social side of the table and the bragging-rights archive are both present.

**Why this priority**: P2 because chat is a nicety that improves UX significantly but is not required to play a game; match history is a retention feature, not a launch blocker.

**Independent Test**: User A `POST /api/rooms/{id}/messages` `{ text: "boa sorte" }`; user B in the same room receives it via the room's WebSocket topic and via `GET /api/rooms/{id}/messages?since=...`. Separately: a user with 3 finished matches calls `GET /api/me/matches` and receives 3 entries with status and winner.

**Acceptance Scenarios**:

1. **Given** an authenticated member of a room, **When** they `POST /api/rooms/{id}/messages` with `{ text: "boa sorte" }`, **Then** the message is persisted with server timestamp and author, and broadcast via Spec 006's `/topic/rooms/{roomId}` channel.
2. **Given** a chat payload, **When** the text is longer than 280 characters, **Then** Bean Validation rejects with `VALIDATION_FAILED`.
3. **Given** a chat payload, **When** the text contains only whitespace, **Then** the request is rejected (400, `EMPTY_MESSAGE`).
4. **Given** a non-member of a room, **When** they POST a chat message to it, **Then** the response is 403.
5. **Given** an authenticated user, **When** they call `GET /api/me/matches?status=finished&page=0&size=20`, **Then** they receive a paginated list of `Match` rows where they were a participant, with fields `{ matchId, startedAt, finishedAt, status, winnerUserId, myColor, myFinalStatus }`.
6. **Given** an active match, **When** any participant calls `GET /api/me/matches?status=in_progress`, **Then** the match appears in their list.

---

### User Story 4 - Private (password-protected) rooms (Priority: P3)

As a user organizing a game with friends, I need to mark a room as private with a password, so that strangers cannot accidentally fill the seats.

**Why this priority**: P3 because public rooms (US1) cover the launch use case; private rooms are an obvious-but-deferrable enhancement. Per analysis §8 decision 4, both flavors will exist but public is the default.

**Independent Test**: Create a room with `{ isPublic: false, password: "abc123" }`; another user calls `GET /api/rooms` and the room does not appear in the default listing; the other user calls `POST /api/rooms/{id}/join` with `{ color, password: "abc123" }` and succeeds; the same call with the wrong password returns 401.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they `POST /api/rooms` with `{ isPublic: false, password: "abc123" }`, **Then** the room is created with `isPublic = false` and a hashed password (BCrypt) stored in the DB.
2. **Given** a private room, **When** anyone calls `GET /api/rooms?status=open`, **Then** the response is identical to a public listing call with the private room **omitted**, unless the caller is already a member.
3. **Given** a private room id (obtained out of band, e.g. shared link), **When** an authenticated user `POST /api/rooms/{id}/join` with the correct password, **Then** they join successfully.
4. **Given** an incorrect password, **When** join is attempted, **Then** the response is 401 (`error.code = "BAD_ROOM_PASSWORD"`). Brute-force attempts MUST be rate-limited per Spec 003 FR-019 (apply the gameplay-endpoint bucket).
5. **Given** a private room with no password (host explicitly set `isPublic = false` but no password), **When** validation runs, **Then** the create request is rejected (`VALIDATION_FAILED`: "private room requires a password").

---

### Edge Cases

- Host of an open room deletes their account (Spec 004 FR-017): host transfers to the longest-tenured remaining member; if no members remain, the room is `CLOSED`.
- Participant of an in-progress match deletes their account: their seat is marked as `ABANDONED` in `match_participants` but the engine continues; Spec 006 governs whether the engine treats them as auto-pass.
- The same user attempts to host two open rooms at once: allowed in v1 (no constraint); the UI may guide them, the server does not enforce.
- A room exists but has been `CLOSED` for more than 30 days: a future cleanup job MAY hard-delete; out of scope for this spec.
- The engine's `joinLobby` is called with a 7th player by a buggy client: the engine throws `GameRulesException("Não é póssível entrar, a sala está cheia")` (`ClassicGameLobby.java:36`); Spec 003's exception handler converts to 400. The room-level check (FR-007) should reject earlier, before the engine sees it; the engine guard is a defense-in-depth.
- Server restarts: per analysis §7.3 / §8 decision 7, all in-memory `ClassicGame` instances are lost. The corresponding `Match` rows are marked `ABANDONED` by a startup hook; their rooms are `CLOSED`. Open rooms persist (their state is in Postgres) and can be rejoined after restart.
- Two users race to join the last seat of a full room: a Postgres `UNIQUE` constraint on `(room_id, color)` plus a `room_memberships` count check inside a serializable transaction ensures only one succeeds; the other receives 409 (`CONFLICT_ROOM_FULL`).
- The chat history is unbounded in v1; a future cleanup job is out of scope. Per-room cap of 500 messages enforced via a delete-oldest trigger or an application-level guard.

---

## Requirements *(mandatory)*

### Functional Requirements

**Rooms — REST**

- **FR-001**: The system MUST expose `POST /api/rooms` accepting `{ name, minPlayers, maxPlayers, isPublic, password? }`. Validation: `name` 1–80 chars, `3 ≤ minPlayers ≤ maxPlayers ≤ 6`, `isPublic` defaults to `true`, `password` only meaningful when `isPublic = false`. Response 201 with the created `Room`.
- **FR-002**: The system MUST expose `GET /api/rooms?status=<open|full|in_progress|finished>&page=<int>&size=<int>` returning a paginated list. Default `status=open`. Private rooms (`isPublic = false`) are excluded from the listing unless the caller is a member.
- **FR-003**: The system MUST expose `GET /api/rooms/{id}` returning the full room state (members, host, status). Only members of a private room may call it; non-members get 404 (not 403, to avoid leaking existence).
- **FR-004**: The system MUST expose `POST /api/rooms/{id}/join` accepting `{ color, password? }`. Validation: `color` is one of `EGameColors` and not already taken; `password` required iff room is private.
- **FR-005**: The system MUST expose `POST /api/rooms/{id}/leave` (idempotent: leaving a room you're not in returns 200 with `{ status: "noop" }`).
- **FR-006**: The system MUST expose `POST /api/rooms/{id}/start`. Host-only; requires `minPlayers ≤ memberCount ≤ maxPlayers` and room status `OPEN`.

**Rooms — invariants**

- **FR-007**: A user MUST NOT be a member of the same room twice (DB constraint `UNIQUE(room_id, user_id)`).
- **FR-008**: A color MUST NOT be assigned twice within a room (DB constraint `UNIQUE(room_id, color)`).
- **FR-009**: Joining a room of status other than `OPEN` MUST return 400 (`ROOM_NOT_JOINABLE`).
- **FR-010**: When the host leaves an `OPEN` room, host MUST transfer to the next-oldest member (`joinedAt ASC`). If no members remain, the room transitions to `CLOSED`.
- **FR-011**: The host of an `IN_PROGRESS` room MUST NOT leave the room via `/leave`; they must use the gameplay layer (Spec 006).
- **FR-012**: All transitions of `Room.status` MUST be recorded in an append-only `room_status_history` table for debugging (event-sourced minimum: id, room_id, old_status, new_status, at, actor_user_id).

**Match start**

- **FR-013**: `POST /api/rooms/{id}/start` MUST, inside a single transaction:
  1. Mark the room `IN_PROGRESS` and stamp `startedAt`.
  2. Create a `Match` row with `roomId`, `startedAt`, `status = IN_PROGRESS`, `currentTurnUserId = null` (set after engine init), `currentPhase = null` (set after engine init).
  3. Insert `match_participants` rows for each member: `(match_id, user_id, color, final_status = IN_GAME)`.
- **FR-014**: After the transaction commits, the system MUST construct a `ClassicGame`, build a `ClassicGameLobby`, call `joinLobby` for each member (mapping `User.id.toString()` to `ClassicGamePlayer.userId`), and call `startMatch()` (per `ClassicGameLobby.java:30-32`). The resulting `ClassicGame` instance is registered in a `MatchRegistry` Spring bean keyed by `Match.id` (UUID).
- **FR-015**: The `Match.currentTurnUserId` and `Match.currentPhase` MUST be updated from the engine state after `startMatch()` returns.
- **FR-016**: A `MATCH_STARTED` event MUST be broadcast on `/topic/rooms/{roomId}` (and `/topic/matches/{matchId}` once the topic exists per Spec 006).
- **FR-017**: If the engine throws `GameRulesException` during `startMatch` (e.g. distribution failure), the transaction MUST roll back, the room returns to `OPEN`, and the response is 400 with the engine's PT-BR message — surfaced through Spec 003's handler.

**Match registry (in-memory)**

- **FR-018**: The system MUST expose a `MatchRegistry` Spring `@Component` holding a `ConcurrentHashMap<UUID, ClassicGame>`. Operations: `register(matchId, game)`, `get(matchId) → ClassicGame`, `remove(matchId)`. Thread-safe.
- **FR-019**: On `MATCH_FINISHED` or `MATCH_ABANDONED` (driven by the gameplay actions in Spec 006), the entry MUST be removed from `MatchRegistry` and the `Match.status` updated.
- **FR-020**: On application startup, any `Match` row in `IN_PROGRESS` whose `ClassicGame` is not in memory (i.e. lost across restart, per analysis §8 decision 7) MUST be transitioned to `ABANDONED` by a `CommandLineRunner` and the host room to `CLOSED`.

**Chat (room messages)**

- **FR-021**: The system MUST expose `POST /api/rooms/{id}/messages` accepting `{ text }` where `text` is 1–280 chars, trimmed, non-empty after trim.
- **FR-022**: The system MUST expose `GET /api/rooms/{id}/messages?since=<ISO-8601>&limit=<int>` returning recent messages.
- **FR-023**: Messages MUST be broadcast via Spec 006's WebSocket channel `/topic/rooms/{roomId}` immediately on persist.
- **FR-024**: Per-room message storage is capped at 500; oldest pruned on insert.
- **FR-025**: Only members of the room may post or read messages.

**History**

- **FR-026**: The system MUST expose `GET /api/me/matches?status=<in_progress|finished|abandoned>&page&size` returning paginated `Match` rows the caller participated in, with `{ matchId, startedAt, finishedAt, status, winnerUserId, myColor, myFinalStatus }`.

**Authorization invariants**

- **FR-027**: All endpoints in this spec require authentication (per Spec 004 FR-011).
- **FR-028**: Only members of a match may call any match-scoped endpoint (defined here or in Spec 006). Enforcement: a shared `MatchAuthorization` bean that consults `match_participants`. Non-members get 403, except when the room is private and they are not a member — then 404 (existence-hiding, see FR-003).

**Migrations**

- **FR-029**: A Flyway migration `V3__rooms_and_matches.sql` MUST create: `rooms`, `room_memberships`, `room_status_history`, `room_messages`, `matches`, `match_participants` — with the constraints listed in FR-007, FR-008, and the foreign keys to `users.id` (FK introduced in Spec 004 FR-027).

### Key Entities *(include if feature involves data)*

- **Room**: `{ id (UUID), name, hostUserId (FK users.id), status (OPEN|FULL|IN_PROGRESS|CLOSED|FINISHED), minPlayers, maxPlayers, isPublic, passwordHash (nullable), createdAt, updatedAt, startedAt (nullable), endedAt (nullable) }`. Lives in `jwarsv-sboot`.
- **RoomMembership**: `{ roomId, userId, color (one of EGameColors), joinedAt, isHost }`. Unique on `(roomId, userId)` and `(roomId, color)`.
- **RoomStatusHistory**: append-only `{ id, roomId, oldStatus, newStatus, at, actorUserId }`.
- **RoomMessage**: `{ id, roomId, authorUserId, text, postedAt }`.
- **Match**: `{ id (UUID), roomId, startedAt, finishedAt (nullable), status (IN_PROGRESS|FINISHED|ABANDONED), winnerUserId (nullable), currentTurnUserId (nullable; mirrored from engine), currentPhase (nullable; mirrored from engine ADD_TROOPS|ATTACK|MOVE_TROOPS — see `ClassicGameConstants` at `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameConstants.java`) }`.
- **MatchParticipant**: `{ matchId, userId, color, finalStatus (IN_GAME|ELIMINATED|WINNER|ABANDONED) }`. Unique on `(matchId, userId)` and `(matchId, color)`.
- **MatchRegistry**: in-memory Spring bean (not persisted), `ConcurrentHashMap<UUID, ClassicGame>` keyed by `Match.id`. Lifecycle bound to JVM (per analysis §8 decision 7).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A room can be created, filled with 3 players, started, and reach the first `MATCH_STARTED` event in under 3 seconds end-to-end on a local stack (P95 over 100 runs).
- **SC-002**: The platform supports at least 100 concurrent `OPEN` rooms and 30 concurrent `IN_PROGRESS` matches on a single JVM without exceeding the existing Hikari pool limits (currently `maximumPoolSize: 2` per `application.yml:19`; this spec MAY raise it but the raise must be justified by load-test data).
- **SC-003**: 100% of room and match endpoints reject non-members with the correct status code (403 for public, 404 for private to hide existence). Verified by integration tests.
- **SC-004**: After a JVM restart with N in-progress matches, all corresponding `Match` rows transition to `ABANDONED` within 5 seconds of startup, and no `ClassicGame` instance is ever consulted from a stale registry entry.
- **SC-005**: A 6-player room start invokes `ClassicGameLobby.joinLobby` exactly 6 times in seat-order; verified by an integration test asserting against a spied `ClassicGameLobby`.
- **SC-006**: Race-test: 50 concurrent join requests targeting the last seat of a `maxPlayers = 3` room result in exactly 1 success and 49 rejections (mixed 400/409). No duplicate memberships.
- **SC-007**: A user's match-history page returns within 500ms p95 for users with up to 200 historical matches.

## Assumptions

- The `EGameColors` enum already lists exactly 6 colors (`Red, Blue, Green, Yellow, Purple, Gray` per `CLAUDE.md`); the color picker UI is out of scope here.
- The engine's `ClassicGamePlayer.userId` (`String`, see analysis §4 referencing `ClassicGamePlayer.java:15`) is populated with `User.id.toString()` (the UUID from Spec 004 FR-013). This is the single bridge between the engine's identity model and the platform's.
- Per analysis §8 decision 5, the room → match relationship is **1:1 in v1**: when the match ends (FINISHED or ABANDONED), the room transitions to `CLOSED` and cannot host another match. Multi-match rooms are a v2 feature.
- Per analysis §8 decision 7, in-flight match state is **not persisted** across server restarts. The `Match` row is metadata only; the `ClassicGame` engine state is lost on restart. Future spec may add event-sourced persistence.
- Per analysis §8 decision 8, **no spectators in v1**: only the 3–6 seated players may call match endpoints.
- Per analysis §8 decision 9, match authorization is enforced inside the controller layer (or a shared `@PreAuthorize`); not via Spring Security URL patterns.
- The chat (US3) does NOT support file uploads, emoji reactions, threading, or moderation tools in v1. Plain text only.
- Room password hashing uses BCrypt with cost 10 (Spring Security `BCryptPasswordEncoder` becomes available via Spec 004's `spring-boot-starter-security` dependency).
- The WebSocket broadcast hook (FR-016, FR-023) is implemented in Spec 006; this spec defines the **trigger points** but the actual `SimpMessagingTemplate.convertAndSend` call is Spec 006's responsibility. Until Spec 006 ships, the broadcast points may be marked with `// TODO Spec 006 — broadcast` or implemented as no-ops behind a feature flag.
- The TODO comments in `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:79, :93, :120` ("`TODO SPRINT2 - COMNS - send update to other players …`") are addressed by Spec 006, not here. This spec only sets the registry plumbing.
- `Match.currentTurnUserId` and `Match.currentPhase` are denormalized mirrors of in-memory engine state, refreshed on every gameplay action (Spec 006 will write them after each successful mutation). They exist for efficient `GET /api/me/matches` queries and to give the UI a fast hydration path before the WebSocket snapshot arrives.
- Pagination parameters (`page`, `size`) follow Spring Data Pageable conventions; `size` is capped at 50 server-side.
- Brazilian Portuguese is used for all user-facing error messages (Constitution Principle III), mirroring engine messages like the one at `ClassicGameLobby.java:36`.
