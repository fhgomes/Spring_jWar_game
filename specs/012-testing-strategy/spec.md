# Feature Specification: Comprehensive Testing Strategy (Unit, Integration, E2E, Migration)

**Feature Branch**: `012-testing-strategy`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Comprehensive test coverage — Playwright E2E for user journeys, Spring @SpringBootTest for REST + WS contract integration, JUnit unit tests for game logic, Flyway migration verification, CI wiring."

> **Context sources**
>
> - Analysis: `docs/analysis/03-infra-auth-rest-state.md` §3 (zero test
>   infrastructure under `jwarsv-sboot/src/test/` today, only the
>   pure-JUnit tests in `jwarsv-core/src/test/java/.../core/game/`
>   exist); §4 (the dead `User.java`); §7.3 (in-memory match state with
>   Postgres metadata persistence); §8 Q12 (Testcontainers Postgres for
>   the single full-context smoke test, H2 in memory for slice tests).
> - Analysis: `docs/analysis/01-core-purity-refactor.md` and
>   `docs/analysis/02-mechanics-gap-analysis.md` (the gaps spec 002 is
>   meant to close — each fix needs a unit test that fails before and
>   passes after).
> - Constitution: `.specify/memory/constitution.md` — Principle II
>   (Test-Driven Game Rules: every game rule MUST have unit tests),
>   Principle I (game logic isolation: tests must respect the
>   `jwarsv-core` / `jwarsv-sboot` split), Principle V (YAGNI: do not
>   over-engineer the test harness).
> - Adjacent specs (assumed): the Docker compose stack from spec 011
>   provides the deployable artifact the E2E tests run against; the
>   REST + WS surface from specs 003 / 005 / 006 is the API the
>   integration tests exercise.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Playwright E2E covers a full multiplayer match (Priority: P1)

A QA engineer (or CI) runs a Playwright suite against the Docker-compose
stack. Two test users register, join the same room, the host starts the
match, and both players play one full turn each (add troops, attack,
move, end turn). The suite asserts the UI reflects every state change.

**Why this priority**: The single golden-path test is the highest-value
acceptance signal: it proves the entire stack — frontend, REST,
WebSocket, persistence, game engine — works end-to-end. Without it,
nothing else in the testing strategy can claim correctness at the
product level. The existing test surface has zero E2E coverage
(`docs/analysis/03-infra-auth-rest-state.md` §3).

**Independent Test**: From a clean checkout, run
`cd e2e && npm ci && npx playwright test play-a-match.spec.ts`; the
test brings up its dependencies (compose stack or reuses a running
one), executes the full two-player flow, and exits zero with traces
and screenshots archived on failure.

**Acceptance Scenarios**:

1. **Given** an empty environment, **When** the Playwright global
   setup runs, **Then** it brings up the compose stack via
   `docker compose up -d` (or detects an existing healthy instance
   and reuses it) before the first test executes.
2. **Given** the `play-a-match.spec.ts` test starts, **When** it
   provisions two test users via the Firebase Auth Emulator,
   **Then** both users can log in through the UI and reach the
   lobby screen.
3. **Given** two users are in the same room, **When** the host
   clicks "Start match" in the UI, **Then** both browser sessions
   navigate to the game board and assert the initial board state
   (countries assigned, initial troops placed, current turn
   indicator visible).
4. **Given** the match is in progress, **When** each player plays
   one full turn (add troops, attack a neighbor, move troops, end
   turn), **Then** the opposing browser receives the state update
   via WebSocket and the UI assertions pass on both sides.
5. **Given** a test fails, **When** Playwright completes the run,
   **Then** a Playwright trace and screenshot for that test are
   stored under `e2e/test-results/` and uploaded as a CI artifact.

---

### User Story 2 - Spring integration tests cover REST + STOMP contracts (Priority: P1)

