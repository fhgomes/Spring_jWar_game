# Feature Specification: UI Foundation

**Feature Branch**: `007-ui-foundation`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Set up the frontend workspace (React 18 + TypeScript 5 + Vite 5 + Tailwind 3) that compiles into `jwar-server/jwarsv-sboot/src/main/resources/static/` and is served by Spring in production; provide routing, theming, design-system primitives, i18n, HTTP client, and global state stores."

## User Scenarios & Testing *(mandatory)*

The jWar frontend is a Single Page Application that presents the board, lobby,
and authentication flows for the digital "War" game. This feature delivers the
foundational scaffolding every subsequent UI feature (008, 009, 010) relies on:
the build pipeline, routing skeleton, theme tokens, reusable primitives,
internationalization, the authenticated HTTP client, and the global state
stores. UI copy is rendered in Brazilian Portuguese per Constitution
Principle III (Domain Fidelity).

### User Story 1 - Bootable workspace with routing skeleton (Priority: P1)

A developer clones the repository, installs frontend dependencies, runs the
dev server, and lands on a routed shell that renders an authenticated layout
for protected paths and a public layout for `/login` and `/signup`. The build
output is produced into the Spring static folder so the same SPA is served
by Spring Boot in production.

**Why this priority**: Without a working workspace and route shell, no other
UI feature can be developed or demoed. This story is the MVP slice for the
entire frontend.

**Independent Test**: From a fresh checkout, run `npm install` then
`npm run dev` inside `frontend/`. A browser at `http://localhost:5173`
renders the public home route. Navigating to `/lobby` while unauthenticated
redirects to `/login`. Then `npm run build` writes assets into
`jwar-server/jwarsv-sboot/src/main/resources/static/`, and running the
Spring app serves the same SPA at `http://localhost:8080/`.

**Acceptance Scenarios**:

1. **Given** a fresh clone with Node 20 installed, **When** the developer runs
   `npm install` then `npm run dev` inside `frontend/`, **Then** Vite starts
   in under five seconds and the home route renders with no console errors.
2. **Given** an unauthenticated user, **When** they navigate to `/lobby`,
   `/rooms/abc`, or `/matches/abc`, **Then** the router redirects to
   `/login` and preserves the originally requested path in a query
   parameter so the user can return after signing in.
3. **Given** an authenticated user, **When** they navigate to `/login` or
   `/signup`, **Then** the router redirects to `/lobby`.
4. **Given** the production build was run with `npm run build`, **When**
   Spring Boot is started, **Then** `GET /` returns `index.html` and any
   deep link like `/rooms/xyz` is served by a fallback controller that
   also returns `index.html` (so the SPA can take over client-side
   routing), while `/api/**` and `/ws/**` paths remain handled by their
   own controllers.

---

### User Story 2 - Tailwind theme & design-system primitives (Priority: P1)

A UI engineer building a new screen imports primitive components
(`<Button>`, `<Input>`, `<Modal>`, `<Toast>`, `<Card>`, `<Container>`,
`<Label>`) and consumes design tokens (the six WAR army colors plus a dark
board-table base palette) to compose a screen consistent with the rest of
the app. All primitives are keyboard-navigable and screen-reader accessible.

**Why this priority**: Every other UI feature reuses these primitives.
Locking down the visual language and accessibility contract up front
prevents drift and rework.

**Independent Test**: A Storybook-equivalent demo route (`/__/playground`,
dev-only) renders one of each primitive in each variant. Manual keyboard
traversal (Tab, Shift+Tab, Esc, Enter) operates focus rings and dismisses
modals; axe-core reports zero serious or critical violations on the
playground page.

**Acceptance Scenarios**:

1. **Given** a developer opens the dev playground, **When** they tab through
   primitives, **Then** a visible focus ring appears on every interactive
   element and tab order matches DOM order.
2. **Given** a `<Modal>` is open, **When** the user presses `Esc` or clicks
   the backdrop, **Then** the modal closes and focus returns to the trigger
   element.
3. **Given** `<Button variant="danger">` is used, **When** rendered, **Then**
   it uses the `army.red` token for background and meets WCAG AA contrast
   against `army.white` text (≥ 4.5:1).
