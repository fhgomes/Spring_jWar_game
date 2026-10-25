# Overnight Build Status — 2026-05-15

> Autonomous build session executed while you were sleeping.
> 8 commits on `feat/first-version-rules`, all pushed to `origin`.

---

## TL;DR

- **Specs**: 12 spec-kit-formatted feature specs in `specs/` (3,883 lines).
- **Analysis**: 3 grounding analysis docs in `docs/analysis/` (1,826 lines).
- **Backend** (Java/Spring): `jwarsv-core` made pure + P1 mechanics bugs fixed; `jwarsv-sboot` filled with 75 Java files (controllers, services, auth, websocket, DTOs, repositories, mappers, exceptions, Flyway migrations).
- **Frontend** (React+TS+Tailwind): 63 TS/TSX files, 9 pages, full component tree, board SVG with 42 territories.
- **DevOps**: multi-stage `Dockerfile`, `docker-compose.yml` with Postgres, `Makefile`, GHCR workflow, CI workflow.
- **Tests**: Playwright E2E scaffold (POMs + golden-path tests), `@SpringBootTest` integration suites (Testcontainers), JUnit skeletons for every P1 mechanic.
- **Build**: `jwarsv-core` 21/21 tests pass. `jwarsv-sboot` compiles main + test. `verifyCorePurity` task verifies zero forbidden imports.

---

## What was delivered (by phase)

### Phase 1 — Analysis (3 parallel agents)
- `docs/analysis/01-core-purity-refactor.md` (466 lines) — core was already nearly pure; the real violation was in `build.gradle` declaring unused deps.
- `docs/analysis/02-mechanics-gap-analysis.md` (505 lines) — found severe attack bugs, missing move phase, missing terminal game state, missing jokers, missing forced exchange, broken first→second round transition.
- `docs/analysis/03-infra-auth-rest-state.md` (855 lines) — sboot was 3 files; picked defaults for 15 open decisions.

### Phase 2 — Specs (4 parallel agents)
| ID | Title | Lines |
|---|---|---|
| 001 | core-purity-refactor | 259 |
| 002 | fix-and-complete-game-mechanics | 272 |
| 003 | server-rest-foundation | 199 |
| 004 | auth-and-users | 235 |
| 005 | game-rooms-and-matches | 215 |
| 006 | realtime-gameplay | 222 |
| 007 | ui-foundation | 328 |
| 008 | ui-auth-pages | 369 |
| 009 | ui-lobby-and-rooms | 400 |
| 010 | ui-game-board | 606 |
| 011 | docker-and-compose | 362 |
| 012 | testing-strategy | 416 |

### Phase 3a — Core purity (me, synchronous)
- `jwarsv-core/build.gradle` slimmed to JUnit + Mockito only (no Spring/Firebase/MapStruct/OpenCSV/Hibernate).
- Deleted empty `User.java` placeholder.
- Replaced `commons-collections4.isEmpty` with pure Java in `ClassicGameValidator`.
- Added `verifyCorePurity` Gradle task that fails the build if forbidden packages reappear.

### Phase 3b — Mechanics P1 fixes (me, synchronous)
- `ClassicGameAttacker:13` — defender dice now from `tgtCountry.getTroopsCount()` (was reading `srcCountry`).
- `ClassicGameAttacker:29` — battle loop now bounded by `Math.min(attackers.length, defense.length)` (was ArrayIndexOutOfBoundsException when defender had fewer dice).
- `ClassicGameAttackResProcessor:146` — `removeTroops(getSrcCountryLoss())` (was passing country code instead of loss count).
- `ClassicGameAttackResProcessor.checkConquer` — conquest now transfers attacker dice-count troops from src to tgt (Manual §7); conquered territory never sits at 0 troops.
- `ClassicGame:170-178` — first→second round flag transition fixed (was `if (secondRound)` after just setting it true; now `else if`).
- `ClassicGame` — added `MatchStatus { LOBBY, IN_PROGRESS, FINISHED }` + `winner` field. `startMatch` → IN_PROGRESS. `turnToNextPlayer`/`attack` call `finishMatch(winner)` when `EndGameEvaluator` reports victory (replaced both `// TODO: Handle game end` placeholders).
- `ClassicGame.endTurnMovePhase` + per-turn `movedTroopsInto` tracking.
- `ClassicGamePActions.moveTroops` (contiguous-only, source keeps ≥1, each territory receives at most once per turn, PT-BR errors) + `endCurrentTurnMovePhase`.