A developer runs Spring `@SpringBootTest` integration tests that boot
the full Spring context against a Testcontainers Postgres, exercise the
key REST endpoints, and validate the STOMP-over-WebSocket subscribe /
broadcast contract. Firebase is replaced with a mock at the bean level
so deterministic test tokens map to deterministic UIDs.

**Why this priority**: Integration tests catch wiring bugs (component
scan, security filter, message broker config) that unit tests cannot.
With the analysis confirming zero `@SpringBootTest` coverage today
(`docs/analysis/03-infra-auth-rest-state.md` §3), the foundational
contracts are uncovered. P1 because the REST + WS surface is the
entire externally observable behavior of the backend.

**Independent Test**: Run `./gradlew :jwarsv-sboot:test --tests
"*integration*"`; the integration tests boot a Testcontainers Postgres
and a full Spring context, run the suite, and exit zero.

**Acceptance Scenarios**:

1. **Given** an authenticated test client with a stubbed Firebase
   token, **When** it calls `POST /api/rooms`, **Then** the
   response status is 201, the body contains the new room id, and
   a row is persisted in the `rooms` table.
2. **Given** a room exists and a second test user has authenticated,
   **When** that user calls `POST /api/rooms/{id}/join`, **Then**
   the response status is 200 and a `room_memberships` row is
   present.
3. **Given** a room with the configured minimum number of players,
   **When** the host calls `POST /api/rooms/{id}/start`, **Then**
   the response status is 200 and an in-memory match is registered
   with the engine.
4. **Given** an authenticated test client, **When** it opens a STOMP
   session on `/ws`, subscribes to `/topic/rooms/{id}`, and another
   client joins the room, **Then** the subscribed client receives a
   "player joined" event within 5 seconds.
5. **Given** the `FirebaseAuth` bean is replaced with a Mockito
   mock or stub at test scope, **When** any integration test sends
   `Authorization: Bearer test-token-<uid>`, **Then** the filter
   resolves the UID deterministically without contacting Google.

---

### User Story 3 - JUnit unit tests cover every fixed game mechanic (Priority: P1)

For every game-rule fix or addition delivered in spec 002 (battle dice,
conquest territory transfer, move-troops phase, joker cards, forced
5-card exchange, end-game state, etc.), there is at least one JUnit
test that fails before the fix and passes after. Branch coverage on
the rule-bearing classes meets a measurable threshold.

**Why this priority**: Constitution Principle II explicitly mandates
that every game rule MUST have unit tests. The engine is the core
asset; regressions here are visible to every player.

**Independent Test**: Run `./gradlew :jwarsv-core:test`; every existing
and newly added unit test passes; the JaCoCo report shows branch
coverage at or above the configured threshold for the rule-bearing
classes.

**Acceptance Scenarios**:

1. **Given** the rule-bearing classes
   `ClassicGameAttacker`, `ClassicGameAttackResProcessor`,
   `ClassicGamePActions`, `ExchangeCardsEvaluator`, **When** the
   JUnit suite for `jwarsv-core` runs, **Then** each public method
   responsible for a game rule has at least one test exercising the
   happy path and at least one test exercising the rule-violation
   path (which must surface a `GameRulesException`).
2. **Given** any fix introduced under spec 002 (mechanics
   gap-fix), **When** that fix is reverted, **Then** at least one
   unit test fails — i.e. every fix has a regression guard.
3. **Given** the JaCoCo report is produced, **When** the build
   evaluates the coverage rule, **Then** branch coverage for the
   four listed rule-bearing classes is at or above 85%.
4. **Given** the engine emits a domain event for every rule
   transition (attack resolved, turn ended, match ended), **When**
   the unit tests assert on these events, **Then** the events
   carry the documented payload fields and no others.

---

### User Story 4 - Flyway migrations verified against a real Postgres (Priority: P2)

