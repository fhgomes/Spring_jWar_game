# Feature Specification: Fix and Complete Game Mechanics — Faithful WAR Rules Implementation

**Feature Branch**: `002-fix-and-complete-game-mechanics`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Fix every P1/P2 bug from `docs/analysis/02-mechanics-gap-analysis.md` and implement every missing mechanic from `docs/regras-do-jogo.md` so that `jwarsv-core` can run a faithful end-to-end game of classic Brazilian WAR. Bundles cover battle resolution, conquest, move phase, round flag transition, end-of-game state, jokers and 5-card forced exchange, +2 owned-territory exchange bonus, reinforcement formula, continent bonus enforcement, discard pile, elimination random discard, and data-table fixes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Faithful Battle, Conquest, and Move Phase (Priority: P1)

As a player attacking a contiguous enemy territory, I need the battle dice to be rolled with the correct counts, losses tallied correctly, conquered territory occupied by a legal troop transfer, and the post-attack Move phase to allow strategic redeployment — so that a single turn from attack through end-of-turn matches sections §5, §6, §7, and §8 of `docs/regras-do-jogo.md`.

**Why this priority**: P1 because without correct battle resolution, conquest, and movement, no game can be played to completion. Per `docs/analysis/02-mechanics-gap-analysis.md:39-69` the executive summary lists every one of these as a blocker. The battle bugs are correctness-critical (attacker loses N troops equal to the source country code today — `ClassicGameAttackResProcessor.java:146`), conquest leaves territories at 0 troops violating the manual's "≥1 occupation army" invariant, and the Move phase is wholly missing despite the constant existing (`ClassicGameConstants.java:6`).

**Independent Test**: Drive an attack from a 4-troop attacker country onto a 3-troop defender. Verify (a) defender rolls dice based on its own troop count (capped at 3), (b) the battle loop compares exactly `min(attDice, defDice)` pairs without ArrayIndexOutOfBoundsException, (c) ties favor defender, (d) attacker loses at most `defDice` troops over the battle, (e) attacker's `removeTroops(...)` receives the loss count (never a country code). Then force a conquest (defender to 0): verify the attacker MUST move 1..lastAttackDiceCount troops into the conquered country and source country drops by exactly that count, with source ≥ 1 troop remaining and target ≥ 1 troop. Then end the attack phase, call `moveTroops(src, tgt, qty)` between two owned contiguous countries with `src` retaining ≥ 1 troop, call it again from the same source to a different target — second call respects the "moved-once" rule. Then call `endCurrentTurnMovePhase()` and observe transition to next player.

**Acceptance Scenarios**:

1. **Given** an attacker country with `troopsCount = 4` and a defender country with `troopsCount = 3`, **When** `attack(...)` is called, **Then** `ClassicGameAttacker.getDefPos(tgtCountry.getTroopsCount())` returns `3` (not based on `srcCountry.getTroopsCount()` — current bug at `ClassicGameAttacker.java:13` per `docs/analysis/02-mechanics-gap-analysis.md:180`), `getAttackPos` returns `3`, and the battle proceeds with 3 attacker dice vs 3 defender dice.
2. **Given** an attacker with 3 dice and a defender with 1 die, **When** the battle loop runs, **Then** the loop iterates exactly `min(3, 1) = 1` pair (not `attackers.length = 3` — current bug at `ClassicGameAttacker.java:29` per `docs/analysis/02-mechanics-gap-analysis.md:195`), no `ArrayIndexOutOfBoundsException` is thrown, and the attacker can lose at most 1 troop in that battle (satisfying Manual §6: _"Se a defesa usou apenas 1 dado, o atacante perde no máximo 1 exército (não 2)"_).
3. **Given** an attacker rolling `[6, 5, 4]` and a defender rolling `[6, 5, 3]` (both at 3 dice), **When** pairs are compared, **Then** the highest pair (6 vs 6) goes to defender (tie favors defense per Manual §6: _"Em caso de empate, vence a defesa"_), the second pair (5 vs 5) goes to defender, the third pair (4 vs 3) goes to attacker. Attacker loses 2, defender loses 1.
4. **Given** an `AttackResultVO` produced by the battle with `srcCountryLoss = 2`, **When** `ClassicGameAttackResProcessor.implyDmg` applies the loss, **Then** the call is `srcCountry.removeTroops(result.getSrcCountryLoss())` (not `result.getSrcCountry()` which returns a country code — current bug at `ClassicGameAttackResProcessor.java:146` per `docs/analysis/02-mechanics-gap-analysis.md:196`), and the attacker country drops by exactly 2 troops.
5. **Given** a successful conquest where defender's `troopsCount = 0` and `lastAttackDiceCount = 3`, **When** the conquest flow completes, **Then** the system requires the player to invoke a "post-conquest move" action specifying 1..3 troops to transfer from `srcCountry` to `tgtCountry`. After the transfer, `tgtCountry.troopsCount >= 1`, `srcCountry.troopsCount >= 1`, and `srcCountry.troopsCount + tgtCountry.troopsCount` equals the pre-conquest `srcCountry.troopsCount`. (Manual §7: _"Deve deslocar exércitos atacantes para o território conquistado"_; _"O número de exércitos a ser deslocado neste momento é, no máximo, igual ao número de exércitos que participaram do último ataque"_.)
6. **Given** the player's turn is in `TURN_PHASE_ATTACK` and they call `endCurrentTurnAttackPhase`, **When** the transition fires, **Then** the turn enters `TURN_PHASE_MOVE` (already constant-defined at `ClassicGameConstants.java:6`).
7. **Given** the turn is in `TURN_PHASE_MOVE` and player owns countries A and B which are contiguous (per `CountriesBordersUtil.hasBorder`), with A having `troopsCount = 5`, **When** `moveTroops(A, B, 3)` is called, **Then** A.troopsCount becomes 2, B.troopsCount increases by 3, and the 3 moved troops are flagged as "moved" so a follow-up call `moveTroops(B, C, 3)` to a third contiguous country in the same turn is REJECTED. (Manual §8: _"Um exército pode ser deslocado uma única vez — não é permitido deslocar para um segundo território contíguo na mesma jogada"_.)
8. **Given** A has `troopsCount = 5` and the player calls `moveTroops(A, B, 5)`, **When** the validator runs, **Then** the call is REJECTED because A must retain at least 1 occupation troop. (Manual §8: _"Em cada território deve permanecer pelo menos um exército (o de ocupação), que nunca pode ser deslocado"_.)
9. **Given** A and B are owned by the player but not contiguous (no border per `CountriesBordersUtil`), **When** `moveTroops(A, B, 1)` is called, **Then** the call is REJECTED with `GameRulesException`. (Manual §8: _"deslocamentos de exércitos entre seus territórios contíguos"_.)
10. **Given** the turn is in `TURN_PHASE_MOVE`, **When** `endCurrentTurnMovePhase()` is called, **Then** the player draws their end-of-turn card (if they conquered ≥ 1 territory this turn) and the turn advances to the next player.

