# Feature Specification: UI Game Board

**Feature Branch**: `010-ui-game-board`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "The actual gameplay screen — SVG map of 42 territories across 6 continents, turn HUD with phase tracking, click/drag interactions for reinforcement, attack, and movement, dice animation, card hand sidebar with exchange dialog, secret objective viewer, end-of-match modal, action history feed. PT-BR copy. WebSocket-driven state updates."

## User Scenarios & Testing *(mandatory)*

This is the heart of the jWar experience: the playable board where the
classic Brazilian "War" rules come to life. The screen renders the
42-territory map across 6 continents (Manual §1, §4.2), tracks the
current player's turn through the three phases — **Reforço** (Add troops),
**Ataque** (Attack), **Movimento** (Move troops) — and provides the input
surfaces for every legal action: placing initial troops, deploying
reinforcements (with the continent-bonus placement constraint of
Manual §4.2), attacking adjacent territories with 1–3 dice (Manual §5),
moving troops within contiguous own territories respecting the
once-per-turn rule (Manual §8), trading cards for bonus troops following
TABELA II (Manual §10), and viewing the secret objective (Manual §3).
The screen is also the entry point for spectator viewing and the
end-of-match summary.

State for the entire screen mirrors the backend `GameStateSnapshot` via
TanStack Query and is patched in real time over STOMP. The UI is
authoritative for nothing — it sends intents (`POST /api/matches/{id}/
actions`) and re-renders from server-confirmed state, with optimistic
preview animations only for non-state-mutating effects (e.g., dice roll
animation while the request is in flight).

### User Story 1 - Board renders and HUD shows turn state (Priority: P1)

A player who has just been routed to `/matches/{matchId}` sees the full
WAR map with all 42 territories colored by their owner (one of the six
army colors from Manual §2.1), troop counts on every territory, the
six continents visually distinct (color tint and label), and a top HUD
that names the current player, the current phase, and any
phase-specific counters (e.g., "8 exércitos a posicionar").

**Why this priority**: Without the board rendering and the turn HUD,
the game cannot be observed, let alone played. This is the irreducible
core of the screen.

**Independent Test**: With a backend match in progress, navigate to
`/matches/{matchId}`. The SVG map renders within 100 ms with 42
territories colored and labeled. The top HUD shows the current
player's name, avatar, color, and the current phase localized in
PT-BR ("Reforço" / "Ataque" / "Movimento"). Inactive players see a
"Aguardando sua vez" badge instead of action buttons.

**Acceptance Scenarios**:

1. **Given** the user is signed in and is a member of the match,
   **When** they navigate to `/matches/{matchId}`, **Then**
   `GET /api/matches/{id}/state` resolves and the SVG board paints
   all 42 territories with owner color, troop count badge, and
   territory name on hover, in under 100 ms after data is in cache.
2. **Given** the board is rendered, **When** the user hovers over a
   territory, **Then** a tooltip shows the territory name in PT-BR
   (e.g., "Brasil", "Estados Unidos", "Tchecoslováquia") and the
   troop count.
3. **Given** the board is rendered, **When** the user inspects a
   continent label, **Then** each of the six continents (Manual §1)
   is visually tinted distinctly:
   - America do Sul → yellow tint
   - America do Norte → green tint
   - Europa → red tint
   - Africa → blue tint
   - Oceania → purple tint
   - Asia → orange tint
   and the continent name is rendered in PT-BR above the cluster.
4. **Given** the current player is not the viewer, **When** the HUD
   renders, **Then** all action affordances are read-only and a
   "Observando: Maria está jogando" badge is visible at the top.
5. **Given** the match state changes (e.g., another player ends
   their phase), **When** the STOMP message arrives on
   `/topic/matches/{id}`, **Then** the board state updates in under
   200 ms with a subtle animation on changed territories.

---

### User Story 2 - Reinforcement & attack interactions (Priority: P1)

When it is the viewer's turn in the **Reforço** phase, they click their
own territories to deploy troops, watching the deploy counter decrement.
Continent-bonus troops are constrained to be placed inside that
continent (per Manual §4.2 note). When it is their turn in the **Ataque**
phase, they pick one of their territories with > 1 troop, see valid
adjacent enemy targets highlighted, click a target, choose how many
dice to roll (1–3), and watch the dice animate; the result is shown
inline; on a successful conquest a slider asks how many troops to
move into the conquered territory.

