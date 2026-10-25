# Feature Specification: UI Auth Pages

**Feature Branch**: `008-ui-auth-pages`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "User-facing authentication pages — sign-up, login, OAuth callback handling, account settings — built with Firebase JS SDK and the backend REST bridge. All copy in Brazilian Portuguese; split-screen on desktop, single-column on mobile."

## User Scenarios & Testing *(mandatory)*

This feature delivers the player-facing entry to jWar: account creation,
login, Google sign-in, session persistence, logout, the account settings
page, and email verification. It builds on `007-ui-foundation`
(routes, primitives, `useAuthStore`, `apiClient`) and prepares the user
to enter the lobby (feature 009). Visual identity is a split-screen with
a tease of the WAR map on the left and the form card on the right; the
tagline "Conquiste o mundo. Cumpra seu objetivo. Vença a guerra." sets
the tone for new players.

### User Story 1 - Email/password sign-up and login (Priority: P1)

A new player visits `/signup`, fills in display name + email + password,
and lands in the lobby with a fully bootstrapped session. A returning
player visits `/login`, enters credentials, and is taken to the lobby
(or to the `next` path captured by the protected-route redirect).

**Why this priority**: Without sign-up/login, no other UI feature is
reachable. This is the minimum to onboard a brand-new user end to end.

**Independent Test**: With Firebase Auth emulator running, a fresh
browser visits `/signup`, completes the form, and is automatically
routed to `/lobby` with `useAuthStore.status === "signed-in"` and a
valid ID token attached to the next `GET /api/me` call. A second
visit, signed out and then `/login`'d with the same credentials,
reaches `/lobby` in the same way.

**Acceptance Scenarios**:

1. **Given** the user is on `/signup`, **When** they submit a valid
   form (`displayName ≥ 2`, valid email, password ≥ 8 chars), **Then**
   the form calls Firebase `createUserWithEmailAndPassword`, sets
   `displayName` via `updateProfile`, calls `GET /api/me` (which
   bootstraps the backend profile on first hit), and navigates to
   `/lobby`.