A dedicated migration test boots a Testcontainers Postgres, runs every
Flyway migration in order, and asserts the resulting schema state
(table names, primary keys, indexes, key columns). This catches the
classic "the migration file went in but Flyway can't find it" bug — the
analysis flagged exactly this risk with the misconfigured
`classpath:db.migration` path
(`docs/analysis/03-infra-auth-rest-state.md` §2.4).

**Why this priority**: Migrations are quiet failures: a typo can break
the whole startup chain in production without any unit test catching
it. P2 because spec 011 already fixes the path typo (FR-015 there)
and v1 has only a handful of migrations, so the blast radius is
small for now.

**Independent Test**: Run `./gradlew :jwarsv-sboot:test --tests
"*FlywayMigrationTest*"`; the test boots Postgres in a container,
runs Flyway, and asserts the expected schema.

**Acceptance Scenarios**:

1. **Given** the migration test under
   `jwarsv-sboot/src/test/java/.../migration/`, **When** the test
   runs, **Then** Flyway picks up every `V*__*.sql` file under
   `src/main/resources/db/migration/` (validating that the path
   typo is fixed and stays fixed).
2. **Given** Flyway completes, **When** the test queries the
   information schema, **Then** every expected table is present
   with the expected primary key and the expected indexed columns.
3. **Given** a migration file is renamed or moved out of the
   classpath, **When** the test re-runs, **Then** it fails with a
   clear message naming the missing migration version.

---

### User Story 5 - CI wiring runs the full pyramid on every PR (Priority: P2)

GitHub Actions runs unit tests, integration tests, the migration test,
and the Playwright E2E suite on every pull request. Coverage and
Playwright traces are uploaded as artifacts. A separate weekly
workflow runs the Pitest mutation tests against `jwarsv-core`.

**Why this priority**: Continuous signal is what keeps the test suite
honest; without CI, tests rot. P2 because the tests themselves
(US1–US4) have to exist before CI can run them, and the local
developer can run them by hand in the meantime.

**Independent Test**: Open a draft PR; the `ci.yml` workflow runs to
completion within the success-criteria time budget, posts a green
check, and the run page shows JaCoCo HTML + Playwright traces as
downloadable artifacts.

**Acceptance Scenarios**:

1. **Given** `.github/workflows/ci.yml` is in place, **When** a PR
   is opened against `main`, **Then** the workflow runs four
   sequential jobs: unit (`./gradlew test`), integration
   (Testcontainers Postgres), migration (Testcontainers Postgres),
   and E2E (Playwright against compose stack).
2. **Given** the unit job completes, **When** JaCoCo finishes,
   **Then** the HTML coverage report is uploaded as the
   `coverage` artifact.
3. **Given** any Playwright test fails, **When** the E2E job
   finishes, **Then** the corresponding trace bundle is uploaded
   as the `playwright-traces` artifact.
4. **Given** a separate workflow `.github/workflows/mutation.yml`,
   **When** the weekly schedule fires, **Then** Pitest runs
   against `jwarsv-core` and uploads the mutation-coverage HTML.
5. **Given** an optional Lighthouse CI job on the frontend bundle,
   **When** it runs on PR, **Then** it asserts the configured
   performance budget for the production build (P3 — may be
   deferred without blocking the rest of the workflow).

---

### Edge Cases

- What happens when the Firebase Auth Emulator is unavailable during
  E2E? The global setup fails fast with a clear error and the suite
  exits before tests run, so failures cannot be confused with real
  product regressions.
- What happens when Testcontainers cannot pull the Postgres image
  (no network, Docker socket missing)? The integration tests should
  be skipped with a clear `assumeTrue` message rather than fail
  silently.
- What happens when an E2E test leaves residual data in Postgres
  (e.g. a half-finished match)? The Playwright global teardown must
  reset the database or destroy the compose volume between full
  runs to keep tests independent.
- What happens when the in-memory match registry contains state
  from a previous integration test? Each integration test class
  must wipe the registry in `@BeforeEach` (or use a fresh Spring
  context via `@DirtiesContext`).
