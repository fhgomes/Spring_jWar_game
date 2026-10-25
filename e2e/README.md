# jWar E2E (Playwright)

Browser-driven end-to-end tests that exercise the jWar stack as a real
user would: sign up, join a room, play a match. The suite runs against
the docker-compose stack at the repo root.

## Prerequisites

- Node 20+
- Docker (for the app under test)
- The jWar compose stack running:

  ```bash
  # From the repo root
  cp .env.example .env  # if you haven't already
  make up
  ```

- (Once the emulator wiring lands) The Firebase Auth Emulator running
  on `localhost:9099`.

## Install

```bash
cd e2e
npm ci
npx playwright install --with-deps
cp .env.example .env   # optional -- only needed to override defaults
```

## Run

```bash
npm test                   # all projects, headless
npm run test:chromium      # chromium only
npm run test:headed        # see the browser
npm run test:ui            # Playwright UI runner
npm run report             # open the last HTML report
```

The tests assume `BASE_URL=http://localhost:8080` (override via env).

## Test layout

```
e2e/
  playwright.config.ts     # config (projects, retries, traces)
  package.json
  tsconfig.json
  .env.example
  fixtures/
    users.ts               # TestUser type + Firebase emulator helpers
  pages/                   # Page Object Models
    LoginPage.ts
    LobbyPage.ts
    RoomPage.ts
    GameBoardPage.ts
  tests/
    auth.spec.ts           # golden-path sign-up / login / logout
    play-a-match.spec.ts   # two-player happy-path match
```

## Current status

Most tests are tagged `test.skip(...)` until:

1. The Firebase Auth Emulator is wired into `docker-compose.yml`
   (see `e2e/fixtures/users.ts` TODO).
2. The frontend exposes the `data-testid` attributes the Page Object
   Models reference (see `e2e/pages/*.ts`).
3. Spec 002 (game mechanics) is complete enough for the
   add -> attack -> move -> end-turn flow to succeed.

This scaffold establishes the structure so each piece can be unlocked
incrementally by removing its `test.skip` gate.

## Traces and screenshots

On failure, Playwright records a trace + screenshot in `test-results/`.
View a trace with:

```bash
npx playwright show-trace test-results/path/to/trace.zip
```

CI uploads `test-results/` and `playwright-report/` as artifacts on
every run (see `.github/workflows/ci.yml`).
