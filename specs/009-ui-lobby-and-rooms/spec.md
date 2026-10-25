# Feature Specification: UI Lobby & Rooms

**Feature Branch**: `009-ui-lobby-and-rooms`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Lobby (list of open rooms), room detail (waiting screen with member list, color picker, chat), and the room-creation modal. Real-time updates over STOMP/WebSocket. PT-BR copy. Connects players from sign-in to a match launch."

## User Scenarios & Testing *(mandatory)*

This feature is the matchmaking layer: it shows where games are happening,
lets a player join an open one or create their own, and walks the group
through the pre-match waiting screen where colors are chosen and chat
happens. The host launches the match when the room has at least 3
players who have chosen their colors (Manual §2.2 — minimum 3 players,
maximum 6 players). On launch, every member of the room is taken to
the gameplay screen (feature 010).

### User Story 1 - Browse and create rooms (Priority: P1)

A signed-in player lands on `/lobby`, sees a grid of currently open
rooms, and can either click "Entrar" on one or open a "Criar sala"
modal to start a new room and become its host. The list refreshes
automatically so new rooms appear without a manual reload.

**Why this priority**: Without this step, players cannot find each
other. This is the minimum viable matchmaking surface.

**Independent Test**: With the backend running and at least one
open room seeded, navigate from `/login` to `/lobby`. The room
card grid renders within 200 ms of the query resolving. Click
"Criar sala", fill the modal (name "Sala do Fernando", max 6, no
password), submit. The browser navigates to `/rooms/<id>` and the
player appears as the host on the member list.

**Acceptance Scenarios**:

1. **Given** the user is signed in and lands on `/lobby`, **When**
   `GET /api/rooms?status=open` resolves, **Then** the page renders
   a responsive grid of `<RoomCard>` components — three columns on
   desktop (≥ `lg`), two on tablet (≥ `md`), one on mobile.
2. **Given** the lobby has zero open rooms, **When** the query
   resolves with `[]`, **Then** an empty-state illustration with
   the PT-BR copy "Nenhuma sala aberta — crie a sua!" is shown
   with a primary "Criar sala" button.
3. **Given** the user clicks the floating "Criar sala" button
   (always visible in the top-right of the lobby), **When** the
   modal opens, **Then** focus is moved to the "Nome da sala"
   input and the body scroll is locked (Headless UI `Dialog`).
4. **Given** the form has a valid room name (3–40 chars) and a
   max-players value between 3 and 6, **When** the user clicks
   "Criar", **Then** `POST /api/rooms` is invoked and on 201
   Created the user navigates to `/rooms/<id>` and the new room
   appears in other connected clients' lobbies via WebSocket
   broadcast.
5. **Given** the user is in `/lobby`, **When** another client
   creates a room or starts a match, **Then** the lobby list
   updates within 500 ms — either by a fresh TanStack Query
   refetch (5-second polling) or by a STOMP message on
   `/topic/lobby` (preferred when available).
6. **Given** a room card shows `4/6 players`, **When** the user
   clicks "Entrar", **Then** the app calls
   `POST /api/rooms/{id}/join`, on success navigates to
   `/rooms/{id}`, and on `409 Conflict` (room full) shows a toast
   "Esta sala está cheia.".

---

### User Story 2 - Room detail with color picker & start match (Priority: P1)

Inside a room, every member sees a live list of who has joined,
their chosen color, the host badge, and a swatch picker that lets
the current user claim one of the still-available colors. The host
sees an additional "Iniciar partida" button that becomes enabled
once at least 3 members have all chosen distinct colors. When
clicked, every connected member is navigated to `/matches/{id}`.

**Why this priority**: This is the bridge between matchmaking and
gameplay. Without it the lobby is purely cosmetic.

**Independent Test**: Open three browser tabs signed in as three
different users. Tab A creates a room; Tab B and Tab C join the
same room from their lobbies. Each picks a different color. Tab A
(the host) sees "Iniciar partida" enabled. Clicking it routes
all three tabs to `/matches/<matchId>` within 500 ms.