2. **Given** the user submits an email that already exists, **When**
   Firebase rejects with `auth/email-already-in-use`, **Then** the
   form shows an inline error under the email field ("Este email já
   está em uso") and a toast in PT-BR.
3. **Given** the user is on `/login`, **When** they submit valid
   credentials, **Then** they are redirected to `/lobby` or to the
   `next` query parameter if present (e.g., `/login?next=/rooms/abc`
   → `/rooms/abc`).
4. **Given** the user types an invalid password three times, **When**
   Firebase rejects with `auth/wrong-password`, **Then** a generic
   PT-BR error toast appears within 200 ms ("Email ou senha
   incorretos.") — the form does NOT reveal which field is wrong.
5. **Given** the user clicks "Esqueci minha senha" on `/login`,
   **When** they confirm the email in the dialog, **Then** the app
   calls Firebase `sendPasswordResetEmail` and shows a success toast
   ("Enviamos um email com instruções para redefinir sua senha.").

---

### User Story 2 - Google sign-in & session persistence (Priority: P1)

Both `/login` and `/signup` show a prominent "Entrar com Google" button.
Clicking it opens a Firebase popup; on success the user lands in the
lobby. A returning user who closes the tab and reopens the app is
auto-signed-in via persisted credentials.

**Why this priority**: Reduces friction for new players (no password
to invent) and is critical for retention because most users will not
log in twice without persistence.

**Independent Test**: With the Firebase Auth emulator configured for
the Google provider, click "Entrar com Google" on `/login`, complete
the popup, and assert the user lands in `/lobby`. Then close the tab,
reopen `http://localhost:5173/`, and assert that after a brief splash
the user is in `/lobby` with no further interaction.

**Acceptance Scenarios**:

1. **Given** the user clicks "Entrar com Google" on `/login`, **When**
   the Firebase popup completes successfully, **Then** the user lands
   in `/lobby`, `useAuthStore.currentUser.email` is set, and the
   backend `GET /api/me` is called once to bootstrap the profile.
2. **Given** the popup is closed by the user, **When** Firebase
   rejects with `auth/popup-closed-by-user`, **Then** no error toast
   is shown (intentional cancel), the form remains usable.
3. **Given** a third-party cookie or popup blocker prevents the popup,
   **When** Firebase rejects with `auth/popup-blocked`, **Then** the
   app falls back to `signInWithRedirect` and reconciles the result
   on the next page load.
4. **Given** an active session exists in IndexedDB, **When** the app
   boots, **Then** the splash spinner shows while
   `onAuthStateChanged` resolves, after which the user is placed in
   `/lobby` (or `/login` if signed out) within **1.5 seconds**.

---

### User Story 3 - Logout & account settings (Priority: P2)

A signed-in user opens the top-navigation menu, sees their avatar and
display name, and can sign out or open the account settings page.
On the settings page they edit display name, change photo URL, and
optionally delete their account behind a confirmation modal.

**Why this priority**: Required for compliance (delete account) and
basic usability (logout), but not blocking for first demo of the
game loop.

**Independent Test**: While signed in, click the avatar in the top-nav.
The menu opens (Headless UI primitives), with "Configurações" and
"Sair". Clicking "Sair" clears the auth store and redirects to
`/login`. Clicking "Configurações" navigates to `/me/settings` where
the user changes display name and persists via
`PATCH /api/me` (200 OK), and the avatar in the top-nav updates
without a page reload.

**Acceptance Scenarios**:

1. **Given** the user is signed in, **When** they open the top-nav
   menu and click "Sair", **Then** Firebase `signOut()` is called,
   `useAuthStore` is reset to `{ status: "signed-out" }`, the
   TanStack Query cache is cleared, and the router navigates to
   `/login`.
2. **Given** the user is on `/me/settings`, **When** they edit the
   display name and click "Salvar", **Then** the form calls
   `PATCH /api/me` with `{ displayName }`, on 200 OK both
   `useAuthStore.currentUser.displayName` and the TanStack Query
   `["me"]` cache are updated, and a success toast appears
   ("Perfil atualizado.").
3. **Given** the user clicks "Excluir minha conta", **When** the
   confirmation modal asks them to retype their email and they
   confirm, **Then** the app calls `DELETE /api/me`, signs the user
   out of Firebase, clears the auth store, and routes to a
   farewell page at `/goodbye` ("Sua conta foi excluída.").
4. **Given** the user attempts to delete the account without
   retyping the email, **When** they click "Confirmar exclusão",
   **Then** the button remains disabled and a helper text shows
   ("Digite seu email para confirmar.").

---

### User Story 4 - Email verification banner (Priority: P3)

When the current user's email is not verified, the app shows a
dismissible banner at the top of every authenticated route inviting
them to re-send the verification email.

**Why this priority**: Improves trust signals and reduces support
load, but the app remains fully functional even without email
verification.

**Independent Test**: Sign in as a user whose `emailVerified` is
false. A banner appears across the top of `/lobby`, `/rooms/...`,
and `/matches/...`. Clicking "Reenviar email de verificação" calls
Firebase `sendEmailVerification` and shows a success toast.
Reloading after the user clicks the email link causes the banner
to disappear because `currentUser.emailVerified` is now true.

**Acceptance Scenarios**:

1. **Given** `useAuthStore.currentUser.emailVerified === false`,
   **When** any authenticated route renders, **Then** a yellow
   banner appears at the top with text "Verifique seu email para
   garantir acesso completo." and two buttons: "Reenviar email" and
   "Dispensar".
2. **Given** the user clicks "Reenviar email", **When** Firebase
   resolves, **Then** the button becomes disabled for 60 seconds
   with a countdown ("Reenviar em 42s") to prevent spam.
3. **Given** the user clicks "Dispensar", **When** the banner is
   dismissed, **Then** it stays hidden for the rest of the
   browser session (in-memory) but reappears on a fresh page load
   while the email remains unverified.

---

### Edge Cases

- **Network failure during sign-up**: if Firebase succeeds but the
  follow-up `GET /api/me` fails (network or 5xx), the UI still
  treats the user as signed-in (Firebase is the source of truth)
  and shows a toast ("Não foi possível carregar seu perfil.
  Tente novamente.") with a retry button.
- **Race condition on tab reopen**: if `onAuthStateChanged` resolves
  before the router decides where to send the user, the redirect
  logic uses the latest store state instead of an in-flight
  reference. The splash spinner is the visual guard.
- **Disposable email addresses**: out of scope for v1 — Firebase's
  built-in validation is sufficient.
- **Multiple tabs**: signing out in one tab MUST sign out in every
  open tab. The `onAuthStateChanged` listener handles this
  automatically; the auth store reacts and any protected route
  redirects to `/login`.
- **Browser without third-party cookies**: Google popup may fail;
  the redirect fallback handles it.
- **Backend rejects the user** (e.g., banned in `/api/me`): the
  Firebase user is signed out and a toast shows the backend
  message in PT-BR.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `/signup` MUST render a form with fields
  `displayName`, `email`, `password`, `passwordConfirm`. All four
  fields are required.
- **FR-002**: Client-side validation MUST use **Zod** schemas:
  - `displayName`: min 2, max 40 characters.
  - `email`: valid email format (`z.string().email()`).
  - `password`: min 8 chars, at least one letter and one digit.
  - `passwordConfirm`: equals `password`.
- **FR-003**: Inline error messages MUST appear in PT-BR under
  each invalid field within 200 ms of blur. Form submission MUST be
  blocked until the form passes validation.
- **FR-004**: On sign-up success the app MUST call Firebase
  `createUserWithEmailAndPassword`, then `updateProfile` to set the
  display name, then `GET /api/me` to bootstrap the backend
  profile, then `sendEmailVerification`, then navigate to `/lobby`.
- **FR-005**: `/login` MUST render fields `email` and `password`,
  a "Entrar" submit button, an "Esqueci minha senha" link, and a
  "Não tem conta? Crie uma" link to `/signup`.
- **FR-006**: Both `/login` and `/signup` MUST display a primary
  "Entrar com Google" button styled with the Google G logo (per
  Google branding guidelines), placed **above** the email/password
  form, separated by a horizontal divider "ou".
- **FR-007**: Google sign-in MUST use Firebase `GoogleAuthProvider`
  via `signInWithPopup`. If `auth/popup-blocked` is raised, the
  app MUST fall back to `signInWithRedirect` and reconcile via
  `getRedirectResult` on next app boot.
- **FR-008**: The forgot-password flow MUST open a `<Modal>` with a
  pre-filled email field (taken from the login form if present),
  a "Enviar email" button, and a "Cancelar" button. On submit it
  calls Firebase `sendPasswordResetEmail`.
- **FR-009**: Session persistence MUST default to
  `browserLocalPersistence`. On app boot, the SPA MUST render the
  `Splash` component (from feature 007) until
  `onAuthStateChanged` has fired at least once.
- **FR-010**: After successful authentication (any method), the
  app MUST honor a `next` query parameter on `/login` and
  `/signup` if present, otherwise navigate to `/lobby`.
- **FR-011**: The top navigation bar MUST render only on
  authenticated routes. It MUST contain: WAR wordmark linked to
  `/lobby` (left), and an avatar menu (right) with options
  "Configurações" (links to `/me/settings`) and "Sair".
- **FR-012**: The avatar menu MUST be implemented with Headless UI
  `Menu`, supporting keyboard navigation (Arrow keys, Enter, Esc)
  and closing on outside click.
- **FR-013**: `/me/settings` MUST display the current display name
  (editable), photo URL (editable), email (read-only), email
  verified state (read-only badge), and account creation date
  (read-only). A "Salvar" button is enabled only when the form is
  dirty and valid.
- **FR-014**: Saving settings MUST call `PATCH /api/me` with the
  diff. The backend response MUST update both
  `useAuthStore.currentUser` and the TanStack Query `["me"]` cache.
- **FR-015**: The "Excluir minha conta" action MUST open a
  confirmation `<Modal>` whose primary button is `variant="danger"`
  and whose enable condition is "typed email matches current
  email". On confirm: call `DELETE /api/me`, then Firebase
  `currentUser.delete()` (which re-prompts for credentials if the
  session is old), then sign out, then navigate to `/goodbye`.
- **FR-016**: While the current user's `emailVerified === false`,
  every authenticated layout MUST render a yellow banner at the
  top with PT-BR copy "Verifique seu email para garantir acesso
  completo." The banner has two buttons: "Reenviar email" and
  "Dispensar".
- **FR-017**: Clicking "Reenviar email" MUST call Firebase
  `sendEmailVerification` and disable the button for 60 seconds
  with a visible countdown.
- **FR-018**: All auth pages MUST be **split-screen on desktop**
  (≥ `lg` breakpoint, 1024 px): left half shows a darkened map
  background image with the tagline "Conquiste o mundo. Cumpra
  seu objetivo. Vença a guerra." overlaid in white serif type;
  right half shows the form card centered vertically.
- **FR-019**: On mobile (`< md`, < 768 px), the layout MUST
  collapse to a single column: a small WAR logo on top, the form
  centered below, and no hero image to save bandwidth.
- **FR-020**: All form labels, placeholders, helper texts, error
  messages, and button labels MUST be authored in Brazilian
  Portuguese and routed through `react-i18next`. Examples:
  "Nome de exibição", "Email", "Senha", "Confirmar senha",
  "Entrar", "Criar conta", "Entrar com Google", "Esqueci minha
  senha", "Sair", "Configurações", "Salvar", "Cancelar",
  "Excluir minha conta", "Reenviar email de verificação".
- **FR-021**: Forms MUST be fully accessible: every input has an
  associated `<label>`, error messages are linked via
  `aria-describedby`, and the submit button is disabled with
  `aria-busy="true"` while in-flight.
- **FR-022**: Auth pages MUST NOT render the top navigation bar
  (the SPA's main chrome) — they have their own minimal layout.
- **FR-023**: A `<GoogleAuthFlow>` hook MUST encapsulate popup
  vs redirect, success handling, and error mapping. The components
  on `/login` and `/signup` MUST share the same hook implementation
  to ensure consistent behavior.
- **FR-024**: On Firebase error codes the UI MUST display these
  PT-BR messages (toast or inline):
  - `auth/email-already-in-use` → "Este email já está em uso."
  - `auth/invalid-email` → "Email inválido."
  - `auth/weak-password` → "A senha precisa ter pelo menos 8
    caracteres."
  - `auth/wrong-password` / `auth/user-not-found` →
    "Email ou senha incorretos."
  - `auth/too-many-requests` → "Muitas tentativas. Tente
    novamente em alguns minutos."
  - `auth/network-request-failed` → "Sem conexão. Verifique sua
    internet."

### Key Entities

- **AuthForm**: local form state for the sign-up and login forms.
  Attributes: `values: { email, password, displayName?,
  passwordConfirm? }`, `errors: Record<field, string>`,
  `status: "idle"|"submitting"|"error"`.
- **GoogleAuthFlow**: state for the Google popup/redirect lifecycle.
  Attributes: `status: "idle"|"popup-open"|"redirecting"|"error"`,
  `lastErrorCode?: string`.
- **AccountSettingsForm**: state for `/me/settings`. Attributes:
  `values: { displayName, photoUrl }`, `isDirty`, `isValid`,
  `status`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A brand-new visitor can sign up and reach `/lobby`
  in under **3 seconds** of wall-clock time from the submit click
  (measured against the Firebase Auth emulator on localhost).
- **SC-002**: A bad-credentials toast appears within **200 ms** of
  Firebase returning `auth/wrong-password`.
- **SC-003**: Cold reopen of a previously signed-in tab reaches
  `/lobby` (post-splash) in under **1.5 seconds** at the 95th
  percentile on a developer laptop.
- **SC-004**: Lighthouse Accessibility score on `/login` and
  `/signup` is ≥ **95**.
- **SC-005**: All visible strings on the auth screens are
  Brazilian Portuguese, verified by a snapshot test that asserts
  no English words leak through except brand names (Google,
  Firebase, WAR).
- **SC-006**: Forgot-password flow completes (modal → toast) in
  fewer than **3 clicks** from `/login`.
- **SC-007**: Account deletion completes within **2 seconds** of
  the confirm click and the user lands on `/goodbye`.

## Assumptions

- Feature 007 (UI foundation) is implemented: routes, `<Modal>`,
  `<Toast>`, `<Button>`, `<Input>`, `useAuthStore`, `apiClient`,
  and `react-i18next` setup are available.
- The backend exposes `GET /api/me`, `PATCH /api/me`, and
  `DELETE /api/me` (defined in spec 004); these accept and return
  the standard error envelope.
- Firebase Auth project is configured for Email/Password and
  Google providers; Web SDK config is supplied via
  `VITE_FIREBASE_*` environment variables.
- Social providers beyond Google (Apple, Facebook) are out of
  scope for v1.
- 2FA / MFA is out of scope for v1.
- Localized error messages live in
  `src/locales/pt-BR/auth.json`; future locales can override.
- The "Excluir minha conta" flow accepts that backend retention
  policies (audit logs, anonymization) are implemented separately
  by the backend team; the UI only triggers the deletion.
- The map background image used on auth pages is a darkened,
  desaturated render of the same map asset used in feature 010,
  documented in `frontend/src/assets/auth-hero.webp`.