**Why this priority**: These two phases together cover ~90% of a
turn's interactions. They are non-trivial UX (multi-step,
constraint-aware) and must be precise to match the manual.

**Independent Test**: In a seeded match where it is the viewer's
turn in Reforço, click an owned territory four times — the troop
badge increments by 1 each click, the counter decrements, and the
fourth click sends `POST /api/matches/{id}/actions` with `{ type:
"DEPLOY", country, count: 4 }`. Then end the phase. In Ataque,
click an own territory with troops, observe red rings on adjacent
enemy territories, click one, accept the dice picker default,
watch the animation, see the result panel update.

**Acceptance Scenarios**:

1. **Given** the viewer is the current player in **Reforço** with
   8 troops to deploy, **When** they click an owned territory and
   press `+` (or click a `+1` button in the tooltip), **Then** the
   troop badge increments by 1, the HUD counter "8 → 7 exércitos a
   posicionar" updates, and a `POST /api/matches/{id}/actions` with
   `{ type: "DEPLOY", country, count: 1 }` is sent.
2. **Given** the player received 2 extra troops for owning South
   America entirely (Manual §4.2), **When** these bonus troops are
   active, **Then** territories OUTSIDE South America are dimmed
   and unclickable for the duration of those 2 troops; a banner
   reads "Posicione os 2 exércitos bônus de **America do Sul**".
3. **Given** the viewer is in **Ataque** phase, **When** they
   click an own territory with troops > 1, **Then** the source
   territory shows a green selection ring and all adjacent enemy
   territories show a red dashed ring; territories that are NOT
   valid targets remain unstyled.
4. **Given** a valid target is clicked, **When** the attack picker
   `<Modal>` opens, **Then** it shows a stepper "1–3 exércitos
   (você tem N-1 disponíveis)" capped at min(3, troops − 1) and a
   primary "Atacar" button.