- What happens when a flaky test fails once then passes on retry?
  Flake rate must be tracked across CI runs and stay below the
  configured threshold (see SC-002).
- What happens when a developer adds a new game rule without a
  corresponding unit test? The constitution mandates a test
  (Principle II); CI enforces this by failing on coverage
  regressions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST create a new directory `e2e/` at the
  repository root containing `package.json`, `playwright.config.ts`,
  `tests/`, and a `fixtures/` directory for shared helpers.
- **FR-002**: The Playwright global setup MUST ensure a healthy
  compose stack is available before tests start: detect an existing
  `docker compose` instance with healthy `app` and `postgres`
  services and reuse it, otherwise launch one via
  `docker compose up -d` and tear it down on global teardown.
- **FR-003**: System MUST provide Playwright helpers including at
  least a `loginAs(testUser)` function that provisions a user via
  the Firebase Auth Emulator and signs in through the UI, plus page
  object models for Login, Lobby, Room, and GameBoard pages.
- **FR-004**: Playwright MUST be configured to record traces and
  screenshots on failure and store them under
  `e2e/test-results/` (uploaded as CI artifacts by FR-018).
- **FR-005**: System MUST provide an end-to-end specification
  `e2e/tests/play-a-match.spec.ts` that registers two test users,
  joins them to a single room, has the host start a match, and
  plays one full turn each (add troops → attack → move → end turn),
  asserting UI reflects every state change.
- **FR-006**: System MUST provide an auth-flow E2E specification
  `e2e/tests/auth.spec.ts` covering sign-up via email, login,
  logout, and a Google sign-in flow mocked through the Firebase
  Auth Emulator.