**Acceptance Scenarios**:

1. **Given** the user navigates to `/rooms/{id}`, **When** the page
   mounts, **Then** it fetches `GET /api/rooms/{id}` once and
   subscribes to `/topic/rooms/{id}` over STOMP for live updates.
2. **Given** a member joins the room, **When** the server emits a
   `ROOM_MEMBER_JOINED` event, **Then** the member list updates in
   under 500 ms in every connected client; the join is announced
   via a non-intrusive toast ("Maria entrou na sala.").
3. **Given** the user clicks an unclaimed swatch in the color
   picker, **When** the request `PATCH /api/rooms/{id}/members/me`
   with `{ color }` succeeds, **Then** that swatch is shown with a
   selection ring on the user's row and disappears from the list
   of "available colors" for other members.
4. **Given** the room has 2 members with colors, **When** the host
   inspects the start button, **Then** it is rendered as disabled
   with a tooltip "Mínimo de 3 jogadores com cor escolhida
   (Manual §2.2)".
5. **Given** the room has 3 to 6 members all with distinct colors,
   **When** the host clicks "Iniciar partida", **Then** the app
   calls `POST /api/rooms/{id}/start`, the server emits a
   `MATCH_STARTED { matchId }` event, and every member of the
   room navigates to `/matches/{matchId}`.
6. **Given** the user is not the host, **When** they view the
   room, **Then** they MUST NOT see the "Iniciar partida" button;
   they MUST see a passive caption "Aguardando o anfitrião iniciar
   a partida.".

---

### User Story 3 - Leave room & chat (Priority: P2)

A room member can leave the room at any time via a secondary
"Sair da sala" button (with confirmation). The room also hosts
a live chat sidebar where members exchange messages while waiting.
Messages persist for the lifetime of the room and are delivered
in real time to all connected members.

**Why this priority**: Communication and the ability to leave a
room are important for usability but the matchmaking flow itself
works without chat.

**Independent Test**: With three tabs in the same room, Tab A
types "oi gente" and hits Enter. The message appears in Tab B and
Tab C with the sender avatar and a relative timestamp within
500 ms. Tab B clicks "Sair da sala", confirms, and returns to
`/lobby`. Tab A sees Maria's row disappear from the member list.

**Acceptance Scenarios**:

1. **Given** the user clicks "Sair da sala", **When** a
   `<Modal>` appears with "Tem certeza que deseja sair?",
   primary destructive "Sair" and secondary "Cancelar", **Then**
   pressing "Sair" calls `POST /api/rooms/{id}/leave` and
   navigates back to `/lobby` on success.
2. **Given** the host leaves the room, **When** the server emits
   the `ROOM_HOST_CHANGED` event with the new host's id, **Then**
   the "Anfitrião" badge moves to that member's row in every
   client.
3. **Given** the chat sidebar is rendered, **When** the user types
   a message and presses Enter, **Then** the message is sent via
   `POST /api/rooms/{id}/messages` with optimistic UI (rendered
   immediately with a faded "enviando..." indicator) and confirmed
   on the WS echo (the indicator is removed).
4. **Given** a message arrives from another member via
   `/topic/rooms/{id}`, **When** the chat sidebar is not at the
   bottom of the scroll, **Then** a small "Novas mensagens"
   floating pill appears; clicking it scrolls to the latest
   message.