5. **Given** the attack request is sent, **When** the server
   responds with `{ attackerDice, defenderDice, attackerLosses,
   defenderLosses, conquered: boolean }`, **Then** the dice
   animation plays (≤ 1 s), each die settles on its face, and a
   result panel appears summarizing losses in PT-BR ("Ataque: 2
   dados perdidos · Defesa: 1 dado perdido").
6. **Given** the attack resulted in conquest (`conquered: true`),
   **When** the result panel closes, **Then** a follow-up modal
   appears asking "Quantos exércitos mover para [Territory]?"
   with a slider from `attackerDice` to `sourceTroops - 1`
   (Manual §7 — minimum equals number of dice used in the
   final attack roll); confirming sends a `POST .../actions`
   with `{ type: "MOVE_AFTER_CONQUEST", source, target, count }`.

---

### User Story 3 - Movement phase, card hand, and exchange (Priority: P1)

In **Movimento**, the viewer relocates troops between contiguous
own territories under the constraint that no single troop moves
twice in one turn (Manual §8). The right-hand drawer shows the
player's territory cards: face-down for opponents (count only),
face-up for the viewer, with each card revealing its territory and
its shape (circle / triangle / square / joker — Manual §1, §10).
The viewer can select a valid 3-card combination (3 of a kind or 3
different shapes; jokers wild — Manual §10) and trade them for
bonus troops per TABELA II.

**Why this priority**: Movement and card exchange round out the
turn and are essential to mid-/late-game strategy. Without them
the game collapses to attrition.

**Independent Test**: In Movimento, drag troops from territory A
to a contiguous territory B; the troop badge on A decrements and
on B increments; the request `POST /api/matches/{id}/actions`
with `{ type: "MOVE", source, target, count }` is sent. Then open
the card drawer, select three cards (e.g., three circles), click
"Trocar cartas", confirm in the modal, see the deploy counter
increase by the troops awarded.

**Acceptance Scenarios**:

1. **Given** the viewer is in **Movimento**, **When** they click an
   own territory with troops > 1, **Then** all contiguous own
   territories are highlighted with a green dashed ring; non-
   contiguous own territories and any non-owned territories are
   dimmed.
2. **Given** a source and target are selected, **When** the user
   drags a number-bubble or uses a slider in the move tooltip,
   **Then** the action `POST .../actions { type: "MOVE", source,
   target, count }` is sent; the response updates the troop
   counts on both territories.
3. **Given** a troop has already moved this turn (per backend
   tracking), **When** the user tries to relocate it again,
   **Then** the slider's max value is reduced accordingly and a
   helper text in PT-BR explains "Estes exércitos já se moveram
   nesta rodada (Manual §8)".
4. **Given** the card drawer is open, **When** the viewer's hand
   contains 3 valid cards for an exchange, **Then** the
   "Trocar cartas" primary button becomes enabled with the
   resulting bonus count visible (e.g., "Trocar — receber 4
   exércitos").
5. **Given** the viewer selects 3 cards whose territories they
   currently own, **When** the exchange modal opens, **Then** it
   shows an additional bonus of "+2 exércitos em [Território]"
   for each owned card per Manual §10 ("Bônus de território").
6. **Given** the exchange is confirmed, **When**
   `POST .../actions { type: "EXCHANGE_CARDS", cardIds }`
   succeeds, **Then** the cards are removed from the drawer with
   a slide-out animation, the deploy counter increases by the
   awarded bonus, and the +2 territory bonuses are deposited on
   the matching territories with a subtle pulse.

---

### User Story 4 - Secret objective viewer & end-of-match (Priority: P1)

The viewer can collapse/expand a panel "Meu objetivo" that displays
only their secret objective text, in PT-BR. When the match ends —
either by a `MATCH_FINISHED` event from the backend or by the local
state reaching a terminal — an end-of-match `<Modal>` shows the
winner, their (now revealed) objective, key stats, and two CTAs.

**Why this priority**: A WAR match without a visible objective is
unplayable strategy. The end-of-match screen is the payoff for the
whole game loop.

**Independent Test**: While in a match, click "Meu objetivo" — a
collapsible panel reveals the viewer's objective text (e.g.,
"Conquistar America do Sul e Africa"). When the backend emits
`MATCH_FINISHED { winner, objective }`, a modal appears with
"Maria venceu!" and the objective text; buttons "Ver mapa final"
and "Voltar ao lobby" route accordingly.

**Acceptance Scenarios**:

1. **Given** the match is active, **When** the viewer clicks
   "Meu objetivo" in the right sidebar, **Then** an inline panel
   expands showing the objective text in PT-BR (e.g., "Destruir
   o exército Azul. Caso seja o seu próprio, conquistar 24
   territórios."). No other player sees this.
2. **Given** `MATCH_FINISHED` arrives, **When** the SPA processes
   it, **Then** a non-dismissible `<Modal>` overlays the board
   with: winner display name + avatar + color swatch, the
   revealed winning objective, summary stats (turns played, top
   territory-holder, longest streak), and two buttons:
   "Ver mapa final" (closes the modal, board remains visible
   read-only) and "Voltar ao lobby" (navigates to `/lobby`).
3. **Given** the user clicks "Ver mapa final", **When** the modal
   closes, **Then** the board remains rendered with all
   territories visible, all phase buttons disabled, and a small
   ribbon "Partida encerrada" appears at the top.

---

### User Story 5 - Dice animation & action history (Priority: P2)

Dice rolls play a short 3D-style CSS/SVG animation by default; the
player can disable animations from the settings menu (accessibility:
respects `prefers-reduced-motion`). A collapsible "Histórico de
ações" drawer at the bottom lists every action chronologically with
a localized PT-BR summary, supporting scroll-back through the entire
match.

**Why this priority**: Polish and clarity over pure functionality —
the game is playable with static dice, but animation and history
make it readable and enjoyable.

**Independent Test**: Roll an attack — the dice tumble visibly for
roughly 1 second before settling on faces matching the server
result. Open the bottom action-history drawer; the latest entry
reads e.g., "Brasil atacou Argentina — vencedor: ataque (2 vs 1)".
Scroll up to see earlier entries. In settings, toggle "Reduzir
animações"; subsequent dice show faces immediately.

**Acceptance Scenarios**:

1. **Given** the user has not opted out of animations, **When** a
   dice roll request resolves, **Then** an animation of ~800 ms
   plays during which each die spins; on completion each die
   shows its server-confirmed face value.
2. **Given** `prefers-reduced-motion: reduce` is set OR the user
   has toggled "Reduzir animações" in settings, **When** a dice
   roll resolves, **Then** the dice render directly on their
   final faces with no animation.
3. **Given** the action history drawer is open, **When** an
   action is performed, **Then** a new entry is prepended to the
   list with a relative timestamp ("agora") and a PT-BR summary
   formatted like `"<Atacante> atacou <Defensor> — vencedor:
   <ataque|defesa> (<atkLoss> vs <defLoss>)"`.
4. **Given** the action history is collapsed, **When** a new
   action arrives, **Then** the drawer header shows a "Novo"
   badge with a count of unread entries; opening the drawer
   clears the badge.

---

### User Story 6 - Spectator mode (Priority: P3)

A user who is not a player in the match — e.g., a friend who joined
a "watch" link or a user from a finished match revisiting it — sees
the board fully but cannot perform actions and cannot see any
player's secret objective. Action history and dice animations are
visible.

**Why this priority**: Nice-to-have for sharing matches with
friends; not blocking the v1 multiplayer experience.

**Independent Test**: Open `/matches/{id}` as a user not in the
match's `members[]`. The board renders fully, the HUD shows a
"Modo espectador" badge, all action buttons are absent, the "Meu
objetivo" panel is replaced by an "Objetivos secretos ocultos"
caption, and the right drawer shows only face-down card counts
for every player.

**Acceptance Scenarios**:

1. **Given** the viewer is not in `members[]`, **When** the match
   screen mounts, **Then** the HUD shows "Modo espectador" and no
   action affordances are rendered.
2. **Given** the match is finished, **When** any user (including
   non-members) navigates to `/matches/{id}`, **Then** they see
   the final board state with the winner ribbon and the
   `MATCH_FINISHED` summary in a side panel.

---

### Edge Cases

- **Invalid action attempted** (e.g., attacking from a 1-troop
  territory): the UI MUST prevent this on the client (button
  disabled) AND show a PT-BR error toast if the server rejects
  with `code: "INVALID_ACTION"`.
- **Out-of-turn action**: action buttons are not rendered when
  it is not the viewer's turn; if a race condition allows the
  request to fire, the server's `code: "NOT_YOUR_TURN"` triggers
  a re-fetch and a toast "Não é a sua vez."
- **Forced card exchange**: per Manual §10, having 5 cards
  triggers a mandatory exchange at the start of the next Reforço.
  The UI MUST open the exchange modal automatically with the
  current best legal selection pre-highlighted, and Encerrar
  phase is disabled until the exchange completes.
- **Player disconnects mid-turn**: the HUD shows a small "offline"
  indicator on that player's avatar; the backend's timeout
  policy handles turn advancement, the UI just reflects it.
- **Network drop in the middle of an action**: TanStack Query
  retries once; if still failing, the action is shown as failed
  ("Falha ao enviar. Tente novamente.") with a retry button. The
  optimistic preview is reverted.
- **Browser zoomed / tiny viewport**: the board uses an SVG
  `viewBox` so it scales; below `md` the UI shifts to a portrait
  layout with the board horizontally scrollable and actions in a
  bottom sheet.
- **Color-blind users**: each army color is paired with a unique
  shape pattern on the troop badge (circle, triangle, square,
  star, hex, plus). The pattern legend lives in the settings
  panel.
- **Two players claim the same color**: prevented by feature 009;
  defensively, the UI uses the backend `playerId` as the source
  of truth and maps to color via the player record.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `/matches/:matchId` MUST fetch `GET /api/matches/{id}/
  state` via TanStack Query on mount and subscribe to
  `/topic/matches/{id}` over STOMP. Every server event MUST patch
  the local cache (no full refetch unless the diff cannot be
  applied).
- **FR-002**: The board MUST be rendered as a single SVG (within
  a responsive container) using a `viewBox` of approximately
  `0 0 1600 900`. The map asset MUST be an in-repo simplified WAR
  map under `frontend/src/assets/map/`; the chosen artwork MUST
  be either original to the project or under a public-domain /
  CC0 license, with provenance documented in
  `frontend/src/assets/map/README.md`.
- **FR-003**: All 42 territories MUST be rendered as SVG paths
  with `data-country="<EClassicCountries key>"`, an accessible
  `<title>` element holding the PT-BR name, and an `<aria-label>`
  for screen readers. Country keys MUST match the backend enum
  `EClassicCountries`.
- **FR-004**: Each territory MUST render a troop badge anchored
  to a "centroid" point in the path. The badge background uses
  the owner's `army.*` Tailwind token; the badge MUST overlay a
  color-blind shape pattern (one of 6 patterns mapped 1:1 to the
  6 army colors).
- **FR-005**: Continents (the six enum values in
  `EClassicContinents`) MUST be visually distinguished by tinted
  overlays (low-opacity fills) and labeled with their PT-BR
  names: "America do Sul", "America do Norte", "Europa",
  "Africa", "Oceania", "Asia".
- **FR-006**: The top HUD MUST be a fixed bar showing: current
  player's avatar + display name + color swatch, the localized
  phase chip ("Reforço" | "Ataque" | "Movimento"), phase-specific
  counters (e.g., "8 exércitos a posicionar"), and a primary
  "Encerrar fase" button (visible only on the current player's
  client).
- **FR-007**: When it is NOT the viewer's turn, the HUD MUST
  replace action affordances with a passive caption
  "Aguardando: <current player name>".
- **FR-008**: In **Reforço**, clicking an owned territory MUST
  open a small popover with a `+1` button, a `+5` button (when
  remaining ≥ 5), and a numeric input bounded to `[1, remaining]`.
  Each commit fires `POST .../actions` with
  `{ type: "DEPLOY", country, count }`.
- **FR-009**: During the placement of continent-bonus troops
  (Manual §4.2), the UI MUST restrict clickability to territories
  inside the bonus-source continent and surface a banner
  "Posicione os <n> exércitos bônus de <continente>".
- **FR-010**: In **Ataque**, the workflow MUST be:
  (1) click own territory with troops > 1 → green ring +
  red dashed rings on adjacent enemy targets;
  (2) click an adjacent enemy → open `<AttackModal>` with a
  stepper 1–`min(3, sourceTroops-1)` and an "Atacar" button;
  (3) on submit POST `{ type: "ATTACK", source, target,
  attackerDice }`;
  (4) play dice animation;
  (5) if `conquered`, open `<MoveAfterConquestModal>` with a
  slider `min = attackerDice`, `max = sourceTroops - 1`
  (Manual §7).
- **FR-011**: Adjacency MUST be sourced from the backend's
  authoritative graph (already in `EClassicCountries`); the UI
  MUST NOT redefine adjacency in code.
- **FR-012**: The `<AttackModal>` MUST cap the dice stepper at
  `min(3, sourceTroops - 1)`; if `sourceTroops = 2` the only
  value is 1 die.
- **FR-013**: In **Movimento**, the UI MUST allow movement only
  between contiguous own territories AND MUST respect the
  per-troop "moves once per turn" rule by reading the
  per-territory `movableTroops` field from the snapshot (Manual
  §8).
- **FR-014**: Movement input MUST support two equivalent
  modalities: (a) click source → click target → slider; (b)
  drag-and-drop a number-bubble from source to target.
- **FR-015**: A right drawer "Minhas cartas" MUST list the
  viewer's territory cards as small rectangles showing: the
  territory name in PT-BR, a shape glyph (circle / triangle /
  square / joker), and a colored stripe matching the owner's
  army color. Cards belonging to currently-owned territories
  MUST show a small "Você possui" badge to remind the player of
  the +2 bonus on exchange.
- **FR-016**: Selecting cards MUST follow Manual §10: a valid
  set is exactly **3 cards** where either all three shapes are
  the same OR all three shapes are different (jokers wild). The
  UI MUST disable the "Trocar cartas" button until the
  selection is valid.
- **FR-017**: The exchange modal MUST display the troop count
  awarded per the current `exchangeRound` index (TABELA II:
  1st = 4, 2nd = 6, 3rd = 8, 4th = 10, 5th = 12, +5 per round
  thereafter — values sourced from backend, not duplicated in
  the UI). It MUST also list any +2 territory bonuses (Manual
  §10) by name.
- **FR-018**: If the viewer's hand reaches 5 cards (Manual §10
  forced exchange), the UI MUST auto-open the exchange modal at
  the start of the next Reforço, pre-select the best legal
  combination, and prevent "Encerrar fase" until the exchange
  completes.
- **FR-019**: A right-side collapsible panel "Meu objetivo" MUST
  display the viewer's objective text in PT-BR, sourced from
  the snapshot's per-viewer `myObjective` field. Other players'
  objectives MUST NOT be present in any payload visible to the
  viewer.
- **FR-020**: On `MATCH_FINISHED` events, a non-dismissible
  `<Modal>` MUST overlay the board with winner info, the now-
  revealed winning objective text, and the two CTAs
  "Ver mapa final" and "Voltar ao lobby".
- **FR-021**: Dice MUST animate by default for ~800 ms using a
  CSS 3D transform. The animation MUST be skipped when
  `(prefers-reduced-motion: reduce)` is true OR when the
  settings store flag `reducedMotion === true`.
- **FR-022**: A bottom collapsible "Histórico de ações" drawer
  MUST render every action as a PT-BR sentence with a relative
  timestamp. Event types covered: DEPLOY, ATTACK, MOVE,
  MOVE_AFTER_CONQUEST, EXCHANGE_CARDS, PHASE_ENDED,
  TURN_ENDED, MATCH_FINISHED.
- **FR-023**: The board MUST occupy roughly **70%** of viewport
  width on desktop (`lg+`) with the card drawer on the right
  (`~20%`), the HUD on top (`~60 px` tall), and the action log
  collapsed at the bottom by default.
- **FR-024**: On mobile (`< md`), the layout MUST switch to
  portrait: the board fills the top viewport area and is
  horizontally scrollable; phase-specific actions appear in a
  bottom sheet; cards and objective live in tabs of a single
  collapsible right drawer that becomes a full-height bottom
  sheet.
- **FR-025**: Spectators (users not in `members[]`) MUST NOT
  receive any payload field corresponding to per-player secret
  state (objectives, cards). Defensive UI: any spectator render
  that receives a non-empty `myObjective` MUST treat it as a
  bug and refuse to display it.
- **FR-026**: All copy MUST be in PT-BR. Mandatory phrases
  include: "Reforço", "Ataque", "Movimento", "Encerrar fase",
  "Encerrar turno", "Minhas cartas", "Trocar cartas",
  "Meu objetivo", "Histórico de ações", "Atacar", "Atacar com
  quantos exércitos? (1–3)", "Quantos exércitos mover?",
  "Aguardando sua vez", "Modo espectador", "Posicione os
  exércitos bônus de <continente>", "Reduzir animações",
  "Ver mapa final", "Voltar ao lobby", "Partida encerrada".
- **FR-027**: Every action button MUST be keyboard-operable.
  The board territories MUST be reachable via Tab and selectable
  with Enter; arrow keys navigate to adjacent territories. A
  visible focus ring is mandatory.
- **FR-028**: Color-blind support MUST be on by default: troop
  badges always render with a shape pattern in addition to color.
  A settings toggle "Desativar padrões de cor" exists for users
  who prefer flat color.
- **FR-029**: When a STOMP message arrives that cannot be
  applied to the local cache (schema mismatch, missing keys),
  the UI MUST fall back to a full `GET /api/matches/{id}/state`
  refetch within 500 ms and log a warning.
- **FR-030**: The "Encerrar fase" button MUST be disabled when
  the current phase has unresolved required actions, with a
  tooltip explaining what is missing (e.g., "Posicione os
  exércitos restantes (3)" in Reforço).

### Key Entities

- **BoardState**: frontend mirror of the backend
  `GameStateSnapshot`. Attributes: `matchId`, `turnIndex`,
  `phase: "DEPLOY"|"ATTACK"|"MOVE"`, `currentPlayer`,
  `players: PlayerSummary[]`, `territories: Territory[]`,
  `troopsToDeploy?`, `continentBonusContext?`,
  `exchangeRound`, `myCards`, `myObjective`,
  `recentActions: ActionLogEntry[]`.
- **Territory**: a single country. Attributes: `id` (key from
  `EClassicCountries`), `nameI18n`, `continent`, `ownerPlayerId`,
  `troops`, `movableTroops`, `centroid: { x, y }`, `pathD`
  (SVG path data, loaded from the map asset).
- **Continent**: a region. Attributes: `id` (key from
  `EClassicContinents`), `nameI18n`, `territoryIds`,
  `bonusTroops`, `tintColor`.
- **CardSelection**: local UI state for the exchange drawer.
  Attributes: `selectedCardIds: string[]`, `isValidSet: boolean`,
  `awardedTroops: number`, `territoryBonusCountries: string[]`.
- **DiceResult**: result of an attack roll. Attributes:
  `attackerDice: number[]`, `defenderDice: number[]`,
  `attackerLosses`, `defenderLosses`, `conquered: boolean`.
- **ActionLogEntry**: a chronological event. Attributes: `id`,
  `type`, `actorPlayerId`, `payload`, `localizedText`,
  `serverTimestamp`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Rendering 42 territories from cached state MUST
  complete in under **100 ms** on a developer laptop (first
  paint to last troop badge).
- **SC-002**: Dice animation MUST complete in under **1 second**
  and dice MUST always settle on the server-confirmed face
  values.
- **SC-003**: Board state updates triggered by a WebSocket event
  MUST be visible in under **200 ms** from message receipt at
  the 95th percentile.
- **SC-004**: A reinforcement click MUST update the troop badge
  optimistically within **50 ms** and reconcile with the server
  response (revert on error) within **500 ms**.
- **SC-005**: The match screen MUST pass axe-core accessibility
  checks with zero "serious" or "critical" violations for
  Reforço, Ataque, and Movimento phase states.
- **SC-006**: A keyboard-only user MUST be able to (a) reach
  every territory via Tab/arrows, (b) initiate an attack and
  set the dice count via keyboard, (c) confirm a card exchange,
  (d) end a phase — without using a pointer device.
- **SC-007**: Color-blind users running the Cb simulator on
  the production build MUST be able to distinguish every army
  thanks to the shape-pattern overlays (manual check on
  deuteranopia, protanopia, tritanopia).
- **SC-008**: Total JavaScript transferred for `/matches/:id`
  on a cold cache (excluding the SVG map asset) MUST be under
  **300 KB** gzipped.
- **SC-009**: The end-of-match modal MUST appear in under
  **500 ms** of the `MATCH_FINISHED` event arriving.

## Assumptions

- Features 007, 008, and 009 are implemented; this feature
  assumes the viewer is signed-in and has joined the room from
  which the match was started.
- The backend (specs 005, 006) exposes:
  `GET /api/matches/{id}/state`,
  `POST /api/matches/{id}/actions` accepting union types
  `DEPLOY | ATTACK | MOVE | MOVE_AFTER_CONQUEST |
  EXCHANGE_CARDS | END_PHASE | END_TURN`,
  STOMP topic `/topic/matches/{id}` emitting
  `STATE_PATCH`, `DICE_ROLLED`, `MATCH_FINISHED`, etc.
- The state snapshot already filters per-viewer secrets
  (objectives, cards), so the UI does not need to redact.
- The map SVG is shipped with the frontend as a static asset;
  the artwork's provenance is documented in
  `frontend/src/assets/map/README.md` (Constitution Principle
  III — Domain Fidelity allows the map to be a simplified
  representation as long as the 42 territories and 6
  continents match the manual).
- TABELA II's exchange values come from the backend in the
  state snapshot; the UI does not hard-code them (Constitution
  Principle I — game logic isolation).
- Spectator-link sharing (e.g., a sharable read-only URL) is
  delivered automatically: any signed-in user can navigate to
  `/matches/{id}` and is treated as a spectator if not in
  `members[]`. Anonymous spectator access is out of scope for
  v1.
- Replays (re-watching a finished match action-by-action) are
  out of scope for v1; the static finished-state view is
  considered sufficient.
- Voice / video chat overlays are out of scope for v1.
- The action history drawer keeps the full log in memory for
  the current match session; persistence across reloads is
  handled by re-fetching from the backend on mount.
- Per-territory animations on owner change are subtle (color
  cross-fade ~ 200 ms) to avoid distracting players.
- The "Encerrar turno" button (vs "Encerrar fase") appears
  only in the Movimento phase; in Reforço and Ataque the
  button is labeled "Encerrar fase".