4. **Given** dark mode is the default theme, **When** any primitive is
   rendered without overrides, **Then** background is a desaturated dark
   green-brown (board-table aesthetic), text is `army.white`, and there
   is no flash of light theme on first paint.

---

### User Story 3 - i18n, HTTP client, and global state (Priority: P2)

The application boots with `pt-BR` as the only locale; all UI strings are
keyed through `react-i18next` so future locales can be added without code
changes. Every backend call goes through a single `axios` instance that
injects the Firebase ID token, translates the backend error envelope into
a typed `ApiError`, and surfaces user-facing failures via the toast store.
Global client state (auth session, toasts) lives in two small Zustand stores
distinct from the TanStack Query cache, which owns server state.

**Why this priority**: Authenticated, error-aware data flow is required for
every page beyond the static landing, but the visual primitives can be
implemented first.

**Independent Test**: A developer renders a temporary debug page that
(a) shows the current `useAuthStore` state, (b) calls `apiClient.get('/me')`
and asserts the `Authorization: Bearer <idToken>` header is present, and
(c) triggers a fake 401 from a mocked endpoint and observes a toast appear
with the localized PT-BR message "Sua sessão expirou. Entre novamente."

**Acceptance Scenarios**:

1. **Given** the user is signed in, **When** any HTTP call is made through
   `apiClient`, **Then** the request includes
   `Authorization: Bearer <firebase-id-token>` and a `X-Client-Version`
   header carrying the package.json version.
2. **Given** the backend returns the standard error envelope
   `{ "code": "AUTH_EXPIRED", "message": "...", "details": {...} }`,
   **When** the response interceptor processes it, **Then** the call
   rejects with a typed `ApiError` instance exposing `code`, `message`,
   `details`, and `httpStatus`.
3. **Given** the user is on the running dev server, **When** the code calls
   `apiClient.get('/health')`, **Then** Vite's proxy forwards the call to
   `http://localhost:8080/api/health` without CORS configuration.
4. **Given** any component calls `useToastStore().push({...})`, **When**
   the toast renders, **Then** the message is wrapped by a `<Toast>`
   primitive that auto-dismisses after 5 seconds and is announced through
   an ARIA live region (`role="status"`).

---

### User Story 4 - Single-command production deploy via Spring (Priority: P3)

A release engineer runs the existing Gradle build, which triggers the
frontend `npm run build`, copies the output into the Spring static folder,
and produces a fat jar that contains both the API and the SPA.

**Why this priority**: This is a deployment convenience; in early
development engineers can run Vite and Spring separately. Useful once the
app is demoed end-to-end.

**Independent Test**: From `jwar-server/`, run `./gradlew bootJar`. The
resulting jar, when started, serves the SPA at `/` and the API under
`/api/**`. Disconnecting Vite is not required.

**Acceptance Scenarios**:

1. **Given** the Gradle build runs the `npmBuild` task before
   `processResources`, **When** the build completes, **Then** the static
   folder contains `index.html` and a fingerprinted `assets/` directory.
2. **Given** the SPA is served by Spring, **When** the user hard-refreshes
   on `/matches/abc`, **Then** Spring returns `index.html` (HTTP 200) and
   the SPA reconciles the URL on client mount instead of returning HTTP 404.

---

### Edge Cases

- **Stale ID token**: when Firebase reports the token expired mid-session,
  the response interceptor MUST attempt a single silent refresh via
  `currentUser.getIdToken(true)` before rejecting; if refresh fails the
  user is signed out and redirected to `/login` with a toast in PT-BR
  ("Sua sessão expirou. Entre novamente.").
- **Offline / network error**: any `axios` error without an HTTP response
  surfaces as `ApiError { code: "NETWORK_ERROR" }` and a toast
  ("Sem conexão com o servidor.").
- **404 deep link inside SPA**: the SPA renders a PT-BR "Página não
  encontrada" view with a link back to `/lobby` rather than crashing.
- **Browser without `IndexedDB`** (private mode, some embedded webviews):
  auth falls back to in-memory persistence and the user is warned via a
  banner that sessions will not survive a refresh.
- **Slow network on first paint**: the route shell renders a full-screen
  spinner with the WAR logo while the auth store hydrates; the spinner
  is removed only after `onAuthStateChanged` has fired at least once.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST contain a top-level `frontend/` directory
  with a Vite 5 + React 18 + TypeScript 5 setup using strict TypeScript
  compiler options (`"strict": true`).