---

### User Story 2 - Round 1/2 Flag Transition and End-of-Game Terminal State (Priority: P1)

As a player participating in a 3-6 player match, I need the first-round and second-round flags to transition correctly so that the special first-round troop distribution (Manual §3) actually fires for both rounds, and I need the match to enter a `FINISHED` terminal state with a queryable `winner` field the moment a player achieves their objective, instead of logging "TODO: Handle game end" and continuing to accept actions.

**Why this priority**: P1 because (a) the round-flag bug at `ClassicGame.java:170-178` makes round 2 unreachable — per `docs/analysis/02-mechanics-gap-analysis.md:144-145,337-339`, both `firstRound→false; secondRound→true` and `secondRound→false` execute in the same `turnToNextPlayer()` call, so the special distribution that was supposed to fire on round 2 is skipped; (b) the missing terminal state means a game continues accepting attack/move/exchange actions after `EndGameEvaluator.hasPlayerWon` returns true (`ClassicGame.java:151-154,234-237` — both call sites are `// TODO`). Without these fixes the game never ends and the round-2 distribution is silently dropped.

**Independent Test**: Start a 3-player match. After the third player ends their round-1 turn, observe that `firstRound = false` and `secondRound = true`. Run all three players through their second-round turns. After the last player of round 2 ends their turn, observe that `secondRound = false` and the formula reinforcement (`max(3, floor(N/2))`) applies from round 3 onward. Separately: trigger a `DESTROY_PLAYER_RED` win condition. Confirm `getMatchStatus() == FINISHED`, `getWinner()` returns the victor, and any subsequent call to `attack(...)`, `moveTroops(...)`, or `endCurrentTurn(...)` throws `GameRulesException` with a "match finished" message.

**Acceptance Scenarios**:

1. **Given** a 3-player match where `currentPlayer == 3 == qtdPlayers` at the end of round 1, **When** `turnToNextPlayer()` is called, **Then** the state machine refactor sets `firstRound = false` and `secondRound = true` in a single transition AND does NOT immediately flip `secondRound = false` in the same call (the current bug at `ClassicGame.java:170-178` per `docs/analysis/02-mechanics-gap-analysis.md:337-339`).
2. **Given** the match is in round 2 (`secondRound == true`), **When** a player's add-phase fires, **Then** the special first-round distribution table from `ClassicGameDist.distributeFirstRoundsTroops` is applied (3/4/5/7 troops by player count per `ClassicGameDist.java:186-201`).
3. **Given** the last player of round 2 ends their turn, **When** `turnToNextPlayer()` is called, **Then** `secondRound` transitions to `false` and from round 3 forward the formula `max(3, floor(N/2))` from US4 applies.
4. **Given** `ClassicGame` exposes a `MatchStatus` enum with values `LOBBY`, `IN_PROGRESS`, `FINISHED`, **When** the match is created via the lobby, **Then** `getMatchStatus() == LOBBY`.
5. **Given** `ClassicGame.startMatch()` is called from the lobby, **When** the call completes, **Then** `getMatchStatus() == IN_PROGRESS`.
6. **Given** a player completes their objective and `EndGameEvaluator.hasPlayerWon(...)` returns `true`, **When** the post-action hook runs (either after attack at `ClassicGame.java:149` or after turn advance at `ClassicGame.java:233`), **Then** `setMatchStatus(FINISHED)` fires, `setWinner(player)` fires, and the `// TODO: Handle game end` placeholders at lines 153 and 236 are replaced with the real state transition.
7. **Given** `getMatchStatus() == FINISHED`, **When** any `ClassicGamePActions` method is called (`attack`, `addTroops`, `moveTroops`, `exchangeCards`, `endCurrentTurn`, etc.), **Then** the call throws `GameRulesException` with a clear "match finished" message.
8. **Given** the match is `FINISHED`, **When** the caller invokes `getWinner()`, **Then** the method returns the winning `ClassicGamePlayer` (whose objective card may be surfaced for display per Manual §12: _"ele deve mostrar sua carta-objetivo"_).

---

### User Story 3 - Jokers, 5-Card Forced Exchange, and +2 Owned-Territory Exchange Bonus (Priority: P1)

As a player accumulating territory cards, I need the deck to include 2 jokers, the 5-card threshold to force me to exchange before I can continue, and the +2-per-owned-card bonus to be added correctly during an exchange so that the card-exchange subsystem matches Manual §10.

**Why this priority**: P1 because each of the three pieces is required for the card subsystem to behave correctly: the deck has 44 cards including 2 curingas per Manual §1 (currently 42 per `EClassicCountryCard.java:13-66` per `docs/analysis/02-mechanics-gap-analysis.md:85-86`); the forced exchange at 5 cards is missing per `docs/analysis/02-mechanics-gap-analysis.md:254` and means a player can skip the exchange indefinitely; and although the +2 owned-territory bonus is *partially* implemented at `ExchangeCardsEvaluator.java:78-83`, it must be re-verified after joker introduction since a joker matches no specific country and thus contributes zero owned-territory bonus.

**Independent Test**: Create a deck and verify it has 44 cards (42 country + 2 jokers). Deal 5 cards to a player including 2 jokers and 1 country card; verify the player can exchange the 3 cards treating the jokers as any shape. Set a player to 5 cards mid-turn; verify their `endCurrentTurnAddPhase` is REJECTED until they exchange. Exchange 3 owned-territory country cards; verify the player receives the TABELA II prize PLUS 6 extra troops (2 per owned card), and the 6 extras are required to be placed on those three specific territories.

