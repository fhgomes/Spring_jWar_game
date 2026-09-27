# Feature Specification: Realtime Gameplay over STOMP/WebSocket

**Feature Branch**: `006-realtime-gameplay`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Players in an active match receive turn updates, attack results, troop deployments, and card-exchange notifications over a STOMP-over-WebSocket channel without polling. Auth is performed once at handshake using the Firebase ID token from Spec 004. Game commands remain REST POSTs that mutate the in-memory `ClassicGame` via `ClassicGamePActions`, and the server broadcasts the resulting events to subscribers."

This is the final platform spec in the initial sequence. It depends on:
- **Spec 003** — error envelopes, validation, OpenAPI documentation for the new REST command endpoints, correlation IDs propagated through STOMP messages.
- **Spec 004** — Firebase ID token verification (re-used at STOMP handshake), `CurrentUser.rawIdToken` carried into the STOMP channel.
- **Spec 005** — `MatchRegistry` Spring bean holding the in-memory `ClassicGame`, `Match` and `MatchParticipant` tables, the membership/authorization checks.

Anchored in:
- `docs/analysis/03-infra-auth-rest-state.md` §6.1 (REST surface enumeration), §6.2 (no transport for engine push hooks exists today), §7.2 (STOMP-over-WebSocket chosen), §8 decisions 6 (disconnect timeout 90s), 10 (topic naming `/topic/matches/{matchId}` + `/user/queue/private`), 11 (handshake auth via query param `?token=`).
- Engine push hook TODOs: `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:79` (after `addTroops`), `:93` (after `addContinentTroops`), `:120` (after `exchangeCards`); `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java:139-140` (after `startMatch`).
- Engine action surface: `ClassicGamePActions.attack` (`:32-48`), `endCurrentTurnAttackPhase` (`:50-53`), `endCurrentTurnAddPhase` (`:55-58`), `endCurrentTurn` (`:60-65`), `addTroops` (`:67-80`), `addContinentTroops` (`:82-94`), `exchangeCards` (`:105-121`). A `moveTroops` engine method does not yet exist (per analysis §6.1) — this spec assumes it is added as part of Spec 002 (mechanics completeness) before Spec 006 ships, or is stubbed with a `GameRulesException` until then.

Constitution alignment:
- **Principle I** — STOMP/WebSocket plumbing lives entirely in `jwarsv-sboot`. The engine in `jwarsv-core` has no awareness of STOMP, no `SimpMessagingTemplate` import. Engine push hooks (the `TODO SPRINT2 - COMNS` comments) are resolved by emitting Spring `ApplicationEvent`s **from the controller layer** after each successful action, not from inside the engine.
- **Principle IV** — sboot depends on core; the gameplay-action controllers translate HTTP commands into `ClassicGamePActions` calls and broadcast the resulting events.
- **Principle V** — Spring's in-memory simple broker is sufficient (analysis §7.2). No Redis, no Kafka, no external broker. No durable subscriptions in v1.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authenticated subscription to a match (Priority: P1)

As a player in an active match, I need to connect once to a WebSocket endpoint and subscribe to the match's event topic so that I receive turn changes, attack results, and troop deployments in real time without polling — but only if I am a seated participant.

**Why this priority**: P1 because without the push channel, every player has to poll `GET /api/matches/{id}` repeatedly. The engine's TODO comments at `ClassicGamePActions.java:79, :93, :120` describe exactly this push as a P1 missing piece.