- **FR-002**: The build output directory MUST be configured to
  `../jwar-server/jwarsv-sboot/src/main/resources/static/` so the
  Spring Boot jar serves the SPA without extra deployment steps.
- **FR-003**: The frontend MUST use **npm** as its package manager (a
  single `package-lock.json` checked into the repository). Yarn and
  pnpm lockfiles MUST NOT be committed.
- **FR-004**: TypeScript path alias `@/*` MUST resolve to `src/*` in both
  `tsconfig.json` and Vite's `resolve.alias`.
- **FR-005**: ESLint (with `@typescript-eslint`, `react`, `react-hooks`,
  `jsx-a11y`) and Prettier MUST be configured; `npm run lint` MUST exit
  with code zero on the bootstrapped project.
- **FR-006**: The router MUST be React Router 6 (data routers) with at
  least these routes: `/`, `/login`, `/signup`, `/lobby`,
  `/rooms/:roomId`, `/matches/:matchId`, plus a `*` not-found route.
- **FR-007**: A `<ProtectedRoute>` wrapper MUST redirect unauthenticated
  users to `/login?next=<original-path>`. The login page MUST honor the
  `next` param after a successful sign-in.
- **FR-008**: A `<PublicOnlyRoute>` wrapper MUST redirect authenticated
  users away from `/login` and `/signup` to `/lobby`.
- **FR-009**: Tailwind MUST be configured with a `dark` class strategy
  applied to `<html>` by default. The theme MUST extend the palette with
  the six army colors keyed as `army.red`, `army.blue`, `army.green`,
  `army.yellow`, `army.black`, `army.white` (Manual §2.1 — six army
  color sets) and a `table` palette for the board-table dark base.
- **FR-010**: Tailwind MUST expose responsive breakpoints `sm: 640px`,
  `md: 768px`, `lg: 1024px`, `xl: 1280px`, `2xl: 1536px`.
- **FR-011**: The design system MUST provide the following primitives,
  all under `src/components/ui/`: `<Container>`, `<Card>`, `<Button>`
  (variants: `primary`, `secondary`, `danger`, `ghost`; sizes: `sm`,
  `md`, `lg`), `<Input>`, `<Label>`, `<Modal>`, `<Toast>`.
- **FR-012**: Every primitive MUST: (a) forward refs, (b) accept native
  DOM props for its underlying element, (c) render a visible focus ring
  (`focus-visible:ring-2`), (d) support a disabled state with reduced
  opacity and `aria-disabled="true"`, (e) pass axe-core "serious" and
  "critical" rules.
- **FR-013**: `<Modal>` MUST be implemented with Headless UI's `Dialog`
  (or Radix `Dialog`) so focus trap, scroll lock, and `Esc`-to-close
  are handled correctly out of the box.
- **FR-014**: `<Toast>` MUST be implemented with Radix `Toast` primitives
  and rendered via a singleton `<ToastProvider>` at the app root.
- **FR-015**: `react-i18next` MUST be configured with `pt-BR` as default
  and fallback locale. Translations live under
  `src/locales/pt-BR/<namespace>.json`. The codebase MUST NOT contain
  hard-coded user-facing strings outside translation files (lint rule
  `react/jsx-no-literals` MAY be relaxed for non-PT alphanumeric tokens
  like brand names).
- **FR-016**: An `apiClient` (axios instance) MUST be exported from
  `src/lib/http.ts` with `baseURL = "/api"`, a 15-second timeout, and
  a request interceptor that calls
  `useAuthStore.getState().getIdToken()` and sets
  `Authorization: Bearer <token>` when a session exists.
- **FR-017**: The response interceptor MUST convert non-2xx responses
  whose body matches the backend error envelope into a typed `ApiError`
  with fields `code: string`, `message: string`, `details: unknown`,
  `httpStatus: number`. Network errors (no `response`) become
  `ApiError { code: "NETWORK_ERROR", httpStatus: 0 }`.
- **FR-018**: TanStack Query 5 MUST be the only mechanism for server-state
  fetching/caching. A single `<QueryClientProvider>` lives at the app
  root with `staleTime: 30s` default, retries set to `1` for queries and
  `0` for mutations.