**Acceptance Scenarios**:

1. **Given** `EClassicCountryCard` defines 42 country cards today (`EClassicCountryCard.java:13-66`), **When** the refactor lands, **Then** the enum (or a sibling representation) defines exactly 2 additional joker entries — call them `JOKER_1` and `JOKER_2` — for a total of 44 cards in the deck per Manual §1 (_"44 cartas de territórios (incluindo 2 curingas)"_).
2. **Given** a joker card, **When** it is queried for its shape, **Then** the shape is either a dedicated `WILD` value on `ECardShape` or `null`/equivalent that the exchange validator treats as wildcard.
3. **Given** a player holds `[J1, X_circle, Y_circle]` (joker + two circles), **When** `ExchangeCardsEvaluator.playerExchangeAvailable(...)` checks the trio, **Then** the trio is VALID as a "3-same-shape" exchange — the joker substitutes for a circle. (Manual §10.4: _"O curinga sempre possibilita ao jogador a escolha de qualquer uma das 3 figuras para realizar uma troca"_.)
4. **Given** a player holds `[J1, X_circle, Y_triangle]`, **When** the trio is checked, **Then** it is VALID as a "3-different-shapes" exchange — the joker substitutes for square. (Manual §10.1: _"3 cartas com figuras diferentes, OU 3 cartas com figuras iguais"_.)
5. **Given** a player holds `[J1, J2, X_circle]`, **When** the trio is checked, **Then** it is VALID by either "3 same" (both jokers become circles) or "3 different" (jokers become triangle + square). The implementation MUST accept the trio.
6. **Given** a player ends their attack phase or starts their add phase with exactly 5 cards in hand, **When** `endCurrentTurnAddPhase` or `endCurrentTurnAttackPhase` (or whichever guard the implementation chooses) is checked, **Then** the action is REJECTED with `GameRulesException` until the player performs an `exchangeCards(...)`. (Manual §10.2: _"Quando o jogador acumula 5 cartas, é obrigado, na sua vez de jogar, a trocar 3 cartas por exércitos"_.)
7. **Given** a player has cards representing countries A and B which they OWN, plus a third card representing country C which they do NOT own, **When** they exchange the trio, **Then** they receive (a) the TABELA II prize tracked in `CardExchangeState`, PLUS (b) `2 troops × 2 owned-cards = 4 extra troops`. The 4 extra troops are constrained to be placed on countries A and B (2 each), not anywhere else. (Manual §10.3: _"2 exércitos extras para cada carta trocada que represente território de sua propriedade (além da quantidade indicada na TABELA II)"_; _"obrigatoriamente naquele território"_.)
8. **Given** a player exchanges a trio that includes a joker, **When** the +2 owned-territory bonus is computed, **Then** the joker contributes ZERO extra troops (jokers represent no specific country). Only the country cards in the trio that match owned territories yield +2 each.

---

### User Story 4 - Reinforcement Formula, Continent Bonus Enforcement, Discard Pile, Elimination Random Discard (Priority: P2)

As a player progressing through multiple rounds, I need: (a) my add-phase reinforcement to follow the canonical `max(3, floor(N/2))` formula rather than the misleading `>7` branch; (b) my continent bonus troops to be enforceably placed only inside the conquered continent and only when I fully own it; (c) used exchange cards to accrue to a discard pile that re-enters play only when the draw deck empties; (d) when I eliminate a player and end up with > 5 cards, the excess to be discarded by random sample, not by deterministic list-order.

**Why this priority**: P2 because each item is a correctness bug whose impact is non-blocking — the game can still finish, but with mathematically incorrect outcomes. Per `docs/analysis/02-mechanics-gap-analysis.md:399-431`, these are all in the P2 tier. The reinforcement formula (`ClassicGameDist.java:154-161`) "happens to" give correct values for the current player-count caps but is fragile; continent bonus has a guard gap that becomes exploitable as soon as continent troops can be banked across calls; the immediate-reshuffle of exchanged cards contradicts Manual §10.5 verbatim; and the deterministic "first-N from list" transfer on elimination contradicts Manual §11 ("faz-se um sorteio").

**Independent Test**: For each of the four pieces, write a focused JUnit test: (a) verify a player with 11 owned countries receives `max(3, floor(11/2)) = 5` troops via `distributeRoundTroops`; (b) attempt to call `addContinentTroops` on a country in a continent the player does NOT fully own — verify REJECTED; (c) exchange a trio, then verify the 3 cards are in a separate "discard pile" not back in the draw deck. Drain the draw deck via repeated `drawCardForPlayer` and on next draw verify the discard pile is shuffled into a new draw deck; (d) eliminate a player with 4 cards while victor holds 4. Verify victor receives a randomly-sampled subset of (8 - 5) = 3 cards to discard, not the first 3 in list order. Discards go to the discard pile per (c).

**Acceptance Scenarios**:

1. **Given** a player owns `N` countries where `N ∈ {1, 2, 3, 4, 5, 6, 7, 8, 11, 20, 42}`, **When** `ClassicGameDist.distributeRoundTroops` computes the round troop count, **Then** the result equals `Math.max(3, N / 2)` for every input. For `N = 1..7`, the result is `3`. For `N = 8`, the result is `4`. For `N = 11`, the result is `5`. For `N = 20`, the result is `10`. For `N = 42`, the result is `21`. (Manual §4.1: _"divide por 2 (considerando só a parte inteira do resultado)"_, _"O número mínimo de exércitos a receber é sempre 3"_.)
2. **Given** the player does NOT fully own continent `EClassicContinents.AFR`, **When** the player calls `addContinentTroops(countryInAfrica)`, **Then** the call is REJECTED with `GameRulesException`. (Manual §4.2: _"se ... o jogador possuir um continente inteiro ... recebe exércitos extras"_ implies the bonus is conditional on ownership.)
3. **Given** the player fully owns continent `AFR` and `AFR.availableTroopsCount = 3`, **When** the player calls `addContinentTroops(country)` where `country.getContinent() == AFR`, **Then** 1 troop is placed on `country`, `AFR.availableTroopsCount` decrements to 2, and the player's general `availableTroops` pool is unaffected.
4. **Given** the player fully owns `AFR` with available bonus, **When** the player calls `addContinentTroops(country)` where `country.getContinent() == AMS`, **Then** the call is REJECTED — bonus troops can only be placed inside the continent that granted them. (Manual §4.2: _"obrigatoriamente distribuídos nos territórios do próprio continente"_.)
5. **Given** a player has performed a successful exchange of 3 cards, **When** `ExchangeCardsEvaluator` returns the cards, **Then** the cards are appended to `ClassicGame.cardsDiscardPile` (a new collection), and the draw deck (`ClassicGame.cardsDeck`) is NOT re-shuffled at this point. (Manual §10.5: _"As cartas trocadas são colocadas à parte do jogo"_.)
6. **Given** `ClassicGame.cardsDeck.isEmpty()` and `ClassicGame.cardsDiscardPile` contains N > 0 cards, **When** `drawCardForPlayer` is called, **Then** the discard pile is shuffled and becomes the new draw deck, the discard pile is emptied, and one card is drawn from the new deck. (Manual §10.5: _"Quando todas as cartas tiverem sido distribuídas, são recolhidas, embaralhadas e recolocadas em jogo formando um novo monte"_.)
7. **Given** `ClassicGame.cardsDeck.isEmpty()` AND `ClassicGame.cardsDiscardPile.isEmpty()`, **When** `drawCardForPlayer` is called, **Then** the call is REJECTED with `GameRulesException` (no cards available anywhere — this is the only legitimate empty state).
8. **Given** the attacker eliminates a defender holding cards `[C1, C2, C3, C4]` while attacker holds `[A1, A2, A3]`, **When** `ClassicGameAttackResProcessor.transferCardsFromDefeatedPlayer` runs, **Then** the attacker receives ALL 4 cards, totaling 7. Since 7 > 5, the attacker randomly selects exactly 2 cards (using `Collections.shuffle` or equivalent — not deterministic list order) to discard, and the 2 discarded cards go to `cardsDiscardPile`. The attacker ends with exactly 5 cards. (Manual §11: _"faz-se um sorteio: o jogador retira, sem olhar, o número de cartas necessárias para completar cinco"_.)
9. **Given** the attacker eliminates a defender and the resulting hand totals ≤ 5, **When** the transfer runs, **Then** all cards transfer and no discard happens.
10. **Given** that DESTROY_PLAYER objectives must filter out colors not in play per Manual §2.2 (out of scope of this US, see Edge Cases / Assumptions), **When** the test for US4 acceptance runs, **Then** the four mechanics in this US do not regress that gap. The objective-filter fix is a separate concern tracked at P2-5 in the analysis doc but listed in Assumptions below.

---

### User Story 5 - Data Table Fixes: Country Codes, Continent Names, Border Symmetry, Color Localization (Priority: P3)

As a contributor reading the game's data tables, I need the country/continent/color enums and the border map to be free of duplicates, typos, self-loops, asymmetric edges, and inconsistent localization so that the static data exactly represents the manual's board.

**Why this priority**: P3 because these are cosmetic and data-quality fixes that do not block gameplay (the lobby caps at 6 players so the duplicate-code 12 issue is masked, the border self-loop is harmless, the continent-name typo only shows in logs and UI). However, per `docs/analysis/02-mechanics-gap-analysis.md:434-444` and §2.1 (lines 90-118), the data table is the foundation everything else reads from, and shipping with duplicates and typos is unprofessional.

**Independent Test**: Run a one-shot data-validation test that asserts: (a) `EClassicCountries.values()` has 42 unique codes; (b) every `EClassicContinents` literal has a continent name without typos; (c) every `EClassicCountries` literal uses PT-BR naming consistently; (d) `CountriesBordersUtil.BORDERS` has no entry whose key appears in its own value list (no self-loops); (e) `CountriesBordersUtil.BORDERS` is symmetric — for every `a → b` edge there exists `b → a`; (f) `EGameColors.BLUE.name == "Azul"`.

**Acceptance Scenarios**:

1. **Given** `EClassicCountries.OTW` has code `12` and `EClassicCountries.ALA` also has code `12` (`EClassicCountries.java:21-22` per `docs/analysis/02-mechanics-gap-analysis.md:92-96`), **When** the fix lands, **Then** ALA receives a unique code (e.g., 43) and the countries-by-code map in `ClassicGameDist` (`ClassicGameDist.java:129`) no longer silently overwrites one of them.
2. **Given** `EClassicContinents.ASI(6, "America do Sul", 13, 7)` (`EClassicContinents.java:12` per `docs/analysis/02-mechanics-gap-analysis.md:99-101`), **When** the fix lands, **Then** the name reads "Ásia".
3. **Given** `EClassicCountries.SWD("Sweden", ...)` and `MOS("Moscow", ...)` (lines 29 and 31 per `docs/analysis/02-mechanics-gap-analysis.md:102-108`), **When** the fix lands, **Then** SWD is "Suécia" and MOS is "Moscou".
4. **Given** `EClassicCountries.VIE("Vietan", ...)` (line 51 — typo), **When** the fix lands, **Then** VIE is "Vietnã".
5. **Given** `CountriesBordersUtil.BORDERS.put(NVG, List.of(IND, NVG, AUS))` (line 50 — self-loop on NVG per `docs/analysis/02-mechanics-gap-analysis.md:111-113`), **When** the fix lands, **Then** the NVG entry no longer lists itself.
6. **Given** `CountriesBordersUtil.BORDERS` is asymmetric for the pair BRA↔ARL (BRA does not list ARL but ARL lists BRA — `CountriesBordersUtil.java:16,41` per `docs/analysis/02-mechanics-gap-analysis.md:114-117`), **When** the fix lands, **Then** the map is symmetric: for every `BORDERS.get(a).contains(b)`, the inverse `BORDERS.get(b).contains(a)` is also true.
7. **Given** `EGameColors.BLUE(6L, "Blue")` (`EGameColors.java:12` per `docs/analysis/02-mechanics-gap-analysis.md:109-110`), **When** the fix lands, **Then** the literal name is "Azul" matching the PT-BR convention used by other colors.