**Independent Test**: An authenticated participant in `match-X` opens a WebSocket to `/ws?token=<idToken>`, sends a STOMP `SUBSCRIBE` to `/topic/matches/{X}`, and receives a `MATCH_SNAPSHOT` message within 500ms. A non-participant attempting the same `SUBSCRIBE` is rejected.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they open a WebSocket to `wss://api.example/ws?token=<firebase-id-token>`, **Then** the server's `HandshakeInterceptor` calls Spec 004's token-verification path and binds a `Principal` carrying the Firebase UID and internal `userId` to the session.
2. **Given** the handshake token is missing, expired, or revoked, **When** the WebSocket upgrade request is processed, **Then** the server responds 401 / 403 on the upgrade and the connection is not established.
3. **Given** a connected user, **When** they send STOMP `SUBSCRIBE destination:/topic/matches/{matchId}`, **Then** the server's `ChannelInterceptor` (preSend on SUBSCRIBE) confirms the user is a participant of `matchId` (via Spec 005's `MatchAuthorization`) and either accepts the subscription or sends an `ERROR` frame.
4. **Given** a non-participant attempts to subscribe, **When** the interceptor checks membership, **Then** the server sends an `ERROR` frame with `message: "Acesso negado à partida"` and disconnects the channel (does NOT send the subscription confirmation).
5. **Given** a connected and subscribed participant, **When** the server emits any of `TROOPS_ADDED`, `ATTACK_RESULT`, `TROOPS_MOVED`, `PHASE_ENDED`, `TURN_CHANGED`, `MATCH_FINISHED`, **Then** the client receives the corresponding STOMP MESSAGE within 200ms p95 of the originating command's HTTP response.

---

### User Story 2 - Gameplay command endpoints (Priority: P1)

As a player, I need REST endpoints that mutate the live match state — adding troops, exchanging cards, attacking, moving troops, ending phases — and broadcast the resulting events to every subscriber so that all players see the same evolving game state.

**Why this priority**: P1 because the engine already exposes the action surface (`ClassicGamePActions` at `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:24-123`); without HTTP wrappers no client can drive it. This is the moment the platform stops being a stub and becomes playable.

**Independent Test**: For a 3-player match where it is player A's turn (ADD_TROOPS phase), player A `POST /api/matches/{id}/add-troops { qtdTroops, tgtCountry }` → 200 (or 204), and players B and C receive a `TROOPS_ADDED` event with the same `correlationId`. Player B attempting the same call gets 403 (`NOT_YOUR_TURN`).

**Acceptance Scenarios**:

1. **Given** an authenticated participant of an `IN_PROGRESS` match where it is their turn and the phase matches, **When** they `POST /api/matches/{id}/add-troops` with `{ qtdTroops, tgtCountry, expectedTurnUserId, expectedPhase }`, **Then** the server calls `ClassicGamePActions.addTroops(...)` (`ClassicGamePActions.java:67-80`), updates `Match.currentTurnUserId` and `Match.currentPhase`, broadcasts a `TROOPS_ADDED` event to `/topic/matches/{id}`, and returns 200 with no body.
2. **Given** the same precondition with mismatched `expectedTurnUserId` (stale optimistic state), **When** the controller checks before invoking the engine, **Then** the response is 409 (`STALE_STATE`) with `{ currentTurnUserId, currentPhase }` so the client can resync.
3. **Given** a player who is not the current turn-holder, **When** they call any gameplay endpoint, **Then** the engine throws `GameRulesException` via `isMyTurn` (referenced from `ClassicGamePActions.java:33, :51, :56, :61, :68, :83, :107`); Spec 003's handler converts to 400 with the PT-BR message.
4. **Given** an attack action, **When** the engine returns an `AttackResultVO` (per `ClassicGamePActions.attack` at `:32-48`), **Then** the broadcast `ATTACK_RESULT` event carries the dice rolls, troop losses, and whether the target was conquered — but the attacker's card-draw is broadcast publicly as "card drawn" only; the actual card identity goes to the attacker's private channel (see US3).
5. **Given** a `POST /api/matches/{id}/exchange-cards` with valid card codes, **When** the engine processes via `ClassicGamePActions.exchangeCards` (`:105-121`), **Then** the server broadcasts a `CARDS_EXCHANGED` event carrying the bonus troops gained (publicly visible) but NOT the discarded card identities (delivered privately per US3).
6. **Given** a `POST /api/matches/{id}/end-phase`, **When** the engine returns from `endCurrentTurnAddPhase`, `endCurrentTurnAttackPhase`, or `endCurrentTurn` (`:50-65`), **Then** a `PHASE_ENDED` or `TURN_CHANGED` event is broadcast and `Match.currentPhase` / `Match.currentTurnUserId` are updated.
7. **Given** any gameplay action that triggers a match-end (last objective complete, sole survivor, etc.), **When** the engine signals completion, **Then** a `MATCH_FINISHED` event is broadcast carrying the winner's user id, the match is removed from `MatchRegistry` (Spec 005 FR-019), and `Match.status` is set to `FINISHED`.