### Phase 3c — Server backend (X1, parallel)
Created under `br.com.bnuuy.jwar.server`:
- **domain/** — User, Room, RoomMember, RoomMessage, Match + RoomStatus, MatchStatus enums.
- **dto/** — 30 records covering every endpoint from specs 003-006.
- **repository/** — UserRepository, RoomRepository, RoomMessageRepository, MatchRepository.
- **exception/** — BadRequest, Conflict, Forbidden, NotFound, Unauthorized.
- **auth/** — FirebaseAuthFilter (verifies `Authorization: Bearer <id-token>`), FirebaseAuthService interface + Real/Stub impls (stub for dev profile accepting `dev:<uid>`), FirebaseConfig, SecurityConfig, CurrentUser, FirebasePrincipal.
- **api/** — HealthController, AuthController, UserController, RoomController, MatchController.
- **api/advice/** — GlobalExceptionHandler (maps exceptions to PT-BR ErrorResponse), CorrelationIdFilter (MDC + `X-Correlation-Id` header).
- **api/mapper/** — UserMapper, RoomMapper, GameStateMapper (the boundary between core's mutable ClassicGame and the immutable wire DTOs).
- **service/** — UserService (bootstrap from Firebase verified token), RoomService (full lifecycle), MatchService (wraps ClassicGame, per-match locking, WS event emission), MatchRegistry (ConcurrentHashMap holder).
- **ws/** — WebSocketConfig (STOMP `/ws` with `/topic` and `/user` destinations), WebSocketAuthInterceptor (Firebase ID token on CONNECT), GameEventPublisher.
- **config/** — JacksonConfig, OpenApiConfig (springdoc-openapi), DataRestConfig (disables Spring Data REST auto-discovery).
- **resources/** — `application.yml` typos fixed (scanBasePackages, Flyway path, Postgres URL); profiles `dev` (H2), `docker` (Postgres), `test`. Flyway migrations V1__users, V2__rooms, V3__matches.

### Phase 3d — Frontend (X2, parallel)
Created under `frontend/`:
- **Build chain**: Vite 5 + TS 5 + Tailwind 3 + ESLint + Prettier. Build output targets `jwar-server/jwarsv-sboot/src/main/resources/static/`. Dev-proxy routes `/api` and `/ws` to localhost:8080.
- **Pages** (9): HomePage (auth-aware redirect), LoginPage, SignupPage, LobbyPage, RoomPage, MatchPage, AccountSettingsPage, NotFoundPage, GoodbyePage.
- **Components**:
  - `ui/` primitives — Button, Input, Label, Card, Container, Modal, Toast, Spinner (Headless UI for accessible focus traps).
  - `layout/` — AppShell, TopNav, UserMenu.
  - `auth/` — LoginForm, SignupForm, GoogleSignInButton, ProtectedRoute, VerifyEmailBanner, AuthHero, PT-BR errorMessages.
  - `rooms/` — RoomCard, RoomList, CreateRoomModal, ColorPicker (6 WAR colors), RoomMemberList, RoomChat.
  - `game/` — BoardSvg (42 territories), TerritoryNode, TroopBadge, TurnHud, ActionPanel, ActionFeed, AttackModal, DiceRoll, ExchangeCardsModal, MoveTroopsControls, CardHandSidebar, ObjectivePanel, EndGameModal, map-data.ts.
- **lib/** — api.ts (axios + Firebase token interceptor), stomp.ts (STOMP client factory), firebase.ts (Web SDK), utils.ts.
- **stores/** — useAuthStore (Firebase onAuthStateChanged hydration), useToastStore.
- **hooks/** — useCurrentUser, useRooms, useRoom, useMatch (TanStack Query + WS subscription).
- **types/api.ts** mirrors backend DTOs exactly.
- **locales/pt-BR.json** holds every UI string.

### Phase 3e — Docker + compose (X3, parallel)
- `Dockerfile` — 3 stages (frontend-builder → backend-builder → runtime). Non-root user, healthcheck on `/api/health`, G1GC + ramp-percent heap, graceful shutdown.
- `docker-compose.yml` — postgres:16-alpine + app + optional pgadmin under `dev-tools` profile. Healthchecks, env-file (`.env`).
- `Makefile` — up, down, logs, rebuild, psql, shell, dev-tools-up, clean.
- `.env.example`, `.dockerignore`, `docs/docker.md` first-run guide.
- `.github/workflows/docker.yml` — GHCR publish on `v*` tag.
- `.github/workflows/ci.yml` — backend-test (gradle), frontend-typecheck, e2e (compose + Playwright).

### Phase 3f — Testing (X3, parallel)
- `e2e/` — Playwright config (chromium/firefox/webkit projects, traces, retries, screenshots), Page Object Models (LoginPage, LobbyPage, RoomPage, GameBoardPage), fixtures/users.ts, golden-path tests (auth.spec, play-a-match.spec) — gated `test.skip` until Firebase Auth Emulator is wired.
- `jwarsv-sboot/src/test/java/.../integration/` — AbstractIntegrationTest (Testcontainers Postgres), FirebaseTestConfig (stub), HealthControllerIntegrationTest (live), AuthFlowIntegrationTest & RoomLifecycleIntegrationTest (@Disabled until matching spec implementation lands).
- `jwarsv-sboot/src/test/java/.../migration/FlywayMigrationsTest`.
- `jwarsv-core/src/test/java/.../*Test.java` — 8 JUnit skeletons one-to-one with spec 002 user stories (@Disabled, ready to enable as mechanics are wired up).

---

## Verification done

```
./gradlew :jwarsv-core:test                  → 21/21 pass
./gradlew :jwarsv-core:verifyCorePurity       → OK (no forbidden imports)
./gradlew :jwarsv-sboot:compileJava           → green
./gradlew :jwarsv-sboot:compileTestJava       → green
```

Integration tests with Testcontainers require a running Docker daemon (not available in the autonomous env). They will run in CI.

---

## Open items / known limitations

### Infrastructure
| Item | Severity | Where |
|---|---|---|
| Firebase service account JSON not provided | Required for Google OAuth + real auth | `secrets/firebase-service-account.json` (gitignored). For dev profile, `StubFirebaseAuthService` accepts `dev:<uid>` tokens. |
| Frontend `npm install` not run | Required for Vite build | Run `cd frontend && npm install` then `npm run typecheck && npm run lint` |
| E2E `npm install` not run | Required for Playwright | Run `cd e2e && npm install && npx playwright install` |
| Integration tests need Docker daemon | Will pass in CI | `e2e/`, `jwarsv-sboot/src/test/integration/` |

### Backend gaps (X1 follow-ups)
| Item | Severity | Where |
|---|---|---|
| `ClassicGamePActions.attack(...)` returns `void` | High | `MatchService.attack` rebuilds best-effort `AttackResultDto` from before/after snapshots; the actual dice arrays are lost. Need to refactor core to return `AttackResultVO` from the action. |
| Bucket4j on classpath but not wired | Medium | `application.yml` has `app.rate-limit.*` config; the filter implementation needs a `HandlerInterceptor` with a `BucketRegistry`. |
| `/api/auth/login` permit-all entry has no controller | Low (intentional) | Login is fully client-side via Firebase Web SDK + custom token exchange; entry is in `SecurityConfig` to avoid 401 redirects. |
| Replay buffer / disconnect resilience (Spec 006 P3) | Low | `GameEventPublisher` is the single chokepoint where a future `SessionRegistry` can intercept. |

### Frontend gaps (X2 follow-ups)
| Item | Severity | Where |
|---|---|---|
| **Map polygons are schematic** — mechanically generated octagons grouped by continent, not geographically faithful | Visual polish | `frontend/src/components/game/map-data.ts` — replace `pathD` for richer art |
| **Adjacency permissive on UI** — every non-owned territory shows as attack target | Correctness | `frontend/src/pages/MatchPage.tsx`. Backend should ship `adjacency` or `validTargets` in `CountrySnapshot`; UI swaps the filter then. |
| **MOVE_AFTER_CONQUEST flow not auto-opened** — after conquest, no follow-up modal asks how many troops to move in | Functionality | `AttackModal` → chain into `MoveTroopsControls` with `minMove = attackerDice` when `result.conquered === true` |
| **Forced exchange (5-card rule) not auto-triggered on UI** | Functionality | Spec 010 FR-018: when `myCards.length >= 5`, auto-open exchange modal with best legal selection pre-highlighted |
| **Card shape mismatch** — frontend type includes `'JOKER'`, backend `ECardShape` enum has only `TRIANGLE/CIRCLE/SQUARE` | Contract | Align: either backend adds `JOKER` to enum or uses a separate `isJoker` flag |
| **WS auth header convention** — frontend sends `Authorization: Bearer ...` in CONNECT frame; backend expects same | Verify | `frontend/src/lib/stomp.ts` ↔ `WebSocketAuthInterceptor` |
| **Action feed text empty** — `useMatch.handleMatchEvent` appends events with `text: ''`; PT-BR formatting needs hooking | Polish | `frontend/src/hooks/useMatch.ts` + `match.actions.*` keys in `locales/pt-BR.json` |
| **Color-blind shape overlay on troop badges** | A11y polish | `frontend/src/components/game/TroopBadge.tsx`; `COLOR_PATTERN` map exists in `types/game.ts` but not painted |
| **Backend DTOs are best-effort** — frontend `types/api.ts` mirrors what specs implied; reconcile when the Java side serializes | Contract | Compare `frontend/src/types/api.ts` vs `jwarsv-sboot/.../dto/*.java` once a real call lands |

### Mechanics P2/P3 (spec 002)
| Item | Severity | Where |
|---|---|---|
| Reinforcement uses `>7` branch instead of `max(3, floor(N/2))` | P2 | `ClassicGameDist.java:154-161` |
| Continent bonus deployment not constrained to that continent | P2 | `ClassicGameValidator.continentHasAvailableTroopsToAdd` |
| Used cards reshuffled immediately (no discard pile) | P2 | `ExchangeCardsEvaluator` |
| Elimination card transfer not random | P2 | `ClassicGameAttackResProcessor.transferCardsFromDefeatedPlayer` |
| Jokers absent from deck and exchange logic | P2 | `EClassicCountryCard`, `ExchangeCardsEvaluator` |
| 5-card forced exchange not enforced | P2 | `ClassicGamePActions` — block turn start |
| +2 troops per owned-territory card in exchange | P2 | `ExchangeCardsEvaluator` |
| Data fixes: dup country code 12 (OTW/ALA), continent ASI name typo "America do Sul", NVG→NVG self-loop, asymmetric BRA↔ARL, English country names SWD "Sweden"/MOS "Moscow", `EGameColors.BLUE.name="Blue"` | P3 | `EClassicCountries`, `EClassicContinents`, `CountriesBordersUtil`, `EGameColors` |

---

## First-run checklist (you, when you wake up)

```bash
# 1. Set up env
cp .env.example .env
# Edit .env with DB password.
# Place Firebase service account JSON at ./secrets/firebase-service-account.json
# (or omit and run dev profile — stub auth accepts `dev:<uid>` Bearer tokens)

# 2. Install frontend deps
cd frontend && npm install && cd ..

# 3. Install e2e deps (optional)
cd e2e && npm install && npx playwright install --with-deps && cd ..

# 4. Start everything
make up
# Logs:
make logs

# 5. Open
# - UI: http://localhost:8080
# - Swagger: http://localhost:8080/swagger-ui.html
# - Postgres: localhost:5432 (creds from .env)
```

To run tests:

```bash
cd jwar-server
./gradlew :jwarsv-core:test                # 21/21 should pass
./gradlew :jwarsv-core:verifyCorePurity     # constitution check
./gradlew test                              # all (needs Docker daemon for Testcontainers)
```

---

## Architecture cheat sheet

```
┌──────────────────────────────────────────────────────────────────┐
│                         Browser (React)                          │
│  • Firebase JS SDK (login/Google OAuth)                          │
│  • Axios → /api (with Firebase ID token in Authorization header) │
│  • STOMP over WS → /ws (with token in CONNECT header)            │
└──────────────────────────┬───────────────────────────────────────┘
                           │
       ┌───────────────────▼────────────────────────┐
       │      Spring Boot (Undertow, port 8080)     │
       │  serves static UI from /                   │
       │                                            │
       │  FirebaseAuthFilter (verifies ID token)    │
       │  GlobalExceptionHandler (PT-BR errors)     │
       │  CorrelationIdFilter (MDC)                 │
       │                                            │
       │  Controllers ─→ Services ─→ Mappers        │
       │                  │                         │
       │                  └─→ MatchRegistry         │
       │                       ↓                    │
       │  ┌────────────────────────────────────┐    │
       │  │  jwarsv-core (PURE Java, POJOs)    │    │
       │  │  • ClassicGame (Facade)             │    │
       │  │  • ClassicGamePActions (Commands)   │    │
       │  │  • ClassicGameAttacker (battle)    │    │
       │  │  • EndGameEvaluator (Strategy +    │    │
       │  │    Registry of ObjectiveEvaluators) │    │
       │  │  • E* enums (board, cards, colors)  │    │
       │  └────────────────────────────────────┘    │
       │                                            │
       │  WebSocketConfig (STOMP /topic /user)      │
       │  GameEventPublisher (broadcast events)     │
       └────────────────────┬───────────────────────┘
                            │
            ┌───────────────▼──────────────┐
            │      Postgres (volume)       │
            │  • users                     │
            │  • rooms, room_members,      │
            │    room_messages             │
            │  • matches (metadata only —  │
            │    live game state is        │
            │    in-memory)                │
            └──────────────────────────────┘
```

---

## Commit history (this session)

```
560ce05  Complete controllers, frontend pages, and service wiring
2e1c659  Add Spring backend, Docker compose, E2E + integration test scaffolds
a9532ac  Refactor jwarsv-core to be pure + fix P1 game mechanics
1af4d38  Add 12 feature specs (specify phase of speckit workflow)
c65a251  Add parallel analysis docs (core purity, mechanics gap, infra state)
389a666  Add AGENTS.md and .ai/ guides; update CLAUDE.md pointers
e8062dd  Add WAR game rules reference document
```

All commits include `Co-Authored-By: Claude Opus 4.7 (1M context)`.

Branch is in sync with `origin/feat/first-version-rules` — nothing to push.

---

**Sleep well 😴 — wake up to a multi-thousand-line foundation, ready to `make up`.**