- **FR-007**: System MUST create a test source tree
  `jwarsv-sboot/src/test/java/.../integration/` containing Spring
  integration tests annotated with
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` and
  `@Testcontainers` for Postgres.
- **FR-008**: Integration tests MUST cover the room lifecycle
  REST surface at minimum: `POST /api/rooms`,
  `POST /api/rooms/{id}/join`, `POST /api/rooms/{id}/start`, plus
  the STOMP `/ws` handshake, `SUBSCRIBE` to
  `/topic/rooms/{id}`, and the receipt of an event when state
  changes.
- **FR-009**: Integration tests MUST replace the `FirebaseAuth`
  bean with a Mockito mock or a deterministic stub at test scope,
  documented in `docs/testing.md`. Test tokens of the form
  `test-token-<uid>` MUST resolve to UID `<uid>` without contacting
  Google.
- **FR-010**: System MUST add a Flyway migration verification test
  (`@SpringBootTest` or dedicated test with Flyway invoked directly)
  that boots a Testcontainers Postgres, runs every migration under
  `src/main/resources/db/migration/`, and asserts the resulting
  schema.
- **FR-011**: System MUST add JUnit unit tests for every game-rule
  fix or addition delivered under spec 002 (mechanics gap-fix).
  Each fix MUST have at least one test that fails before the fix
  and passes after — i.e. each test acts as a regression guard.
- **FR-012**: Branch coverage for `ClassicGameAttacker`,
  `ClassicGameAttackResProcessor`, `ClassicGamePActions`, and
  `ExchangeCardsEvaluator` MUST reach 85% or higher, enforced by
  a JaCoCo coverage-verification rule in the Gradle build.
- **FR-013**: System MUST add the Pitest Gradle plugin to
  `jwarsv-core`, configured with a baseline mutation threshold
  appropriate to the current state of the engine.
- **FR-014**: System MUST provide a GitHub Actions workflow at
  `.github/workflows/ci.yml` triggered on `pull_request` against
  `main` and on `push` to `main`, running the unit, integration,
  migration, and E2E jobs in that order.
- **FR-015**: System MUST provide a separate workflow at
  `.github/workflows/mutation.yml` scheduled weekly that runs
  Pitest against `jwarsv-core` and uploads the mutation report.
- **FR-016**: System SHOULD provide a Lighthouse CI workflow that
  runs the performance budget against the built frontend on PR.
  This workflow MUST NOT block merge if performance regresses; it
  reports only.
- **FR-017**: The CI workflow MUST publish JaCoCo coverage HTML as
  an artifact on every run.
- **FR-018**: The CI workflow MUST publish Playwright traces and
  screenshots as artifacts on every E2E job run, regardless of
  pass/fail outcome.
- **FR-019**: Test documentation MUST be added at `docs/testing.md`
  describing how to run each layer locally (unit, integration,
  migration, E2E), the Firebase mocking pattern, the
  Testcontainers prerequisites, and the Playwright trace-viewing
  workflow.
- **FR-020**: All test code MUST respect the constitution's module
  boundary: unit tests for `jwarsv-core` MUST NOT import Spring
  classes; Spring tests live only under `jwarsv-sboot/src/test/`.

### Key Entities

- **TestUser fixture**: A reusable record representing a
  deterministic test account. Attributes: `uid` (string), `email`,
  `password`, `displayName`. Used by both Playwright (`loginAs`)
  and Spring integration tests (token stub).
- **GameStateAssertion helper**: A test-scope utility that asserts
  the engine's `ClassicGame` state matches an expected snapshot
  (current turn, phase, troop counts on a given country, cards in
  hand). Available to both unit and integration tests.
- **FirebaseEmulatorClient**: A thin wrapper around the Firebase
  Auth Emulator REST API used by Playwright global setup to seed
  test users before each E2E run.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A full CI run (unit + integration + migration + E2E)
  completes in fewer than 12 minutes on a standard GitHub Actions
  runner.
- **SC-002**: The aggregate flake rate across all suites (a test
  failing then passing on retry without a code change) is below 1%
  over a rolling 30-day window of CI runs.
- **SC-003**: `jwarsv-core` line coverage is at or above 85% and
  branch coverage on the four rule-bearing classes
  (`ClassicGameAttacker`, `ClassicGameAttackResProcessor`,
  `ClassicGamePActions`, `ExchangeCardsEvaluator`) is at or above
  85%.
- **SC-004**: Every user story in spec 002 (Fix-and-complete game
  mechanics) has a corresponding unit test that fails when the
  fix is reverted and passes when the fix is applied.
- **SC-005**: The Playwright `play-a-match.spec.ts` test passes on
  every PR build within a 90-second wall-clock budget.
- **SC-006**: 100% of Spring integration tests that need a
  database use Testcontainers Postgres rather than H2, so the
  schema under test matches production engine behavior.
- **SC-007**: A developer following `docs/testing.md` can run each
  test layer locally in fewer than 5 shell commands per layer.

## Assumptions

- Spec 011 (Docker & compose) ships first; the E2E suite depends
  on the compose stack it produces.
- Specs 003, 005, and 006 (REST foundation, game rooms, realtime
  gameplay) provide the endpoints and STOMP topics the
  integration tests exercise.
- The Firebase Auth Emulator is available locally and in CI via
  the Firebase CLI; the Playwright setup launches it as part of
  global setup or expects an existing instance.
- Testcontainers can pull `postgres:16-alpine` in CI; the runner
  has Docker-in-Docker or a host Docker socket available.
- JUnit 5 + Mockito remain the unit-test stack
  (constitution Principle II); no migration to alternative
  frameworks is planned.
- Coverage thresholds (85% branch) are starting baselines and may
  be raised as the engine stabilizes.
- Pitest weekly mutation runs are advisory; they do not block PR
  merges.
- The Lighthouse CI job is best-effort: a performance regression
  reports but does not fail the build.
- The repository allows the GitHub Actions runners to use the
  Docker socket (required for Testcontainers and the E2E compose
  stack).
- All test text and identifiers are written in English (test code
  is developer-facing); only user-visible game text remains in
  Brazilian Portuguese per constitution Principle III.