---

### User Story 3 - Private per-player channel for hand and snapshot (Priority: P2)

As a player, I need a private STOMP channel that only I can read, so that my objective card and my hand of country cards stay secret per the Manual's §9 (cards remain hidden until played).

**Why this priority**: P2 because v1 can technically ship with all card data inlined into the snapshot and hand updates broadcast publicly (UX would degrade — anyone watching the network would see opponents' hands), but to honor the rulebook the private channel is the correct model from day one.

**Independent Test**: Player A subscribes to `/user/queue/cards`; another player attacks and conquers a territory of A's — A receives a `CARDS_REVEALED` event on the private queue when A draws a card; other players see only the public `ATTACK_RESULT` without card content. Separately: any participant subscribing to `/topic/matches/{id}` immediately receives a single `MATCH_SNAPSHOT` message with the public game state.

**Acceptance Scenarios**:

1. **Given** a participant, **When** they `SUBSCRIBE` to `/user/queue/cards`, **Then** Spring's `SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/cards", payload)` delivers messages only to that user's session(s).
2. **Given** a player draws a card after a successful conquest, **When** the engine records the new card, **Then** the server emits to `/user/queue/cards` for that player only: `{ event: "CARD_DRAWN", card: { countryCode, shape } }`. No other player's channel receives the card identity.
3. **Given** a player exchanges cards, **When** the engine processes the exchange (`ClassicGamePActions.exchangeCards`), **Then** the public broadcast on `/topic/matches/{id}` is `CARDS_EXCHANGED { byUserId, troopsGained }` — without card details — and the player's private queue receives `CARDS_DISCARDED { cardCodes }` so the UI can update its hand.
4. **Given** a participant `SUBSCRIBE`s to `/topic/matches/{id}`, **When** the subscription is accepted, **Then** the server immediately sends a `MATCH_SNAPSHOT` message (single full game-state DTO) so the UI can hydrate without an extra REST call. The snapshot MUST omit per-player private data (hands, objective texts other than the recipient's own).
5. **Given** a participant `SUBSCRIBE`s to `/user/queue/state`, **When** the subscription is accepted, **Then** the server sends a `PRIVATE_SNAPSHOT` with the recipient's own objective text, hand of cards, and any pending private events.

---

### User Story 4 - Disconnect resilience (Priority: P3)

As a player whose connection dropped briefly, I need to reconnect within 30 seconds without losing my turn or the match's state.

**Why this priority**: P3 because the match server-side state lives in memory (per Spec 005's `MatchRegistry`) and survives client disconnects naturally; this story formalizes the timeout and reconnect UX, and locks in the analysis §8 decision 6 about turn-timeout.

**Independent Test**: Player A is connected; force-close their WebSocket; within 30s reopen with the same Firebase ID token; resubscribe; A receives a fresh `MATCH_SNAPSHOT` and any events emitted during the gap (replayed from a per-user buffer) and can continue their turn.

**Acceptance Scenarios**:

1. **Given** a participant whose WebSocket closes unexpectedly, **When** they reconnect within 30 seconds using the same Firebase UID, **Then** the session is re-established and a fresh `MATCH_SNAPSHOT` is delivered on `SUBSCRIBE`.
2. **Given** a per-user replay buffer of size N (default 50 events), **When** the participant reconnects, **Then** they receive any events emitted on `/topic/matches/{id}` and `/user/queue/*` during the gap, in original order, before any new events.
3. **Given** a participant is the current turn-holder and disconnects, **When** the disconnect exceeds the configurable turn-timeout (analysis §8 decision 6, default 90s) AND it remains their turn, **Then** the server auto-ends their turn by calling `endCurrentTurn` on their behalf and broadcasts `TURN_AUTO_ADVANCED`. The player can reconnect later and resume control on their next turn.
4. **Given** all participants disconnect simultaneously, **When** the JVM continues running, **Then** the match state is preserved in the `MatchRegistry` and any participant can reconnect and resume.
5. **Given** the JVM restarts while a match is in progress (per Spec 005 FR-020 / analysis §8 decision 7), **When** participants reconnect, **Then** they receive a `MATCH_ABANDONED` event explaining the state was lost and the match is marked `ABANDONED`.

---

### Edge Cases

- A participant subscribes to `/topic/matches/{id}` for a match that has already finished: the server sends a single `MATCH_FINISHED` snapshot and unsubscribes them.
- A participant opens two browser tabs: both authenticate, both receive their `/user/queue/*` messages (Spring's user-destination resolution delivers to all of a user's sessions). Public-topic messages are delivered once per session, which is the desired behavior.
- nginx in front of the app must forward WebSocket upgrade headers; per analysis §5.4, the current `default.conf` does NOT have `Upgrade` / `Connection` headers for `/api/` and would reject WebSocket. This spec MUST update `others/docker/nginx/default.conf` to add `proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade";` for `/ws`.
- A command arrives concurrently with another for the same match (two endpoints racing): the engine is not thread-safe; commands MUST be serialized per match. Implementation: a `synchronized` block keyed on the `ClassicGame` instance, or a `ReentrantLock` per match in `MatchRegistry`.
- The engine throws `GameRulesException` mid-broadcast: the broadcast MUST NOT fire (the action transactionally failed). Implementation: emit the Spring `ApplicationEvent` only after the engine call returns normally.
- A user is banned (account deleted via Spec 004 FR-017) mid-match: their seat is marked `ABANDONED` in `match_participants`; their WebSocket sessions are terminated; the engine continues with their seat skipped.
- The optimistic-concurrency mismatch (FR-019) is the *cheap* check; the expensive check (engine's own turn/phase validators) runs anyway. Both must align — if a command passes the optimistic check but the engine rejects, the response is still 400.
- Token revocation during a long-lived WebSocket session: the connection MUST be torn down within 5 minutes (next heartbeat). A `ScheduledExecutorService` periodically re-verifies sessions older than 5 min.
- A non-participant manages to send a `MESSAGE` frame (e.g. STOMP `SEND`): the channel interceptor (preSend on SEND) rejects with `ERROR`.

---

## Requirements *(mandatory)*

### Functional Requirements

**WebSocket plumbing**

- **FR-001**: The system MUST register a STOMP-over-WebSocket endpoint at `/ws` using `spring-boot-starter-websocket`. Broker prefix `/topic`, application prefix `/app` (unused by external clients in v1 — commands are REST), user destination prefix `/user`.
- **FR-002**: The system MUST install a `HandshakeInterceptor` that reads the Firebase ID token from the `token` query parameter (per analysis §8 decision 11), verifies it via the same code path as Spec 004's filter, and attaches a `Principal` (carrying `userId` and `firebaseUid`) to the WebSocket session.
- **FR-003**: The system MUST install a `ChannelInterceptor.preSend` that, on STOMP `SUBSCRIBE` and `SEND` frames, validates the user's authorization for the destination. For `/topic/matches/{matchId}`: must be a participant. For `/topic/rooms/{roomId}`: must be a member. For `/user/queue/*`: implicit (Spring routes to the principal).
- **FR-004**: Unauthorized subscribes MUST return a STOMP `ERROR` frame with PT-BR message and MUST NOT establish the subscription.

**Gameplay command endpoints (REST POST → engine + broadcast)**

- **FR-005**: The system MUST expose `POST /api/matches/{id}/add-troops` accepting `{ qtdTroops, tgtCountry, expectedTurnUserId, expectedPhase }`. Delegates to `ClassicGamePActions.addTroops(...)` (`ClassicGamePActions.java:67-80`).
- **FR-006**: The system MUST expose `POST /api/matches/{id}/add-continent-troops` accepting `{ qtdTroops, tgtCountry, expectedTurnUserId, expectedPhase }`. Delegates to `ClassicGamePActions.addContinentTroops(...)` (`:82-94`).
- **FR-007**: The system MUST expose `POST /api/matches/{id}/exchange-cards` accepting `{ countryCodes: int[], expectedTurnUserId, expectedPhase }`. Delegates to `ClassicGamePActions.exchangeCards(...)` (`:105-121`).
- **FR-008**: The system MUST expose `POST /api/matches/{id}/attack` accepting `{ srcCountryId, tgtCountryId, expectedTurnUserId, expectedPhase }`. Delegates to `ClassicGamePActions.attack(...)` (`:32-48`).
- **FR-009**: The system MUST expose `POST /api/matches/{id}/move-troops` accepting `{ srcCountryId, tgtCountryId, qtdTroops, expectedTurnUserId, expectedPhase }`. Delegates to the engine's move-troops method (added in Spec 002 if not present today — see analysis §6.1 noting that "engine doesn't have this method yet").
- **FR-010**: The system MUST expose `POST /api/matches/{id}/end-phase` accepting `{ expectedTurnUserId, expectedPhase }`. Routing: if `expectedPhase = ADD_TROOPS`, call `endCurrentTurnAddPhase` (`:55-58`); if `ATTACK`, call `endCurrentTurnAttackPhase` (`:50-53`); if `MOVE_TROOPS`, call `endCurrentTurn` (`:60-65`).
- **FR-011**: The system MUST expose `GET /api/matches/{id}` returning a public game-state snapshot (full board + all players' visible state, **excluding** other players' hands and objective texts).
- **FR-012**: All gameplay command endpoints MUST be rate-limited per Spec 003 FR-019 (30/min/(IP+UID)).

**Optimistic concurrency**

- **FR-013**: Every gameplay command body MUST carry `expectedTurnUserId` and `expectedPhase`. The controller MUST reject with 409 (`STALE_STATE`) if either does not match the current engine state, returning `{ currentTurnUserId, currentPhase }` for client resync.
- **FR-014**: After a successful engine mutation, the controller MUST update `Match.currentTurnUserId` and `Match.currentPhase` (Spec 005 FR-015) in the same transaction as the broadcast event emission.

**Per-match serialization**

- **FR-015**: Concurrent gameplay commands for the same `matchId` MUST execute serially. Implementation: a `ReentrantLock` per match (acquired in the controller, released in `finally`), or a single-threaded executor per match. Choose the simplest viable approach.

**Event broadcast model**

- **FR-016**: The system MUST define a sealed hierarchy of game events. Wire shape: `{ event: <type>, matchId, at, correlationId, payload }` where `<type>` ∈ { `MATCH_STARTED`, `TROOPS_ADDED`, `CARDS_EXCHANGED`, `ATTACK_RESULT`, `TROOPS_MOVED`, `PHASE_ENDED`, `TURN_CHANGED`, `TURN_AUTO_ADVANCED`, `MATCH_FINISHED`, `MATCH_ABANDONED`, `MATCH_SNAPSHOT` }.
- **FR-017**: Events MUST be broadcast via `SimpMessagingTemplate.convertAndSend("/topic/matches/" + matchId, event)` from the controller layer (NOT from inside `jwarsv-core` — Constitution Principle I).
- **FR-018**: Private events (`CARD_DRAWN`, `CARDS_DISCARDED`, `OBJECTIVE_REVEALED`, `PRIVATE_SNAPSHOT`) MUST go via `convertAndSendToUser(userId, "/queue/cards", event)` and MUST NOT appear on any public topic.
- **FR-019**: `correlationId` on broadcast events MUST equal the `X-Correlation-Id` of the originating HTTP command (Spec 003 FR-015), so a single user action and its resulting events share one ID across logs.
- **FR-020**: A `MATCH_SNAPSHOT` event MUST be sent automatically by the server immediately after a successful subscribe to `/topic/matches/{id}`, without requiring an extra REST call.

**Resolving engine push-hook TODOs**

- **FR-021**: The TODO at `ClassicGamePActions.java:79` (after `addTroops`) MUST be resolved by emitting a `TROOPS_ADDED` event from the corresponding controller wrapper. The comment in the engine source MAY remain or be removed — but no STOMP code shall be added inside `ClassicGamePActions`.
- **FR-022**: The TODO at `ClassicGamePActions.java:93` (after `addContinentTroops`) MUST be resolved similarly.
- **FR-023**: The TODO at `ClassicGamePActions.java:120` (after `exchangeCards`) MUST be resolved similarly.
- **FR-024**: The comment at `ClassicGame.java:139-140` ("send update to all players … let all players know its first player turn") MUST be resolved by Spec 005's `start-match` endpoint emitting `MATCH_STARTED` + the initial `MATCH_SNAPSHOT` (already required by Spec 005 FR-016 and reinforced here for completeness).

**Disconnect handling and turn timeout**

- **FR-025**: When a participant's WebSocket session closes (clean or unclean), the server MUST mark the session offline but NOT terminate the match. Match state is preserved in `MatchRegistry` per Spec 005 FR-018.
- **FR-026**: A per-user event-replay buffer of size 50 MUST be maintained, scoped to `(userId, matchId)`. On reconnect within 30s, the buffer is replayed before live events resume. Older events are dropped from the buffer.
- **FR-027**: When it is a participant's turn and they have been offline for ≥ 90 seconds (configurable via `app.realtime.turn-timeout-seconds`; default 90 per analysis §8 decision 6), a scheduled task MUST call the same code path as `endCurrentTurn` (advancing phases automatically as needed) and broadcast `TURN_AUTO_ADVANCED`.

**Token freshness on long-lived sessions**

- **FR-028**: A scheduled task MUST re-verify Firebase ID tokens on active WebSocket sessions every 5 minutes; sessions with revoked or expired tokens are closed with a STOMP `ERROR` and a `CONNECTION_TERMINATED` event written to the per-user replay buffer.

**nginx config**

- **FR-029**: The nginx configuration at `jwar-server/others/docker/nginx/default.conf` MUST be updated to forward WebSocket upgrade headers on `/ws` (per analysis §5.4 highlighting the current absence of `Upgrade` / `Connection` headers).

**Dependencies**

- **FR-030**: Add `spring-boot-starter-websocket` to `jwar-server/jwarsv-sboot/build.gradle` (per analysis §6.2 noting it is not currently on the classpath).

### Key Entities *(include if feature involves data)*

- **GameEvent** (sealed type hierarchy, sboot only): base envelope `{ event: string, matchId: UUID, at: ISO-8601, correlationId: string, payload: Object }`. Concrete subtypes match the FR-016 enum.
- **GameStateSnapshot**: DTO carrying the public board state: `{ matchId, status, currentTurnUserId, currentPhase, players: [{ userId, color, troopsAvailable, countryCount, eliminated }], countries: [{ id, ownerUserId, troops }], continents: [{ id, troopsAvailable }] }`. Lives in `jwarsv-sboot`.
- **PrivateGameStateSnapshot**: DTO carrying the recipient's private state: `{ matchId, myObjective: { text }, myCards: [{ countryCode, shape }] }`.
- **AddTroopsCommand**: `{ qtdTroops: int (1..N), tgtCountry: int, expectedTurnUserId: UUID, expectedPhase: "ADD_TROOPS" }`.
- **AttackCommand**: `{ srcCountryId: int, tgtCountryId: int, expectedTurnUserId: UUID, expectedPhase: "ATTACK" }`.
- **MoveTroopsCommand**: `{ srcCountryId: int, tgtCountryId: int, qtdTroops: int, expectedTurnUserId: UUID, expectedPhase: "MOVE_TROOPS" }`.
- **ExchangeCardsCommand**: `{ countryCodes: int[] (length 3..N), expectedTurnUserId: UUID, expectedPhase: "ADD_TROOPS" }`.
- **EndPhaseCommand**: `{ expectedTurnUserId: UUID, expectedPhase: "ADD_TROOPS" | "ATTACK" | "MOVE_TROOPS" }`.
- **PerUserReplayBuffer**: in-memory ring buffer keyed by `(userId, matchId)`, capacity 50. Not persisted.
- **MatchSessionRegistry**: in-memory bean tracking `userId → Set<WebSocketSessionId>` and `(userId, matchId) → lastSeenAt` for turn-timeout decisions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An event emitted by one player's command reaches every other subscribed participant in under 200ms p95, measured locally with 3–6 participants per match.
- **SC-002**: The initial `MATCH_SNAPSHOT` after subscribe arrives in under 500ms p95.
- **SC-003**: Concurrent commands for the same match never produce inconsistent state: a property-based test running 100 random command sequences against a single match shows the engine's invariants (sum of country troops + reserve = constant per player, etc.) hold throughout. Verified by an integration test using a real `ClassicGame` and a fake `SimpMessagingTemplate`.
- **SC-004**: A participant who disconnects for ≤ 30 seconds and reconnects sees zero lost events (replay buffer covers the gap). Verified by an integration test driving a closed-then-reopened STOMP session.
- **SC-005**: A non-participant cannot subscribe to `/topic/matches/{id}` under any conditions. Verified by integration tests covering: room non-member, finished-match non-winner, deleted-account user, expired-token user.
- **SC-006**: Engine push hooks at `ClassicGamePActions.java:79, :93, :120` and `ClassicGame.java:139-140` no longer carry the `TODO SPRINT2 - COMNS` comments (or, if comments remain, the corresponding controller-side event emission is verified by an integration test that drives the action and asserts the broadcast).
- **SC-007**: An ArchUnit test confirms no class under `br.com.bnuuy.jwar.core.**` imports `org.springframework.web.socket.**`, `org.springframework.messaging.**`, or `SimpMessagingTemplate`. Principle I.
- **SC-008**: Turn-auto-advance (FR-027) fires within `90s ± 2s` of a turn-holder disconnect for the configured timeout, measured by an integration test that mocks the clock.
- **SC-009**: nginx forwards a WebSocket upgrade through to the Spring app successfully on the local compose stack (`others/docker/compose.yaml` + the updated `default.conf` from FR-029).

## Assumptions

- The browser cannot set custom headers on a WebSocket handshake, so the Firebase ID token is passed as a query parameter `?token=` — per analysis §8 decision 11. TLS termination at nginx (per analysis §5.4) keeps this opaque to network observers. The token is verified server-side identically to Spec 004's REST flow.
- STOMP-over-WebSocket with Spring's in-memory simple broker is the chosen transport (analysis §7.2). No Redis-backed external broker is added in v1. Horizontal scaling beyond a single JVM is explicitly out of scope.
- Commands are REST POSTs, not STOMP `SEND` frames. This is intentional: it preserves HTTP's idempotency / status-code semantics and lets us reuse Spec 003's validation, error envelopes, and rate limiting without duplicating them on the messaging side. STOMP carries only the server-to-client push.
- `Match.currentTurnUserId` is a UUID matching `User.id` (from Spec 004), not the engine's int player index. The mapping from `User.id` → `int playerIndex` (which the engine uses internally — see `ClassicGamePActions.attack(int srcPlayer, ...)` at `:32`) is performed by the controller, using the order in which members were added to the `ClassicGameLobby` during Spec 005's start flow.
- Per-match serialization (FR-015) uses a lock acquired by the controller around the entire engine call + event emission, so the engine's non-thread-safety (it was designed for single-threaded test driving) does not become a defect. This is acceptable for v1's expected load (analysis §7.3).
- The engine event-emission strategy uses Spring's `ApplicationEventPublisher` *only optionally*; the simplest viable path is for the controller method, after the engine call returns successfully, to directly call `SimpMessagingTemplate.convertAndSend(...)`. No engine code is modified to emit Spring events. This keeps Principle I clean.
- The "snapshot on subscribe" behavior (FR-020) is implemented by a `@SubscribeMapping` or a `ChannelInterceptor.postSend` hook that detects the `SUBSCRIBE` frame and dispatches the snapshot before any other server-to-client message.
- Card secrecy (US3) follows the Manual de Regras §9: "as cartas devem permanecer ocultas aos demais jogadores até que sejam trocadas." Public events about card actions carry only effects (troops gained), never card identities.
- The "move-troops" engine method (FR-009) is expected to exist by the time this spec is implemented (Spec 002 is the natural home for that engine work). If it does not yet exist, the controller throws `501 NOT_IMPLEMENTED` (via Spec 003's handler with `error.code = "NOT_IMPLEMENTED"`) until Spec 002 lands.
- Disconnect resilience (US4) is best-effort. The replay buffer is in-memory and bound to the JVM; a JVM restart drops it and the match is `ABANDONED` per Spec 005 FR-020.
- Per analysis §6.2, the existing Undertow embedded server (`build.gradle:30`) supports WebSocket natively; no additional native dependency is required.
- The "engine push hook TODOs" referenced at `ClassicGamePActions.java:79, :93, :120` and `ClassicGame.java:139-140` are documentation-level reminders to the original author; this spec resolves them in the sboot controllers without modifying core. The comments may be removed once their corresponding event emission is wired and tested.