5. **Given** the room is closed (e.g., abandoned by everyone),
   **When** the user navigates to `/rooms/{id}` for a non-existent
   room, **Then** the app shows a PT-BR empty state ("Esta sala
   não existe mais.") with a button "Voltar ao lobby".

---

### User Story 4 - Color picker & member display (Priority: P3)

The color picker presents the six WAR army colors (Manual §2.1:
Vermelho, Azul, Amarelo, Verde, Preto, Branco) as circular swatches
with the localized name on hover. Selecting a color updates the
current user's row. Already-claimed colors are dimmed and
non-clickable.

**Why this priority**: A polish improvement over a basic dropdown.
The room flow works with any color selection mechanism, but a
visual swatch grid is the expected board-game UX.

**Independent Test**: In a room with 3 members, open the color
picker drawer. Six swatches render. Two are already claimed and
visually dimmed with a tooltip ("Já escolhida"). Hover a free
one — the PT-BR color name appears in a tooltip. Click it —
the swatch lights up with a ring and the picker closes.

**Acceptance Scenarios**:

1. **Given** the color picker is open, **When** rendered, **Then**
   it shows six circular swatches with hex backgrounds matching
   `army.red`, `army.blue`, `army.yellow`, `army.green`,
   `army.black`, `army.white` and the PT-BR names "Vermelho",
   "Azul", "Amarelo", "Verde", "Preto", "Branco" available as
   tooltip and `aria-label`.
2. **Given** a swatch is already chosen by another member, **When**
   rendered, **Then** it has `opacity: 0.4`, a small lock icon
   overlay, `aria-disabled="true"`, and pointer events disabled.
3. **Given** the user picks a color that another user picked
   between the click and the request, **When** the backend
   responds with `409 Conflict`, **Then** the UI reverts the
   optimistic update and shows a toast ("Esta cor acabou de ser
   escolhida.").

---

### Edge Cases

- **Banned / kicked from room**: if the backend returns
  `403 Forbidden` on `GET /api/rooms/{id}`, the SPA shows a PT-BR
  message ("Você não tem acesso a esta sala.") and a button back
  to `/lobby`.
- **Backend 5xx during creation**: the create-room modal stays
  open and an inline error appears ("Não foi possível criar a
  sala. Tente novamente.").
- **WebSocket disconnect inside a room**: the room view shows a
  small banner "Reconectando..." and uses TanStack Query polling
  every 3 seconds as a fallback until the socket reconnects.
- **Two clicks on "Iniciar partida"**: the button is disabled
  while in-flight (`aria-busy="true"`); only the first request is
  sent.
- **Room becomes invalid mid-action**: e.g., user is choosing a
  color, then host leaves and last member triggers room closure —
  the next response 404s; the UI navigates back to `/lobby` with
  a toast.
- **Long room names**: truncated to 40 chars with a tooltip
  showing the full name on hover.
- **Long chat messages**: hard-capped at 500 chars; the textarea
  shows a counter (`245 / 500`) and blocks further typing.
- **Empty chat message**: the send button is disabled until the
  trimmed text is non-empty.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `/lobby` MUST query `GET /api/rooms?status=open` via
  TanStack Query with a 5-second `refetchInterval` and
  `staleTime: 2s`.
- **FR-002**: The lobby page MUST also subscribe (when the
  WebSocket connection is alive) to `/topic/lobby` and on any
  `LOBBY_UPDATED` message invalidate the `["rooms","open"]` query
  to refetch immediately.
- **FR-003**: The lobby grid MUST use Tailwind responsive utilities
  to render 3 columns at `lg`, 2 at `md`, 1 below `md`.
- **FR-004**: Each `<RoomCard>` MUST show: room name, host
  display-name + avatar, current/max players ("3 de 6"), a
  horizontal strip of swatches indicating colors already chosen,
  a "Entrar" button (or "Cheia" disabled badge if at max), and a
  lock icon if the room is password-protected.
- **FR-005**: The "Criar sala" button MUST be a floating primary
  button anchored to the top-right of the lobby viewport on
  desktop, or a sticky bottom-right FAB on mobile.
- **FR-006**: The create-room `<Modal>` MUST contain fields:
  `name` (3–40 chars), `maxPlayers` (dropdown 3–6), `password`
  (optional, 4–32 chars, masked input with a "Mostrar/Esconder"
  toggle).
- **FR-007**: On submit the modal MUST call `POST /api/rooms` with
  `{ name, maxPlayers, password? }`. On 201 the modal closes and
  the app navigates to `/rooms/{id}` returned by the server.
- **FR-008**: Joining a password-protected room MUST prompt for
  the password via a `<Modal>` before calling
  `POST /api/rooms/{id}/join` with `{ password }`. A 401
  response shows a PT-BR error "Senha incorreta." in the modal.
- **FR-009**: `/rooms/:roomId` MUST render a 2-column layout on
  desktop: **2/3 width main panel** (room name header, member
  list, color picker drawer, action bar with "Iniciar partida"
  and "Sair da sala") + **1/3 width chat sidebar**. On mobile
  the chat collapses behind a "Chat" toggle button revealing a
  bottom sheet.
- **FR-010**: The member list MUST render each member as a row
  with: avatar, display name, color swatch (or "Sem cor"
  placeholder), host badge ("Anfitrião") for the host, "Você"
  badge for the current user.
- **FR-011**: The color picker MUST present the six colors from
  `EGameColors` (Manual §2.1) as circular swatches keyed to
  Tailwind tokens `army.red`, `army.blue`, `army.yellow`,
  `army.green`, `army.black`, `army.white`. The PT-BR labels are
  "Vermelho", "Azul", "Amarelo", "Verde", "Preto", "Branco".
- **FR-012**: Each swatch MUST have an accessible label
  (`aria-label="Escolher cor Vermelho"`) and a visible focus ring.
  Selecting a swatch fires `PATCH /api/rooms/{id}/members/me` with
  `{ color: "RED" | "BLUE" | ... }`. The endpoint key uses the
  English enum from `EGameColors`.
- **FR-013**: The "Iniciar partida" button MUST be visible only
  to the host. It MUST be disabled when fewer than 3 members
  have selected a color OR when any two members share a color
  (the latter should be prevented by the picker, but defended
  against). The tooltip explains the constraint citing
  Manual §2.2.
- **FR-014**: On click the start button calls
  `POST /api/rooms/{id}/start`. Every connected member listens to
  `/topic/rooms/{id}` and on a `MATCH_STARTED { matchId }` event
  navigates to `/matches/{matchId}`.
- **FR-015**: "Sair da sala" MUST always be visible. It opens a
  destructive `<Modal>` with "Sair" (danger variant) and
  "Cancelar". On confirm: `POST /api/rooms/{id}/leave`, then
  `navigate("/lobby")`.
- **FR-016**: The room's STOMP subscription MUST handle these
  message types: `ROOM_MEMBER_JOINED`, `ROOM_MEMBER_LEFT`,
  `ROOM_COLOR_CHANGED`, `ROOM_HOST_CHANGED`, `ROOM_MESSAGE`,
  `MATCH_STARTED`. Each MUST update the local cache without
  triggering a full refetch.
- **FR-017**: The chat sidebar MUST render messages as a vertical
  list with each entry showing: sender avatar, display name,
  relative timestamp ("agora", "há 2 min", "às 14:32"), and the
  text. The current user's own messages MUST be visually
  distinguished (e.g., aligned right, lighter background).
- **FR-018**: The chat input MUST be a textarea sized for one
  line by default, expanding up to 4 lines, with a 500-char
  cap and a visible counter at right (e.g., "245 / 500").
- **FR-019**: Pressing Enter (without Shift) sends the message.
  Shift+Enter inserts a newline.
- **FR-020**: Messages are sent via `POST /api/rooms/{id}/messages`
  with optimistic UI: the message is appended immediately with a
  faded styling and a status indicator "Enviando..."; on the
  WS echo it is replaced by the canonical message.
- **FR-021**: When the chat container is NOT scrolled to the
  bottom and a new message arrives, a small floating pill
  appears in the lower-right of the chat ("Novas mensagens");
  clicking it scrolls to the latest.
- **FR-022**: All copy MUST be in PT-BR. Examples: "Lobby",
  "Salas abertas", "Criar sala", "Entrar", "Cheia",
  "Iniciar partida", "Sair da sala", "Aguardando jogadores...",
  "Escolher cor", "Anfitrião", "Você", "Chat", "Enviar".
- **FR-023**: The lobby and room detail views MUST gracefully
  degrade without WebSocket: TanStack Query polling at 5 s
  (lobby) and 3 s (room) MUST keep state reasonably fresh, and
  a small banner reads "Atualizações em tempo real
  indisponíveis." if the socket is down for > 10 seconds.
- **FR-024**: Empty lobby renders a centered illustration plus
  the copy "Nenhuma sala aberta — crie a sua!" and a primary
  CTA.

### Key Entities

- **RoomSummary**: row shown in the lobby. Attributes: `id`,
  `name`, `host: { uid, displayName, photoUrl }`,
  `memberCount`, `maxPlayers`, `passwordProtected: boolean`,
  `claimedColors: EGameColors[]`, `status: "OPEN"|"STARTING"|
  "IN_MATCH"|"CLOSED"`.
- **RoomDetail**: extension of `RoomSummary` with the member
  list. Attributes: `members: RoomMember[]`, `createdAt`,
  `hostUid`.
- **RoomMember**: a player inside a room. Attributes: `uid`,
  `displayName`, `photoUrl`, `color?: EGameColors`,
  `isHost: boolean`, `joinedAt`.
- **RoomMessage**: a chat message. Attributes: `id`, `roomId`,
  `senderUid`, `senderDisplayName`, `senderPhotoUrl`, `text`,
  `createdAt`, `clientTempId?` (for optimistic reconciliation).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The lobby list of up to 50 rooms paints within
  **200 ms** of the API response on a developer laptop with a
  warm Vite build.
- **SC-002**: A new member's join becomes visible to every
  connected client in under **500 ms** via WebSocket.
- **SC-003**: Creating a room (from clicking "Criar sala" in the
  lobby to landing in `/rooms/<id>`) takes fewer than **3 clicks**
  and completes in under **2 seconds** end-to-end.
- **SC-004**: When the host clicks "Iniciar partida", every
  connected member's browser navigates to `/matches/{id}` in
  under **500 ms** (95th percentile).
- **SC-005**: The room view passes axe-core accessibility checks
  with zero "serious" or "critical" violations.
- **SC-006**: A chat message round-trip (Enter → received in
  another client) completes in under **300 ms** at the median
  when the WebSocket is connected.
- **SC-007**: Color selection conflict resolution: when two users
  click the same swatch within the same 200 ms window, exactly
  one succeeds and the other receives a clear PT-BR error toast
  in under **500 ms**.

## Assumptions

- Features 007 (UI foundation) and 008 (Auth pages) are
  implemented; this feature assumes the user is signed-in and
  has a valid Firebase ID token.
- The backend (spec 005) exposes `GET /api/rooms`,
  `POST /api/rooms`, `GET /api/rooms/{id}`,
  `POST /api/rooms/{id}/join`, `POST /api/rooms/{id}/leave`,
  `POST /api/rooms/{id}/start`,
  `PATCH /api/rooms/{id}/members/me`,
  `POST /api/rooms/{id}/messages`.
- The backend (spec 006) exposes a STOMP/WebSocket endpoint at
  `/ws` with subscriptions `/topic/lobby` and
  `/topic/rooms/{id}` and the message types listed in FR-016.
- Room chat is text-only in v1: no images, no reactions, no
  threading.
- Direct messages and friend lists are out of scope for v1.
- Spectator-mode rooms (rooms that allow non-playing observers)
  are out of scope for v1; viewer mode in feature 010 is for
  in-match observation only.
- Room password requirement and visibility are decided at
  creation time and not editable afterward (v1 simplification).
- Player kicking by the host is out of scope for v1; if a
  player is disruptive the host can leave/close the room
  themselves.
- The lobby filter for `status=open` is the only filter for
  v1; future versions may add search, region, or skill filters.
- Date/time formatting uses `Intl.DateTimeFormat("pt-BR")` with
  `date-fns/locale/pt-BR` for relative times.