---

### Edge Cases

- **What happens when the attacker has 2 troops, attacks, and ties on a single die?** Attacker has 1 attack die (troops - 1 = 1). Defender has 1 defense die. Tie → attacker loses 1 troop → attacker now has 1 troop → cannot attack again from this country (US1 acceptance #2 + Manual §6).
- **What happens when a player tries to move 0 or negative troops?** `moveTroops(src, tgt, 0)` and `moveTroops(src, tgt, -1)` MUST be rejected with `GameRulesException`. A move must transfer ≥ 1 troop.
- **What happens when a player conquers their last enemy without owning their objective card's target?** `EndGameEvaluator.hasPlayerWon` runs all objective evaluators including `DestroyPlayerObjectiveEvaluator` and `TerritoryObjectiveEvaluator`. If the player has not yet hit any objective, the match continues — `MatchStatus` stays `IN_PROGRESS`.
- **What happens when the conquered defender's last card is transferred to the victor and the victor's objective is `DESTROY_PLAYER_<defenderColor>`?** Per Manual §11 and `ClassicGameAttackResProcessor.checkObjectiveAchievedEndGame` (line 58-60 per `docs/analysis/02-mechanics-gap-analysis.md:270-271`), the victor immediately wins. The card transfer and random-discard step is moot since the match transitions to FINISHED.
- **What happens when a player ends their attack phase having drawn no cards but holding 5 cards from a prior elimination?** Per US3 #6, the forced-exchange guard catches this at the next add-phase start. If the player is currently mid-turn with 5 cards, they cannot end the add phase or attack phase until they exchange.
- **What happens when the discard pile is also empty at deck-exhaustion time?** Per US4 #7, this throws `GameRulesException`. With 44 country cards and a 6-player cap, this is reachable only after many turns of card hoarding without any exchanges — a degenerate state the manual does not address. Throwing is safer than silently no-op'ing.
- **What happens when a joker is in the elimination transfer and the random discard selects it?** The joker is discarded to `cardsDiscardPile` like any other card. No special handling required.
- **What happens when the round-flag transition fires on a 6-player match versus a 3-player match?** Per US2 #1, the transition is parameterized by `qtdPlayers`. Same logic, different threshold.
- **What happens when an attacker chooses fewer dice than `min(troops-1, 3)`?** Manual §5 allows the attacker to announce the dice count. This is captured as P1-10 in `docs/analysis/02-mechanics-gap-analysis.md:414`. Per the Assumptions section, this enhancement is out of scope for US1 acceptance — the implementation continues to use `min(troops-1, 3)` automatically until a separate user story addresses the attacker's choice.

## Requirements *(mandatory)*

### Functional Requirements

#### Battle resolution and conquest (US1)

- **FR-001**: `ClassicGameAttacker.attack(...)` MUST compute defender dice from `tgtCountry.getTroopsCount()`, not `srcCountry.getTroopsCount()` (fix `ClassicGameAttacker.java:13`).
- **FR-002**: The battle loop in `ClassicGameAttacker.doResult(...)` MUST iterate exactly `min(attackers.length, defense.length)` pairs (fix `ClassicGameAttacker.java:29`).
- **FR-003**: On every pair comparison, ties MUST favor the defender (already implemented at `ClassicGameAttacker.java:30-34`; verify it survives the loop-bound fix).
- **FR-004**: Defender losses across a single battle MUST NOT exceed `min(attDice, defDice)` (a consequence of FR-002 once correctly bounded).
- **FR-005**: `ClassicGameAttackResProcessor.implyDmg` MUST pass `result.getSrcCountryLoss()` (a count) to `srcCountry.removeTroops(...)`, NOT `result.getSrcCountry()` (a country code) — fix `ClassicGameAttackResProcessor.java:146`.

#### Conquest troop transfer (US1)

- **FR-006**: When defender's `troopsCount` drops to 0 in a battle, the system MUST require the attacker to perform a "post-conquest move" before any further action on this turn.
- **FR-007**: The post-conquest move MUST transfer between 1 and `lastAttackDiceCount` troops from `srcCountry` to `tgtCountry`, where `lastAttackDiceCount` is recorded on `AttackResultVO` at the time of the battle (Manual §7).
- **FR-008**: After the post-conquest move, `tgtCountry.troopsCount >= 1` and `srcCountry.troopsCount >= 1` MUST both hold.
- **FR-009**: After the post-conquest move, the sum `srcCountry.troopsCount + tgtCountry.troopsCount` MUST equal the pre-move `srcCountry.troopsCount` (no troops created or destroyed in the transfer).

#### Move phase (US1)

- **FR-010**: `ClassicGamePActions` MUST expose a method `moveTroops(srcCountryId, tgtCountryId, qty)` that runs only when the current turn phase is `TURN_PHASE_MOVE` (use `ClassicGameValidator.isMovePhase`, currently unused per `docs/analysis/02-mechanics-gap-analysis.md:222`).
- **FR-011**: `moveTroops` MUST reject when `srcCountry` and `tgtCountry` are not both owned by the current player.
- **FR-012**: `moveTroops` MUST reject when `srcCountry` and `tgtCountry` are not contiguous per `CountriesBordersUtil.hasBorder(...)`.
- **FR-013**: `moveTroops` MUST reject when `qty < 1` or when `srcCountry.troopsCount - qty < 1` (the source must retain at least 1 occupation troop per Manual §8).
- **FR-014**: `moveTroops` MUST track moved troops so a moved troop cannot be moved again in the same turn (Manual §8: "Um exército pode ser deslocado uma única vez"). Per `docs/analysis/02-mechanics-gap-analysis.md:226`, no `movedFrom` state exists today on `ClassicGameCountry` or `ClassicGamePlayer` — add it (e.g., a `Set<EClassicCountries>` of countries that received troops this turn, or a per-country `troopsMovedInCount` field).
- **FR-015**: `ClassicGamePActions` MUST expose `endCurrentTurnMovePhase()` which (a) draws the player's end-of-turn card if `hasConqueredCountryThisTurn`, (b) clears the move-tracking state, (c) calls `turnToNextPlayer()`.

#### Round flag transition (US2)

- **FR-016**: `ClassicGame.turnToNextPlayer()` MUST be refactored so the round-1→round-2 transition and the round-2→round-3 transition fire in separate calls, not the same call (fix `ClassicGame.java:170-178`). Concretely: when `currentPlayer == qtdPlayers` and `firstRound == true`, set `firstRound = false; secondRound = true` and STOP — do not check `secondRound` in the same call.
- **FR-017**: When `currentPlayer == qtdPlayers` and `secondRound == true` (from a prior call), `turnToNextPlayer()` MUST set `secondRound = false`.

#### End-of-game terminal state (US2)

- **FR-018**: `ClassicGame` MUST expose a `MatchStatus` enum with values `LOBBY`, `IN_PROGRESS`, `FINISHED`.
- **FR-019**: `ClassicGame` MUST expose a `getMatchStatus()` accessor and a `getWinner()` accessor that returns the winning `ClassicGamePlayer` (null until `FINISHED`).
- **FR-020**: `ClassicGameLobby` flow MUST set `MatchStatus = LOBBY` at creation.
- **FR-021**: `ClassicGame.startMatch()` MUST transition `MatchStatus` from `LOBBY` to `IN_PROGRESS`.
- **FR-022**: When `EndGameEvaluator.hasPlayerWon(...)` returns true at any check-site (currently `ClassicGame.java:149,233`), the system MUST set `MatchStatus = FINISHED` and `winner = currentPlayer` — replacing the `// TODO: Handle game end` placeholders at lines 153 and 236.
- **FR-023**: When `MatchStatus == FINISHED`, every method on `ClassicGamePActions` (`attack`, `addTroops`, `addContinentTroops`, `moveTroops`, `exchangeCards`, `endCurrentTurnAddPhase`, `endCurrentTurnAttackPhase`, `endCurrentTurnMovePhase`, `endCurrentTurn`) MUST throw `GameRulesException` with a "match finished" message before performing any state mutation.

#### Jokers (US3)

- **FR-024**: The card deck MUST contain exactly 44 cards: 42 country cards + 2 jokers (per Manual §1).
- **FR-025**: Jokers MUST be representable in the data model — either by adding `JOKER_1, JOKER_2` to `EClassicCountryCard` with a `WILD` shape (or `null`-shape) marker, or by a sibling enum/value object. The choice is implementation; the data model MUST be honored by `ExchangeCardsEvaluator`.
- **FR-026**: `ExchangeCardsEvaluator.playerExchangeAvailable(...)` MUST accept a trio that contains 1 or 2 jokers, treating each joker as any shape the player chooses. The validator MUST return TRUE for any trio that can be made valid as 3-same-shape OR 3-different-shapes by some assignment of joker shapes (per Manual §10.4).
- **FR-027**: When computing the +2 owned-territory bonus in `ExchangeCardsEvaluator.processCardExchange`, jokers MUST contribute 0 extra troops (jokers represent no specific country).

#### 5-card forced exchange (US3)

- **FR-028**: When the current player holds ≥ 5 cards at the start of their turn, `ClassicGamePActions` MUST require an `exchangeCards(...)` call before any other action proceeds. Concretely: `endCurrentTurnAddPhase` (or whichever action transitions out of add-phase) MUST reject with `GameRulesException` while `cards.size() >= 5` and no exchange has been performed this turn. (Manual §10.2.)
- **FR-029**: The `MAX_CARDS = 5` constant (`ClassicGameConstants.java:9`) MUST remain as the upper bound — a player may not exceed 5 cards. The current silent skip at `ClassicGameDist.drawCardForPlayer:236-239` becomes structurally unreachable once FR-028 holds.

#### +2 owned-territory bonus per card (US3)

- **FR-030**: For each card in the exchanged trio that represents a country owned by the exchanging player, the player MUST receive 2 extra troops AND those 2 troops MUST be placed on that specific country, not on any other territory (already implemented at `ExchangeCardsEvaluator.java:78-83` per `docs/analysis/02-mechanics-gap-analysis.md:252-253`; verify it survives the joker change in FR-027).

#### Reinforcement formula (US4)

- **FR-031**: `ClassicGameDist.distributeRoundTroops` MUST compute base reinforcement as `Math.max(MIN_ROUND_TROOPS, N / 2)` where `N = player.getOwnedCountries().size()` and `MIN_ROUND_TROOPS = 3`. Replace the `>7` branch at `ClassicGameDist.java:154-161` (per `docs/analysis/02-mechanics-gap-analysis.md:157`).

#### Continent bonus enforcement (US4)

- **FR-032**: `ClassicGamePActions.addContinentTroops(country)` MUST verify that the current player fully owns the continent `country.getContinent()` before decrementing the bonus pool. Add a new validator `ClassicGameValidator.playerOwnsContinent(player, continent)` and call it from `addContinentTroops` (per `docs/analysis/02-mechanics-gap-analysis.md:161,420`).
- **FR-033**: `addContinentTroops(country)` MUST reject when `country.getContinent()` does not match the continent that granted the bonus — i.e., the troop can only land inside its own continent (already implicit per `docs/analysis/02-mechanics-gap-analysis.md:162`; verify it survives FR-032).

#### Discard pile (US4)

- **FR-034**: `ClassicGame` MUST gain a `cardsDiscardPile` collection (initially empty) parallel to the existing `cardsDeck`.
- **FR-035**: `ExchangeCardsEvaluator` (currently re-shuffling exchanged cards into the deck at `ExchangeCardsEvaluator.java:103-109` per `docs/analysis/02-mechanics-gap-analysis.md:256`) MUST be refactored to append the 3 exchanged cards to `cardsDiscardPile` WITHOUT touching `cardsDeck`.
- **FR-036**: `ClassicGameDist.drawCardForPlayer(...)` MUST reshuffle `cardsDiscardPile` into `cardsDeck` (and empty `cardsDiscardPile`) when `cardsDeck.isEmpty()` and `cardsDiscardPile` is non-empty. Only when BOTH are empty does the method throw `GameRulesException`.

#### Elimination random discard (US4)

- **FR-037**: `ClassicGameAttackResProcessor.transferCardsFromDefeatedPlayer` MUST transfer ALL of the defeated player's cards to the victor first. If the resulting count exceeds `MAX_CARDS = 5`, the victor MUST randomly select cards (via `Collections.shuffle` or a deterministic-but-uniformly-distributed source) to discard until the count equals 5. Discarded cards go to `cardsDiscardPile` (FR-034). Replace the current order-dependent `availableSlots` logic at `ClassicGameAttackResProcessor.java:97-110` (per `docs/analysis/02-mechanics-gap-analysis.md:269`).

#### Data table fixes (US5)

- **FR-038**: `EClassicCountries.ALA` MUST receive a unique numeric code distinct from `OTW`'s code 12 (e.g., code 43). The countries-by-code map at `ClassicGameDist.java:129` MUST contain 42 distinct entries.
- **FR-039**: `EClassicContinents.ASI` MUST be named "Ásia" (not "America do Sul" — fix `EClassicContinents.java:12`).
- **FR-040**: `EClassicCountries.SWD.name` MUST be "Suécia" (fix `EClassicCountries.java:29`).
- **FR-041**: `EClassicCountries.MOS.name` MUST be "Moscou" (fix `EClassicCountries.java:31`).
- **FR-042**: `EClassicCountries.VIE.name` MUST be "Vietnã" (fix `EClassicCountries.java:51` — typo "Vietan").
- **FR-043**: `CountriesBordersUtil.BORDERS` MUST NOT contain self-loops. The NVG entry currently reads `BORDERS.put(NVG, List.of(IND, NVG, AUS))` (`CountriesBordersUtil.java:50` per `docs/analysis/02-mechanics-gap-analysis.md:111-113`); the self-reference MUST be removed.
- **FR-044**: `CountriesBordersUtil.BORDERS` MUST be symmetric. For every edge `a → b` in the map, the inverse `b → a` MUST also exist. The BRA↔ARL asymmetry (BRA does not list ARL; ARL lists BRA — `CountriesBordersUtil.java:16,41`) MUST be fixed by adding the missing direction.
- **FR-045**: `EGameColors.BLUE.name` MUST be "Azul" (fix `EGameColors.java:12`).

### Key Entities

- **`AttackResultVO`**: The post-battle value object. MUST gain (or already track) `lastAttackDiceCount` so that the post-conquest move can enforce the 1..`dice` cap (FR-007). Currently exposes `srcCountry` (code), `srcCountryLoss` (count), `tgtCountry`, `tgtCountryLoss`, `conquered` per `AttackResultVO.java`. The bug is the consumer at `ClassicGameAttackResProcessor.java:146` passing the wrong field.
- **`ClassicGameCountry`**: Mutable game state. Already exposes `troopsCount`, `owner`, `continent`. MAY gain a `troopsMovedInCount` field (or sibling tracking on `ClassicGamePlayer`) to support the moved-once rule (FR-014).
- **`ClassicGamePlayer`**: Player state. Currently holds `availableTroops`, `cards`, `objective`, `ownedCountries`. MAY gain a `movedSourcesThisTurn: Set<EClassicCountries>` to support FR-014. Cleared at `endCurrentTurnMovePhase` (FR-015).
- **`MatchStatus` enum**: New. Values `LOBBY`, `IN_PROGRESS`, `FINISHED`. Lives on `ClassicGame`.
- **`ClassicGame.winner`**: New field. Type `ClassicGamePlayer`. Set when `MatchStatus` transitions to `FINISHED` (FR-022).
- **`ClassicGame.cardsDiscardPile`**: New collection. Holds exchanged and elimination-discarded cards until the draw deck empties (FR-034, FR-035, FR-037).
- **`EClassicCountryCard.JOKER_1`, `JOKER_2`**: New enum values (or sibling representation). Two jokers in the deck per Manual §1 (FR-024, FR-025).
- **`ECardShape.WILD`** (optional): New shape variant for jokers, OR jokers carry a `null` shape that `ExchangeCardsEvaluator` treats as wildcard. Either is acceptable (FR-026).
- **`ClassicGameValidator.playerOwnsContinent(player, continent)`**: New validator method (FR-032).
- **Move-phase actions**: New methods on `ClassicGamePActions`: `moveTroops(srcCountryId, tgtCountryId, qty)`, `endCurrentTurnMovePhase()`. Wire `TURN_PHASE_MOVE` properly (FR-010 through FR-015).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All 14 sections of `docs/regras-do-jogo.md` (Components, Setup, Round Structure, Reinforcement, Attacks, Battle, Conquest, Movements, Card Pickup, Card Exchange, Elimination, End of Game, plus the Resumo and Apêndice) are covered by passing JUnit 5 tests under `jwarsv-core/src/test/java/`. Each functional requirement FR-001 through FR-045 has at least one dedicated test method.
- **SC-002**: Battle resolution: a parameterized JUnit test covering 9 dice-roll permutations (att=1/2/3 × def=1/2/3) passes with no `ArrayIndexOutOfBoundsException` and verifies attacker losses ≤ defender dice in every case.
- **SC-003**: Conquest invariant: across 1000 randomized successful conquests in property-based-style tests, no conquered country ever ends at `troopsCount = 0` and no source country ever ends at `troopsCount < 1`.
- **SC-004**: Move phase: a test sequence `moveTroops(A, B, 3); moveTroops(B, C, 3)` from the same player on the same turn rejects the second call. A second sequence `moveTroops(A, B, 3); endCurrentTurnMovePhase(); moveTroops(B, C, 3)` (the second call is now on the next player's turn after both round trips) — the post-turn state correctly resets move tracking.
- **SC-005**: Round 2 reinforcement: a deterministic 3-player game played through 2 complete rounds shows the special first-round distribution table (3/4/5/7 by player count) applied for BOTH round 1 and round 2, and the formula `max(3, floor(N/2))` applied from round 3.
- **SC-006**: Terminal state: a test scenario that triggers any winning objective evaluates `getMatchStatus() == FINISHED` AND `getWinner() != null` AND a subsequent `attack(...)` call throws `GameRulesException`.
- **SC-007**: Jokers: a deck has exactly 44 cards. A trio `[JOKER, X_circle, Y_circle]` is accepted by the exchange validator. A trio `[JOKER, JOKER, X_circle]` is accepted.
- **SC-008**: 5-card forced exchange: a test that places 5 cards in a player's hand and tries to call `endCurrentTurnAddPhase` throws `GameRulesException` until the player exchanges.
- **SC-009**: +2 bonus: a test exchanging 3 cards where 2 represent owned countries yields the TABELA II prize PLUS 4 extra troops, with each owned country gaining exactly 2 troops.
- **SC-010**: Formula: parameterized test for `N ∈ {1, 2, 3, 4, 5, 6, 7, 8, 11, 20, 42}` confirms `distributeRoundTroops` returns `max(3, N/2)`.
- **SC-011**: Continent bonus: a test that has the player NOT own AFR but try to call `addContinentTroops(countryInAfrica)` throws `GameRulesException`.
- **SC-012**: Discard pile: a test that exchanges a trio and then inspects `cardsDeck.size()` and `cardsDiscardPile.size()` shows the 3 cards in the discard pile, not the deck.
- **SC-013**: Deck reshuffle: a test that drains `cardsDeck` to empty via repeated draws while accumulating `cardsDiscardPile` shows that the next `drawCardForPlayer` call reshuffles the discard pile into the deck.
- **SC-014**: Elimination random discard: a Monte Carlo test of 10,000 elimination transfers (defeated has 4 cards, victor has 4 cards → 8 total → 3 must be discarded) shows the discarded card distribution is uniform (each card discarded approximately 3/8 = 37.5% of the time, ±2 percentage points).
- **SC-015**: Data integrity: a test asserts `EClassicCountries.values()` has 42 entries with 42 unique codes; `EClassicContinents.ASI.name.equals("Ásia")`; `CountriesBordersUtil` has no self-loops (no key in its own value list); `CountriesBordersUtil` is symmetric for all 42 country pairs.
- **SC-016**: `./gradlew :jwarsv-core:test` passes 100% of the new and existing tests with zero flakes across 10 consecutive runs.
- **SC-017**: A scripted "happy-path" end-to-end test plays a 3-player game from lobby through a winning objective without any `GameRulesException`, no unhandled exceptions, and reaches `MatchStatus.FINISHED` with a non-null `getWinner()`.

## Assumptions

- Per `docs/analysis/02-mechanics-gap-analysis.md:65`, the existing skeleton is sound. This spec does NOT redesign the core engine; it patches identified bugs and fills identified gaps. Existing class names (`ClassicGame`, `ClassicGamePActions`, `ClassicGameAttacker`, `ClassicGameAttackResProcessor`, `ClassicGameDist`, `ExchangeCardsEvaluator`, `ClassicGameValidator`, `EndGameEvaluator`) are reused.
- Per Constitution Principle II (`.specify/memory/constitution.md:40-46`), every new rule MUST have JUnit 5 + Mockito unit tests. SC-001 through SC-017 codify the measurable test outcomes.
- Per Constitution Principle III (`.specify/memory/constitution.md:48-55`), domain fidelity requires PT-BR text. Data-table fixes in US5 honor this (`Suécia`, `Ásia`, `Moscou`, `Azul`).
- The attacker's choice of dice count (1, 2, or 3, ≤ troops - 1, per Manual §5 and `docs/analysis/02-mechanics-gap-analysis.md:181`) is tracked as P1-10 in the analysis but is OUT OF SCOPE of this spec — it changes the `attack(...)` API signature, which is a separate feature surface. The implementation continues to use `min(troops-1, 3)` automatically. A follow-up spec covers it.
- DESTROY_PLAYER objective filtering for absent colors (P2-5 in `docs/analysis/02-mechanics-gap-analysis.md:424`) is OUT OF SCOPE of this spec — it touches setup (`ClassicGameDist.distributeObjectiveCards`) more than mechanics. A follow-up spec covers it. US4's acceptance scenario #10 calls this out.
- The TABELA II cumulative-prize curve confirmation (P2-9 per `docs/analysis/02-mechanics-gap-analysis.md:428`) is OUT OF SCOPE of this spec. The current `CardExchangeState.incrementExchangeCount` cadence ( +2 while < 10, then +5 ) is retained until the canonical board printing is confirmed.
- The `CardExchangeEvaluator` dead-code duplicate (P3-6 per `docs/analysis/02-mechanics-gap-analysis.md:441`) is OUT OF SCOPE. Deletion is a hygiene task, not a mechanics fix.
- The "distributor decided by dice roll" cosmetic from Manual §2.3 (P3-4 per `docs/analysis/02-mechanics-gap-analysis.md:439`) is OUT OF SCOPE. The digital implementation already shuffles randomly.
- Player-chosen color (P3-3 per `docs/analysis/02-mechanics-gap-analysis.md:438`) is OUT OF SCOPE. Random assignment by `ShufflerUtil.shuffleColors` is retained.
- Objective card "destroy black/white" naming versus the code's "purple/gray" palette (P3-7 per `docs/analysis/02-mechanics-gap-analysis.md:442`) is a content decision OUT OF SCOPE of this spec. The current palette is retained.
- Per Constitution Principle V (Simplicity & YAGNI), no speculative abstractions are introduced. The Move phase is implemented as a focused service with the minimum state needed (a `Set<EClassicCountries>` per player or per-country counter — implementer's choice). No premature generalization to "any phase transition" framework.
- All changes preserve the constitution's Tomcat exclusion (`.specify/memory/constitution.md:91`) and the package root `br.com.bnuuy.jwar` (`.specify/memory/constitution.md:88`).
- Git operations (commit, push, PR) are performed manually by the user per `CLAUDE.md` "Git Workflow". No automated agent commits.
- The user accepts that Manual §10.5 ("As cartas trocadas são colocadas à parte do jogo") explicitly contradicts the current `ExchangeCardsEvaluator.returnCardsToDeckAndShuffle` implementation. The fix in FR-035 takes the manual as authoritative.
- The user accepts that Manual §11 ("faz-se um sorteio") explicitly mandates random sampling for the elimination over-5 case. The fix in FR-037 takes the manual as authoritative. The Monte Carlo uniformity bound in SC-014 (±2 percentage points across 10,000 trials) is achievable with a standard PRNG.