- **FR-019**: Two Zustand stores MUST exist: `useAuthStore`
  (fields: `currentUser`, `idToken`, `status: "loading"|"signed-in"|
  "signed-out"`, actions: `signIn`, `signOut`, `setUser`, `getIdToken`)
  and `useToastStore` (fields: `toasts: ToastMessage[]`, actions:
  `push`, `dismiss`). Domain state (rooms, matches, board) MUST NOT
  live in Zustand.
- **FR-020**: In development, Vite MUST proxy `/api` and `/ws` to
  `http://localhost:8080`. The proxy MUST set `changeOrigin: true` and
  `ws: true` for WebSocket upgrades.
- **FR-021**: In production, a Spring `WebMvcConfigurer` (or a single
  controller method) MUST forward any GET request to a path that does
  NOT start with `/api/`, `/ws/`, `/actuator/`, or `/favicon` and does
  not contain a `.` (file extension) to `/index.html`, returning
  HTTP 200.
- **FR-022**: The default UI locale MUST be `pt-BR`. All visible strings
  MUST be authored in Brazilian Portuguese (e.g., "Entrar", "Sair",
  "Criar conta", "Sala", "Partida", "Reforço", "Ataque", "Movimento").
- **FR-023**: A `Splash` component MUST be displayed while
  `useAuthStore.status === "loading"`. It MUST occupy the full viewport
  and display the WAR wordmark plus a loading indicator.
- **FR-024**: Lighthouse (mobile preset) Performance, Accessibility, and
  Best Practices scores MUST each be ≥ 90 on the home and login routes
  in a production build.
- **FR-025**: The Vite config MUST emit fingerprinted asset filenames
  (default Vite behavior) and a `manifest.json` (`build.manifest: true`)
  so Spring can optionally serve cache-busted assets with long-lived
  `Cache-Control` headers.

### Key Entities *(frontend types)*

- **User**: authenticated session user. Attributes: `uid`,
  `displayName`, `email`, `photoUrl`, `emailVerified`. Mirrors the
  Firebase user augmented by the backend `/api/me` profile.
- **ApiError**: typed error from the backend envelope. Attributes:
  `code`, `message`, `details`, `httpStatus`. Thrown from interceptors;
  surfaced through toasts or local form errors.
- **ToastMessage**: ephemeral user notification. Attributes: `id`,
  `variant: "info"|"success"|"warning"|"error"`, `title`, `description`,
  `durationMs`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cold `npm run dev` start time on a fresh clone is under
  **5 seconds** (Vite ready event) on a developer laptop (M-class or
  equivalent x86 with NVMe).
- **SC-002**: Production `npm run build` completes in under **60 seconds**
  on the same hardware.
- **SC-003**: Total JavaScript transferred for the `/login` route on a
  cold cache is under **200 KB** gzipped (excluding Firebase chunks,
  which are loaded lazily on first auth interaction).
- **SC-004**: Lighthouse mobile scores on `/` and `/login` in the
  production build are ≥ **90** for Performance, Accessibility, and
  Best Practices.
- **SC-005**: A keyboard-only user can navigate to any of the seven
  declared routes, open and close any primitive `<Modal>`, and dismiss
  any `<Toast>` without using a pointer device.
- **SC-006**: 100% of user-facing strings rendered by the foundation
  primitives and shell components come from `pt-BR` translation files
  (verified by a lint rule or a runtime audit script).

## Assumptions

- Node.js 20 LTS is the supported development runtime; older Node
  versions are not tested.
- npm 10 is used as the package manager; the lockfile is npm-flavored.
- Firebase Web SDK v10 is the auth provider; configuration values are
  read from `import.meta.env.VITE_FIREBASE_*` variables at build time.
- The backend already exposes (or will expose under specs 003–006)
  endpoints under `/api/**` with the documented error envelope and
  WebSocket endpoint under `/ws`.
- The board-game UI is desktop-first but the foundation primitives
  must already be responsive down to a 360 px viewport.
- Storybook is intentionally out of scope for v1; a single in-repo
  `/__/playground` route fills the same demo role with less tooling
  overhead (Constitution Principle V — Simplicity & YAGNI).
- Service Workers / offline support are out of scope for v1; the app
  is online-only.
- Telemetry beyond Sentry (already configured server-side) is out of
  scope for v1.
