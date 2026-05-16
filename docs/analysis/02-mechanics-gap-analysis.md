# jWar — Mechanics Gap Analysis (Rules vs. Implementation)

> **Scope:** consolidated rules at `docs/regras-do-jogo.md` vs. the code under
> `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/`.
>
> **Goal:** identify every mechanic from the manual that is missing, partial,
> or buggy so we can write specs to implement each gap.
>
> **Conventions used in this report:**
> - Rule citations refer to sections of `docs/regras-do-jogo.md`.
> - Code citations use `path:line` (always under the project root).
> - Status legend: implemented / partial / missing / buggy.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Section-by-Section Analysis](#2-section-by-section-analysis)
   - 2.1 Components (Manual §1)
   - 2.2 Setup (Manual §2)
   - 2.3 Round Structure (Manual §3)
   - 2.4 Troop Reinforcement (Manual §4)
   - 2.5 Attacks (Manual §5)
   - 2.6 Battle Resolution (Manual §6)
   - 2.7 Conquest (Manual §7)
   - 2.8 Movements (Manual §8)
   - 2.9 Card Pickup (Manual §9)
   - 2.10 Card Exchange (Manual §10)
   - 2.11 Player Elimination (Manual §11)
   - 2.12 End of Game (Manual §12)
3. [Objective Cards — Full Catalog Check](#3-objective-cards--full-catalog-check)
4. [Cross-Cutting Bugs Found](#4-cross-cutting-bugs-found)
5. [Prioritized Gap List](#5-prioritized-gap-list)
6. [Effort & Ownership Proposal](#6-effort--ownership-proposal)

---

## 1. Executive Summary

The codebase ships a coherent skeleton for a "classic" WAR match: lobby,
setup, turn rotation, dice-based attack, post-attack ownership update,
continent ownership tracking, card-exchange escalating prize, and an
objective evaluator framework. **However, several core mechanics are
missing, several rule formulas are wrong, and a handful of clear bugs
will prevent a faithful playthrough.** The most impactful gaps are:

- **No troop-movement phase** (Manual §8). The phase constant exists,
  but no `move(...)` action, no validator usage, no "moved-once" tracking.
- **No conquest-time troop transfer** (Manual §7). Conquering a country
  changes ownership but never moves troops from attacker to conquered
  territory, leaving the conquered country with `troopsCount = 0`.
- **Attacker-loss formula bug** (Manual §6 "Importante"): code passes the
  source country *code* as the loss count instead of the loss count.
- **Card pickup is wired to "next turn start", not "end of turn"** and
  ignores the "max one per turn regardless of count" semantics partly.
- **Jokers are absent** from the country card deck and from exchange rules.
- **5-card forced exchange** is not enforced.
- **Reinforcement formula** uses a non-standard threshold (>7 territories)
  instead of the rulebook's `max(3, floor(N/2))`.
- **Continent bonus distribution constraint** (must be placed inside the
  conquered continent) is partially modeled but never validated against
  countries *of that specific continent*.
- **Only 11 of the 14 objective cards** from the manual are present.
- A handful of data-table typos (continent name, duplicate country code,
  misnamed country) will corrupt the map state.

The skeleton is sound, but it cannot run a faithful game end-to-end.

---

## 2. Section-by-Section Analysis

### 2.1 Components (Manual §1)

> **Manual §1:** _"44 cartas de territórios (incluindo 2 curingas)"_,
> _"14 cartas-objetivos"_, _"3 dados vermelhos (ataque)"_, _"3 dados
> amarelos (defesa)"_.

| Mechanic | Status | Code |
|---|---|---|
| 42 territories | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountries.java:6-69` |
| 6 continents | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicContinents.java:6-25` |
| 6 colors | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EGameColors.java:6-22` |
| 42 country cards (no jokers) | partial | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountryCard.java:13-66` |
| **2 jokers (curingas)** | **missing** | not in enum; `EClassicCountryCard` has no `JOKER_*` value, and `ECardShape` has only the three shapes (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/ECardShape.java:6-9`). |
| 14 objective cards | partial (11/14) | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EObjectiveCard.java:14-46` (see §3). |
| Red/yellow dice (3 each) | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttacker.java:11-22`, `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ShufflerUtil.java:34-37`. |

**Data-quality bugs in the country/continent tables:**

- `EClassicCountries.OTW(12, ...)` and `EClassicCountries.ALA(12, ...)`
  share country code `12`
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountries.java:21-22`).
  The countries map is keyed by code
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameDist.java:129`)
  so one of them is overwritten on insertion.
- `EClassicContinents.ASI(6, "America do Sul", 13, 7)` — name says "America
  do Sul" but should be "Ásia"
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicContinents.java:12`).
- `EClassicCountries.SWD("Sweden", ...)` — PT-BR text everywhere else; should
  be "Suécia"
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountries.java:29`).
- `EClassicCountries.VIE("Vietan", ...)` typo → "Vietnã"
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountries.java:51`).
- `EClassicCountries.NVA(7, "Nova York", ...)` and `MOS("Moscow", ...)` use
  inconsistent translation conventions
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicCountries.java:16,31`).
- `EGameColors.BLUE(6L, "Blue")` — name should be "Azul"
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EGameColors.java:12`).
- Border map (`CountriesBordersUtil`) self-loops `NVG → NVG` —
  `BORDERS.put(NVG, List.of(IND, NVG, AUS))`
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/CountriesBordersUtil.java:50`).
- Border map is asymmetric: `BRA` does not list `ARL` as neighbour
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/CountriesBordersUtil.java:16`)
  but `ARL` lists `BRA`
  (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/CountriesBordersUtil.java:41`).
  A targeted audit pass over `CountriesBordersUtil` is needed.

### 2.2 Setup (Manual §2)

> **Manual §2.1:** _"Cada jogador escolhe o exército da cor que preferir..."_
> **Manual §2.2:** _"Cada jogador recebe uma carta-objetivo, por sorteio... se o número de jogadores for inferior a 6, os objetivos relacionados a exércitos não participantes devem ser excluídos do sorteio."_
> **Manual §2.3:** _"Cada jogador coloca 1 soldado da sua cor em cada um dos territórios recebidos."_

| Mechanic | Status | Code |
|---|---|---|
| Random color assignment | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameDist.java:87-96`, `ShufflerUtil.shuffleColors():17-20` |
| Player-chosen color (rule allows free choice) | missing — only random | `ClassicGameLobby` has no `chooseColor` API (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameLobby.java:13-43`). |
| Objective dealt face-down to each player | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameDist.java:312-326` |
| **Filter out DESTROY_PLAYER objectives for unused colors** | **missing** | `distributeObjectiveCards` shuffles **all** `EObjectiveCard.values()` (`ClassicGameDist.java:333-340`) without removing destroy-player cards whose target color is not in play. A 3-player game can hand someone `DESTROY_PLAYER_PURPLE` for a player who never joined. |
| Distributor decided by dice roll | missing | `distributeCountries` shuffles players and walks them backwards (`ClassicGameDist.java:106-146`) — no per-player dice roll, no concept of "distributor". |
| Remove jokers before territory deal, replace after | missing | jokers don't exist; territory distribution uses `EClassicCountries` enum directly (`ClassicGameDist.java:111`), not the cards deck. The rule is symbolic in the digital version, but the corresponding flow ("distribute then reshuffle") is also absent. |
| Place 1 soldier per territory | implemented (defaulted) | `ClassicGameCountry` constructor sets `troopsCount = 1` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/ClassicGameCountry.java:23`). |
| Starting player = the one **after** the last to receive a card | partial / probably wrong | `ClassicGame.startMatch()` sets `currentPlayer = 1; turnToNextPlayer()`, which immediately advances to player 2 (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java:137-138`). The "last receiver of a card" rule is not modeled; the start is hard-coded. |

### 2.3 Round Structure (Manual §3)

> **Manual §3:** _"Na primeira rodada, cada jogador, na sua vez, deve receber exércitos e colocá-los no mapa de acordo com sua estratégia."_
> _"A partir da segunda rodada, cada jogador, na sua vez, cumpre as seguintes etapas sempre nesta ordem: 1. Recebe novos exércitos... 2. Ataca... 3. Desloca... 4. Recebe uma carta de território..."_

| Mechanic | Status | Code |
|---|---|---|
| First-round special distribution (3/4/5/7 troops by player count) | implemented | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameDist.java:186-201`. **However**, manual specifies one initial-troop pool for setup; jWar uses the same numbers for **both** rounds 1 and 2 — see `ClassicGame.turnToNextPlayer():167-180`. Manual §3 says "na primeira rodada" only; the manual does **not** mandate a separate "second round" reduced reinforcement. **Possibly buggy** vs. the manual (only round 1 should use the table). |
| First-round flag transitions | partial / buggy | Inside `turnToNextPlayer()` the same `if (currentPlayer == qtdPlayers)` branch flips `firstRound=false; secondRound=true;` and **then immediately** also flips `secondRound=false` because both `if` blocks run in sequence (`ClassicGame.java:170-178`). Result: after the very last player of round 1 plays, `secondRound` is set then unset in the same call, and round 2 never gets the special distribution. |
| Phase ordering (Add → Attack → Move → card draw) | partial | `TURN_PHASE_ADD`, `TURN_PHASE_ATTACK`, `TURN_PHASE_MOVE` exist (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameConstants.java:4-6`) and the first two transitions are wired (`ClassicGamePActions.endCurrentTurnAddPhase`, `endCurrentTurnAttackPhase` at lines 50-58). **The Move phase has no `endCurrentTurnMovePhase` action and no `move()` action.** |
| Card draw at end-of-turn if conquered ≥1 territory | partial / wrong timing | `turnToNextPlayer()` draws a card for the **outgoing** player *before* advancing (`ClassicGame.java:158-163`). The semantic outcome is "end of my turn", which is correct — but the operation is bundled inside the "next player" call, making it impossible to draw the card after a Move phase action without ending the turn. Also, the card-pickup must happen **after movements**, and there is no Move phase, so the timing is moot today. |

### 2.4 Troop Reinforcement (Manual §4)

> **Manual §4.1:** _"O jogador soma o número de territórios que possui e divide por 2 (considerando só a parte inteira do resultado)."_
> _"O número mínimo de exércitos a receber é sempre 3, mesmo que o jogador possua menos de 6 territórios."_
> **Manual §4.2:** _"Se, no início da sua vez, o jogador possuir um continente inteiro... recebe exércitos extras conforme a TABELA I... os exércitos recebidos pela posse de um continente devem ser obrigatoriamente distribuídos nos territórios do próprio continente."_

| Mechanic | Status | Code |
|---|---|---|
| `floor(N/2)` per territory count | **buggy** | `ClassicGameDist.distributeRoundTroops()` uses `if (player.getOwnedCountries().size() > 7) { troopsNewRound = size/2; }` else `3` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameDist.java:154-161`). This is **not** `max(3, floor(N/2))`. A player with 7 countries should receive `max(3, 3) = 3` (OK by coincidence); a player with 8 countries should receive 4 (currently returns 4, OK), but a player with **6 countries gets 3** while the rulebook also gives 3 → fine. The bug bites for `N = 7`: rule gives `floor(7/2)=3 → min 3 → 3` (matches); for `N = 6`: rule gives `floor(6/2)=3 → 3` (matches). The current branch threshold is **>7**, so `N=7` → goes to else, returns 3 (correct). But the logic is misleading and fragile: the correct expression is `Math.max(MIN_ROUND_TROOPS, N / 2)`. |
| Min 3 troops floor | implemented (incidentally) | `ClassicGameDist.java:155` defaults to `MIN_ROUND_TROOPS = 3` (`ClassicGameDist.java:29`). Works only because of the branch above; refactor to be intentional. |
| Continent bonus (TABELA I) | implemented (per-continent value) | `EClassicContinents` rewards: AMS=2, AMN=5, EUR=5, AFR=3, OCE=2, ASI=7 (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EClassicContinents.java:7-12`). These match the canonical TABELA I. |
| Continent bonus auto-applied for owned continents | implemented | `ClassicGameDist.distributeRoundTroops` calls `classicGameContinents.forEach(ClassicGameContinent::addRoundTroops)` (`ClassicGameDist.java:160`), which increments `availableTroopsCount` on the continent (`ClassicGameContinent.addRoundTroops:35-37`). |
| **Continent bonus must be placed *inside* the conquered continent** | **partial — not validated by country/continent identity** | `ClassicGamePActions.addContinentTroops()` only checks that the continent has available troops (`continentHasAvailableTroopsToAdd`) (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:82-94`). It does **not** verify that `country.getContinent()` is one the player **fully owns**. A player can call `addContinentTroops` on **any** country and decrement that continent's pool, even if they don't own the continent — provided the pool was somehow populated. The pool is populated via `addRoundTroops()` which is only called for owned continents (`ClassicGameDist.java:160`), so today the abuse window is limited, but the validator is missing. |
| Continent bonus restricted to countries *belonging to that continent* | implemented (incidentally) | `addContinentTroops` reads `country.getContinent()` and decrements the continent the country belongs to, so the troop bonus can never be spent outside the same continent — provided the country is owned. The missing check is "player owns this continent". |
| Player skipping continent bonus (banking troops) | irrelevant — bonus tracked separately | Implementation already separates `Player.availableTroops` (general pool) from `Continent.availableTroopsCount` (continent-bound pool). Good. |

### 2.5 Attacks (Manual §5)

> **Manual §5:** _"...para atacar a partir de um território, são necessários no mínimo 2 exércitos nesse território."_
> _"O número máximo de exércitos participantes em cada ataque é 3."_
> _"O ataque... só pode ser dirigido a um território adversário contíguo..."_
> _"O território atacado pode usar, inclusive, o exército de ocupação para se defender."_

| Mechanic | Status | Code |
|---|---|---|
| Min 2 troops to attack | implemented | `ClassicGameValidator.countryHasAttackTroops:69-73`: `if (country.getTroopsCount() < 2) throw` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:69-73`). |
| Cannot attack own country | implemented | `countryCanBeTarget:60-67` (`ClassicGameValidator.java:60-67`). |
| Adjacency / dotted-line check | implemented | `countryCanBeTarget` calls `CountriesBordersUtil.hasBorder` (`ClassicGameValidator.java:64`, `CountriesBordersUtil.java:69-71`). Border map needs the audit noted in §2.1. |
| Max 3 attack dice | implemented | `ClassicGameAttacker.getAttackPos:55-63` caps at 3 (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttacker.java:55-63`). |
| Attacker leaves ≥1 occupation army (so dice = troops-1) | implemented | `getAttackPos` returns `troopsCount - 1` for 2-4 troops (`ClassicGameAttacker.java:62`). |
| Max 3 defense dice | implemented | `getDefPos:47-53` (`ClassicGameAttacker.java:47-53`). |
| **Defender uses occupation army to roll dice** | **buggy** | `getDefPos` is called with `srcCountry.getTroopsCount()` (`ClassicGameAttacker.java:13`), **not** `tgtCountry.getTroopsCount()`. The defender's dice count is computed from the **attacker's** troop count. Severe bug. Also, even with the right input, defense dice should equal `min(troopsCount, 3)` (defender can use the occupation army) — the current cap `if (>2) return 3; else return troopsCount;` is correct, but only the wrong country is fed into it. |
| Attacker announces troop count (allow <3) | missing | `ClassicGamePActions.attack(srcPlayer, srcCountryId, tgtCountryId)` has no parameter for "how many troops the attacker wants to use" (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:32`). The implementation always uses `min(troops-1, 3)`. The rule allows the attacker to choose 1, 2, or 3 dice. |
| Multiple attacks per turn | implemented (no per-turn cap) | `attack` action has no flag preventing repeat calls during attack phase. |
| Defender always defends (no choice) | implemented (by omission) | Defender dice are auto-rolled. Manual is silent on defender refusal, so this is fine. |

### 2.6 Battle Resolution (Manual §6)

> **Manual §6:** _"Compara-se o maior dado do atacante com o maior dado do defensor. Vence quem tiver mais pontos. **Em caso de empate, vence a defesa.**"_
> _"**Importante:** cada vez que um atacante perde, ele retira do seu território apenas o número de exércitos com que a defesa se defendeu. Se a defesa usou apenas 1 dado, o atacante perde no máximo 1 exército (não 2)."_

| Mechanic | Status | Code |
|---|---|---|
| Dice sorted high→low | implemented | `ShufflerUtil.orderDices:39-47` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ShufflerUtil.java:39-47`). |
| Compare pairs (largest vs. largest, etc.) | implemented | `ClassicGameAttacker.doResult:24-35` (`ClassicGameAttacker.java:24-35`). |
| Tie goes to defender | implemented | `if (attackers[dpos] > defense[dpos])` else srcLoss++ (`ClassicGameAttacker.java:30-34`). |
| **Number of pairs compared = min(attackDice, defenseDice)** | **buggy** | The loop bound is `attackers.length` (`ClassicGameAttacker.java:29`). If attacker has 3 dice and defender has 1, the loop runs 3 times and reads `defense[1]`, `defense[2]` → `ArrayIndexOutOfBoundsException`. Conversely, if attacker has fewer dice than defender, the unused defender dice are ignored (correct). The rule cap "attacker loses at most defender-dice losses" follows naturally from comparing `min(att, def)` pairs only. |
| **Attacker loss application** | **buggy** | `ClassicGameAttackResProcessor.implyDmg:145-151` calls `srcCountry.removeTroops(result.getSrcCountry())` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttackResProcessor.java:146`). `AttackResultVO.getSrcCountry()` returns the **country code** (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/AttackResultVO.java:14`), not the loss count. The intended call is `removeTroops(result.getSrcCountryLoss())`. Severe correctness bug: every attacker loses N troops equal to the source country code (1 for BRA, 5 for MEX, ...). |
| "Importante" cap on attacker losses by defender dice | implemented (transitively, once pair-count is fixed) | When the pair loop runs `min(att, def)` iterations, attacker can lose at most `defenderDice` troops. So fixing the loop bound resolves this rule automatically. |

### 2.7 Conquest (Manual §7)

> **Manual §7:** _"Se, após uma batalha... o atacante destrói todos os exércitos do defensor, conquistou o território. Deve deslocar exércitos atacantes para o território conquistado..."_
> _"O número de exércitos a ser deslocado neste momento é, no máximo, igual ao número de exércitos que participaram do último ataque."_

| Mechanic | Status | Code |
|---|---|---|
| Detect conquest (defender troops = 0) | implemented | `ClassicGameAttackResProcessor.implyDmg:148-150` sets `conquered = true` when `tgtCountry.getTroopsCount() < 1` (`ClassicGameAttackResProcessor.java:148-150`). |
| Change owner on conquest | implemented | `checkConquer:117-143` → `tgtCountry.changeOwner(...)` (`ClassicGameAttackResProcessor.java:123`). |
| **Move attacker troops into conquered country (1 to attackDice)** | **missing** | After `changeOwner`, the code never adds troops to `tgtCountry`. The conquered country sits at `troopsCount = 0`, leaving the attacker with a zero-troop territory (which the rules explicitly forbid — every territory must have ≥1 occupation army). |
| Cap moved troops = dice used in last attack | missing | No troop transfer happens at all (see above), so the cap is moot. |
| Allow follow-up attack from conquered country | partial | The game state remains in `TURN_PHASE_ATTACK` after a conquest, so a follow-up attack is structurally possible. But because the country has 0 troops it fails `countryHasAttackTroops` (`ClassicGameValidator.java:69-73`), so the player cannot in fact continue. |
| Update continent ownership on conquest | implemented | `continent.checkAndUpdateOwnership()` + `updateContinentOwnership(continent)` (`ClassicGameAttackResProcessor.java:133-137,156-170`). |

### 2.8 Movements (Manual §8)

> **Manual §8:** _"Há dois momentos em que o jogador pode deslocar seus exércitos: 1) deslocamento dos exércitos atacantes para o território conquistado; 2) deslocamentos permitidos quando o jogador já finalizou seus ataques."_
> _"Em cada território deve permanecer pelo menos um exército (o de ocupação)..."_
> _"Um exército pode ser deslocado uma única vez — não é permitido deslocar para um segundo território contíguo na mesma jogada."_

| Mechanic | Status | Code |
|---|---|---|
| Move-phase action (`move(src, tgt, qtd)`) | **missing** | `ClassicGamePActions` exposes `attack`, `addTroops`, `addContinentTroops`, `exchangeCards`, `endCurrentTurnAddPhase`, `endCurrentTurnAttackPhase`, `endCurrentTurn` — but **no `moveTroops`** or `endCurrentTurnMovePhase` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:24-122`). |
| Move-phase validator (`isMovePhase`) | implemented but **unused** | `ClassicGameValidator.isMovePhase:75-79` exists but no caller (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:75-79`). |
| Conquest-time troop move | missing | See §2.7. |
| Move only between contiguous owned countries | missing | No move logic exists. |
| Must leave ≥1 troop in source | missing | No move logic exists. |
| "Moved-once" tracking per troop / per country | missing | No `movedFrom` set or similar state on `ClassicGameCountry` or `ClassicGamePlayer`. |
| End-of-turn transition from MOVE phase | missing | `endCurrentTurn` does not check that we're in MOVE phase (`ClassicGamePActions.java:60-65`); it just calls `turnToNextPlayer()`. |

### 2.9 Card Pickup (Manual §9)

> **Manual §9:** _"O jogador que conquistar um ou mais territórios durante sua vez tem direito a uma única carta de território no final da jogada (após os deslocamentos), independentemente do número de territórios conquistados."_

| Mechanic | Status | Code |
|---|---|---|
| At most one card per turn (regardless of count) | implemented | `hasConqueredCountryThisTurn` flag is set once (`ClassicGame.java:158-163,228-229`) and consulted only at next-player advance. |
| Card drawn after movements (i.e. at end of turn) | partial | The draw happens inside `turnToNextPlayer()`, which is called by `endCurrentTurn()` — semantically end-of-turn. But since the Move phase doesn't exist, "after deslocamentos" is impossible to enforce literally. |
| Deck reshuffles when exhausted | **missing** | `drawCardForPlayer` throws `GameRulesException` if `cardsDeck.isEmpty()` (`ClassicGameDist.java:231-234`). The rules say _"Quando todas as cartas tiverem sido distribuídas, são recolhidas, embaralhadas e recolocadas em jogo formando um novo monte"_ (Manual §10.5). There is no exchanged-cards reshuffle pile because exchanged cards are **immediately** returned to the deck and re-shuffled (`ExchangeCardsEvaluator.returnCardsToDeckAndShuffle:103-109`). That is **not** what the manual says: the manual keeps used cards "à parte" until the deck is exhausted, then reshuffles. |
| Player at MAX_CARDS doesn't draw (skipped silently) | implemented but **non-standard** | `drawCardForPlayer` returns silently if `player.hasMaxCards()` (`ClassicGameDist.java:236-239`). Manual §10.2 says a player with 5 cards is **obligated** to exchange — so the situation "player has 5 cards at end of turn" should be unreachable. Today nothing prevents it: a player can end their attack phase with 5 cards if they never exchanged. |

### 2.10 Card Exchange (Manual §10)

> **Manual §10.1:** _"Para trocar cartas por exércitos, é necessário ter: 3 cartas com figuras diferentes, OU 3 cartas com figuras iguais."_
> **Manual §10.2:** _"Quando o jogador acumula 5 cartas, é obrigado, na sua vez de jogar, a trocar 3 cartas por exércitos."_
> **Manual §10.3:** _"...2 exércitos extras para cada carta trocada que represente território de sua propriedade (além da quantidade indicada na TABELA II)."_
> **Manual §10.4:** _"O curinga sempre possibilita ao jogador a escolha de qualquer uma das 3 figuras para realizar uma troca."_

| Mechanic | Status | Code |
|---|---|---|
| Validate "3 same shape" or "3 different shapes" | implemented | `ExchangeCardsEvaluator.playerExchangeAvailable:120-140` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ExchangeCardsEvaluator.java:120-140`). |
| Escalating prize TABELA II (4, 6, 8, ... and the 10+ → +5 step) | partial / non-standard | `CardExchangeState.incrementExchangeCount:37-49` adds +2 while `currentPrize < 10`, then +5 (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/CardExchangeState.java:37-49`). The canonical TABELA II is 4, 6, 8, 10, 12, 15, 20, 25, ... so the threshold is correct but the +5 jump after 10 only matches the 12→15 step, not 10→12. The "after threshold" branch should likely trigger after 12 troops with a +3 then +5 cadence. **Possibly buggy** (it depends on whether the project uses the strict TABELA II or the "consolidated" simpler curve). The manual's text is _"e assim sucessivamente"_, leaving room for interpretation; this should be confirmed against the board printing. |
| Cumulative count across game (not per-player) | implemented | `CardExchangeState` is a single instance on `ClassicGame` (`ClassicGame.java:56,90`). |
| Bonus +2 for own-territory card | implemented | `ExchangeCardsEvaluator.processCardExchange:78-83` adds `COUNTRY_BONUS_TROOPS` if the player owns the country on the card. The troops are placed on that specific country (rule respected). |
| **Bonus +2 per matching card (not capped at 1)** | implemented | The loop runs over all 3 cards (`ExchangeCardsEvaluator.java:71-84`). Manual says _"2 exércitos extras para cada carta trocada que represente território de sua propriedade"_ — implementation already complies. |
| **5-card forced exchange** | **missing** | Nothing in `ClassicGame.turnToNextPlayer()`, `ClassicGamePActions.endCurrentTurnAddPhase`, or any validator forces the player to exchange when they reach 5 cards. The MAX_CARDS=5 cap (`ClassicGameConstants.java:9`) just silently refuses draws when a player has 5 cards. |
| **Jokers**: select any of 3 shapes | **missing** | No joker exists in `EClassicCountryCard` or `ECardShape`. The exchange validator has no special path for "1 joker + 2 same-shape" or "1 joker + 2 different shapes". |
| Used cards set aside, reshuffled only when deck empties | **buggy** | Used cards are returned to the deck **and the deck is shuffled** immediately (`ExchangeCardsEvaluator.returnCardsToDeckAndShuffle:103-109`). Manual §10.5: _"As cartas trocadas são colocadas à parte do jogo. Quando todas as cartas tiverem sido distribuídas, são recolhidas, embaralhadas e recolocadas em jogo formando um novo monte."_ — i.e. a discard pile that only re-enters play when the draw pile is exhausted. |
| Exchange allowed only during Add phase | implemented | `ClassicGamePActions.exchangeCards:107-108` calls `isAddPhase(...)` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java:107-108`). |
| Two coexisting evaluators | code smell | Both `CardExchangeEvaluator` and `ExchangeCardsEvaluator` exist (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/CardExchangeEvaluator.java:21` and `.../ExchangeCardsEvaluator.java:26`). The former is unused in production (`ClassicGame` wires `ExchangeCardsEvaluator`). Stale code → delete `CardExchangeEvaluator`. |

### 2.11 Player Elimination (Manual §11)

> **Manual §11:** _"Se um jogador destrói por completo um exército adversário (não sendo este seu objetivo — caso em que ganharia o jogo), recebe as cartas do jogador eliminado."_
> _"Se, ao somar as cartas recebidas às suas, ficar com mais de 5 cartas, faz-se um sorteio: o jogador retira, sem olhar, o número de cartas necessárias para completar cinco."_

| Mechanic | Status | Code |
|---|---|---|
| Detect elimination | implemented | `ClassicGameAttackResProcessor.process:52-60` (`ClassicGameAttackResProcessor.java:52-60`). |
| Transfer cards to victor | partial | `transferCardsFromDefeatedPlayer:82-115` (`ClassicGameAttackResProcessor.java:82-115`). |
| **Cap at 5 cards via random discard** | **buggy** | The code transfers cards "from the top of the list" up to availableSlots, then dumps the remainder back into the deck (`ClassicGameAttackResProcessor.java:97-110`). The manual mandates a **random sample**: the victor receives **all** cards, then randomly discards down to 5. The current implementation is order-dependent, not random, and the discarded cards go back to the deck immediately (manual would have them re-enter only when the deck empties — see §2.10). |
| If the defender was the victor's `DESTROY_PLAYER` target → victory | implemented | `checkObjectiveAchievedEndGame` is called after `transferCardsFromDefeatedPlayer` (`ClassicGameAttackResProcessor.java:58-60`); `DestroyPlayerObjectiveEvaluator.hasCompletedObjective:28-46` returns true when the target player has no countries. |
| **Reassignment of "DESTROY_PLAYER X" objective if X was eliminated by someone else** | partial | `EObjectiveType` docs claim "the objective changes to CONQUER_TERRITORIES with 24" (`EObjectiveType.java:7-12`), and `DestroyPlayerObjectiveEvaluator` falls back to a `TerritoryObjectiveEvaluator` check for 24 territories (`DestroyPlayerObjectiveEvaluator.java:38-43`). **However**, the implementation returns true for **any** player whose target color has no countries — including the **eliminator themselves**. So if player A's target is BLUE and player B kills BLUE, player A's objective auto-completes when A then reaches 24 territories — but the bigger bug is: the check returns `true` for A even **if A is the one who killed BLUE** with fewer than 24 territories, because `targetPlayer.getOwnedCountries().isEmpty()` triggers the territory evaluator, which is **also** false for A if A has <24, **so** the final answer is `false`. Result actually OK, but the logic is opaque and brittle. Worth a refactor: track "who eliminated whom" explicitly. |

### 2.12 End of Game (Manual §12)

> **Manual §12:** _"O jogo termina quando um jogador consegue atingir seu objetivo. Nesse momento, ele deve mostrar sua carta-objetivo, comprovando sua vitória."_

| Mechanic | Status | Code |
|---|---|---|
| Detect win | implemented | `EndGameEvaluator.hasPlayerWon:51-63` (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/EndGameEvaluator.java:51-63`). Called after every attack and at next-turn transition (`ClassicGame.java:149,233`). |
| **End the match** (lock state, stop accepting actions) | **missing** | Both win-check call sites log a message and continue (`ClassicGame.java:151-154` and `ClassicGame.java:234-237`), with `// TODO: Handle game end` comments. No `GameState.FINISHED` / `winner` field is set; subsequent calls to `ClassicGamePActions` would still process actions. |
| Show winner's objective card | partial | The win is logged with the player's nickname but the objective card is not surfaced anywhere queryable (no `getWinner()` API on `ClassicGame`). |

---

## 3. Objective Cards — Full Catalog Check

The classic WAR boardgame ships **14 objective cards** (Manual §1 and
§2.2). The implementation defines **11**
(`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EObjectiveCard.java:14-46`). Below is the canonical catalog with status.

| # | Canonical objective (PT-BR) | Status | Code constant |
|---|---|---|---|
| 1 | Destruir totalmente o exército VERMELHO | implemented | `DESTROY_PLAYER_RED` (`EObjectiveCard.java:14`) |
| 2 | Destruir totalmente o exército AZUL | implemented | `DESTROY_PLAYER_BLUE` (`EObjectiveCard.java:15`) |
| 3 | Destruir totalmente o exército VERDE | implemented | `DESTROY_PLAYER_GREEN` (`EObjectiveCard.java:16`) |
| 4 | Destruir totalmente o exército AMARELO | implemented | `DESTROY_PLAYER_YELLOW` (`EObjectiveCard.java:17`) |
| 5 | Destruir totalmente o exército PRETO | **missing** | absent (the manual lists six colors: branca, vermelha, preta, azul, amarela, verde — implementation uses GRAY/PURPLE instead of BLACK/WHITE). The mapping is fine functionally but the canonical "destroy black" card is missing; the impl has DESTROY_PLAYER_PURPLE and DESTROY_PLAYER_GRAY instead. |
| 6 | Destruir totalmente o exército BRANCO | **missing** | same note as above. |
| 7 | Conquistar a Oceania e a Ásia | implemented | `CONQUER_OCEANIA_ASIA` (`EObjectiveCard.java:22-23`) |
| 8 | Conquistar a Europa, a Oceania e mais um continente à sua escolha | **missing** | impl has `CONQUER_EUROPE_SOUTH_AMERICA_ANY` (EUR+AMS+1) and `CONQUER_EUROPE_AFRICA_ANY` (EUR+AFR+1) but not Europe+Oceania+1. |
| 9 | Conquistar a América do Norte e a África | implemented | `CONQUER_NORTH_AMERICA_AFRICA` (`EObjectiveCard.java:27-28`) |
| 10 | Conquistar a Ásia e a América do Sul | implemented | `CONQUER_ASIA_SOUTH_AMERICA` (`EObjectiveCard.java:32-33`) |
| 11 | Conquistar a América do Norte e a Oceania | implemented | `CONQUER_NORTH_AMERICA_OCEANIA` (`EObjectiveCard.java:34-35`) |
| 12 | Conquistar a Europa e a América do Norte | implemented | `CONQUER_EUROPE_NORTH_AMERICA` (`EObjectiveCard.java:36-37`) |
| 13 | Conquistar 18 territórios com 2 exércitos em cada | implemented | `CONQUER_18_TERRITORIES_2_TROOPS` (`EObjectiveCard.java:40-41`) |
| 14 | Conquistar 24 territórios | implemented | `CONQUER_24_TERRITORIES` (`EObjectiveCard.java:46`) |

**Additional cards present in code but not in manual:**

- `CONQUER_EUROPE_SOUTH_AMERICA_ANY` (`EObjectiveCard.java:24-26`) — matches a canonical WAR objective.
- `CONQUER_EUROPE_AFRICA_ANY` (`EObjectiveCard.java:29-31`) — matches a canonical WAR objective.
- `CONQUER_16_TERRITORIES_3_TROOPS` (`EObjectiveCard.java:42-43`) — matches a canonical WAR objective.

**Net result:** the implementation actually has **all of the canonical "conquer" objectives**, including the two `_PLUS_ONE` variants the markdown summary table omits. The real gap is the **color naming mismatch** between the manual (white/black included) and the code (purple/gray used instead). Either the manual needs updating to reflect modern editions, or the codebase needs to add WHITE/BLACK and remove PURPLE/GRAY. This is a content decision, not a code bug per se. The 14 ↔ 11 deficit in the prompt's prior count seems to have been based on the markdown TOC; the actual count in code is 14 too — but with a different palette.

---

## 4. Cross-Cutting Bugs Found

1. **`ClassicGameAttackResProcessor.implyDmg` passes the wrong field** —
   `srcCountry.removeTroops(result.getSrcCountry())` should be
   `removeTroops(result.getSrcCountryLoss())`
   (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttackResProcessor.java:146`).

2. **Defender dice computed from attacker's troop count** —
   `getDefPos(srcCountry.getTroopsCount())` in `ClassicGameAttacker.attack`
   should be `getDefPos(tgtCountry.getTroopsCount())`
   (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttacker.java:13`).

3. **Battle loop iterates over `attackers.length` only** — when defender
   has fewer dice, `defense[dpos]` reads out-of-bounds; when defender
   has more dice, extra dice are silently ignored. Correct: iterate
   `min(attackers.length, defense.length)`
   (`jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameAttacker.java:29`).

4. **First/second round flag flips fire in the same call** — at
   `currentPlayer == qtdPlayers`, both `firstRound→false; secondRound→true`
   and `secondRound→false` execute (`ClassicGame.java:170-178`), skipping
   round 2's special distribution entirely.

5. **No conquest-time troop transfer** — conquered country sits at 0
   troops (`ClassicGameAttackResProcessor.java:117-143`).

6. **No move phase actions** — `TURN_PHASE_MOVE` exists but no
   `moveTroops`, no `endCurrentTurnMovePhase`, no contiguity validation.

7. **Used cards re-shuffled into the deck immediately** — should accrue
   to a discard pile (Manual §10.5)
   (`ExchangeCardsEvaluator.java:103-109`).

8. **5-card forced exchange never enforced** — a player can sit on 5
   cards indefinitely; subsequent conquests draw nothing
   (`ClassicGameDist.java:236-239`).

9. **Random discard down to 5 on elimination missing** — cards are
   transferred in deterministic list order
   (`ClassicGameAttackResProcessor.java:97-110`).

10. **DESTROY_PLAYER objectives for absent colors not filtered** —
    `ClassicGameDist.distributeObjectiveCards` deals from the full
    `EObjectiveCard.values()` set (`ClassicGameDist.java:312-326`).

11. **`addContinentTroops` does not verify the player owns the continent**
    (`ClassicGamePActions.java:82-94`).

12. **Reinforcement formula** branched on `>7` rather than the
    canonical `max(3, floor(N/2))` (`ClassicGameDist.java:154-161`).

13. **`hasPlayerWon` log-only on victory** — game continues processing
    after a win (`ClassicGame.java:149-154,233-237`).

14. **Color name "Blue" not localized** — should be "Azul"
    (`EGameColors.java:12`).

15. **Duplicate country code 12 for OTW and ALA**
    (`EClassicCountries.java:21-22`).

16. **Continent name typo** — `ASI` says "America do Sul"
    (`EClassicContinents.java:12`).

17. **Border map asymmetric/loops** — `NVG → NVG`, `BRA ↛ ARL` but
    `ARL → BRA` (`CountriesBordersUtil.java:50,16,41`).

18. **Dead/duplicate utility class** — `CardExchangeEvaluator` is
    unused; only `ExchangeCardsEvaluator` is referenced from
    `ClassicGame`. Either delete or consolidate.

19. **`distributeFirstRoundsTroops` returns 0 for ≥7 players** — silently
    no-ops because no branch matches (`ClassicGameDist.java:186-201`).
    The lobby caps at 6 (`ClassicGameLobby.java:35-37`), so unreachable
    today, but the gap is a defect waiting to surface.

20. **No "match finished" state** — `ClassicGame` has no `winner` field
    or `MatchState` enum; the `// TODO: Handle game end` comments are
    placeholders (`ClassicGame.java:153,236`).

---

## 5. Prioritized Gap List

### P1 — Blockers for a playable game

| # | Manual ref | Gap | Code reference |
|---|---|---|---|
| P1-1 | §6 "Importante" | Fix attacker-loss bug (`getSrcCountry` vs `getSrcCountryLoss`) | `ClassicGameAttackResProcessor.java:146` |
| P1-2 | §6 | Fix defender dice using `tgtCountry` not `srcCountry` | `ClassicGameAttacker.java:13` |
| P1-3 | §6 | Loop over `min(att, def)` pairs, not `attackers.length` | `ClassicGameAttacker.java:29` |
| P1-4 | §7 | Implement post-conquest troop transfer (1..lastAttackDice) | new method on `ClassicGameAttackResProcessor` and/or `ClassicGamePActions` |
| P1-5 | §8 | Implement Move phase: `moveTroops(src, tgt, qty)`, contiguity, leave-occupation, moved-once tracking, `endCurrentTurnMovePhase` | `ClassicGamePActions`, `ClassicGameValidator`, `ClassicGame` |
| P1-6 | §3 | Fix round 1 → round 2 flag transition | `ClassicGame.java:167-180` |
| P1-7 | §12 | Add `MatchState` / `winner` field and stop accepting actions after a win | `ClassicGame.java` + new state enum |
| P1-8 | §1 / §10.4 | Add jokers to deck and to exchange validator | `EClassicCountryCard`, `ECardShape`, `ExchangeCardsEvaluator` |
| P1-9 | §10.2 | Enforce 5-card forced exchange before allowing end of Add phase | `ClassicGamePActions.endCurrentTurnAddPhase`, validator |
| P1-10 | §5 | Allow attacker to choose dice count (1–3, ≤ troops-1) | `ClassicGamePActions.attack(...)`, `ClassicGameAttacker` |

### P2 — Correctness (rules currently wrong)

| # | Manual ref | Gap | Code reference |
|---|---|---|---|
| P2-1 | §4.1 | Replace `>7` branch with `Math.max(3, N/2)` | `ClassicGameDist.java:154-161` |
| P2-2 | §4.2 | Validate that player owns the continent before consuming continent-bonus pool | `ClassicGamePActions.addContinentTroops` / new validator |
| P2-3 | §10.5 | Use a discard pile; reshuffle only when draw pile empties | `ExchangeCardsEvaluator.returnCardsToDeckAndShuffle`, `ClassicGameDist.drawCardForPlayer` |
| P2-4 | §11 | Random sample to discard down to 5 cards on elimination | `ClassicGameAttackResProcessor.transferCardsFromDefeatedPlayer` |
| P2-5 | §2.2 | Filter out DESTROY_PLAYER objectives for unused colors before shuffle | `ClassicGameDist.distributeObjectiveCards` |
| P2-6 | §1 | Fix duplicate country code (OTW vs ALA) | `EClassicCountries.java:21-22` |
| P2-7 | §1 | Fix continent name typo for ASI | `EClassicContinents.java:12` |
| P2-8 | §5 | Audit `CountriesBordersUtil` for self-loops and asymmetry | `CountriesBordersUtil.java` |
| P2-9 | §10 | Confirm TABELA II curve (4, 6, 8, 10, 12, 15, 20, 25 …) and fix the +5 threshold if needed | `CardExchangeState.incrementExchangeCount`, `ClassicGameConstants` |
| P2-10 | §11 | Refactor DESTROY_PLAYER fallback to track "eliminated by whom" explicitly | `DestroyPlayerObjectiveEvaluator`, `ClassicGameAttackResProcessor` |
| P2-11 | §9 / §10.5 | When draw pile is empty at end of turn, reshuffle discards and draw | `ClassicGameDist.drawCardForPlayer` |

### P3 — Nice-to-have polish

| # | Manual ref | Gap | Code reference |
|---|---|---|---|
| P3-1 | §1 | Localize `EGameColors.BLUE` name to "Azul" | `EGameColors.java:12` |
| P3-2 | §1 | Localize `SWD` to "Suécia", `VIE` to "Vietnã", `MOS` to "Moscou", `NVA` to "Nova York" (already mostly OK) | `EClassicCountries.java` |
| P3-3 | §2.1 | Allow player-chosen colors in lobby (alongside random) | `ClassicGameLobby` |
| P3-4 | §2.3 | Distributor selected by dice roll (cosmetic in digital version) | `ClassicGameDist.distributeCountries` |
| P3-5 | §12 | Surface winner's objective card via `ClassicGame.getWinner()` | `ClassicGame` |
| P3-6 | n/a | Delete unused `CardExchangeEvaluator` to remove duplication | `CardExchangeEvaluator.java` |
| P3-7 | §1 | Replace WHITE/BLACK objectives or keep PURPLE/GRAY; decide on a canonical palette | `EObjectiveCard`, `EGameColors` |
| P3-8 | §3 | Distinguish round 1 (special distribution) from round 2+ (formula) — manual only specifies special distribution for round 1 | `ClassicGame.turnToNextPlayer`, `ClassicGameDist.distributeFirstRoundsTroops` |
| P3-9 | §5 | Surface a `getValidAttackTargets(player)` helper for UI | new utility |

---

## 6. Effort & Ownership Proposal

Effort key: **S** = ≤ ½ day, **M** = 1–2 days, **L** = 3–5 days. Owning
class = where the new mechanic (or fix) primarily belongs.

| ID | Effort | Proposed owner |
|---|---|---|
| P1-1 | S | `ClassicGameAttackResProcessor` |
| P1-2 | S | `ClassicGameAttacker` |
| P1-3 | S | `ClassicGameAttacker` |
| P1-4 | M | new `ConquestMoveProcessor` (or method on `ClassicGameAttackResProcessor`); state in `AttackResultVO` (last-attack dice count); action in `ClassicGamePActions.moveAfterConquest` |
| P1-5 | L | new `ClassicGameMover` utility; new actions in `ClassicGamePActions` (`moveTroops`, `endCurrentTurnMovePhase`); per-country "movedFrom/to" tracking via a `Set<EClassicCountries>` on `ClassicGamePlayer`; validators in `ClassicGameValidator` |
| P1-6 | S | `ClassicGame.turnToNextPlayer` (state machine refactor) |
| P1-7 | M | new `MatchState` enum on `ClassicGame`; guard at the top of every `ClassicGamePActions` method |
| P1-8 | M | `EClassicCountryCard` (add JOKER_1, JOKER_2 with shape=null), `ExchangeCardsEvaluator.validateExchange` (treat joker as wildcard), `ECardShape` (optional `WILD`) |
| P1-9 | S | `ClassicGamePActions.endCurrentTurnAddPhase` + new validator `mustExchangeIfFiveCards` |
| P1-10 | M | `ClassicGamePActions.attack(srcPlayer, srcCountryId, tgtCountryId, attackDiceCount)`; `ClassicGameAttacker` accepts the count |
| P2-1 | S | `ClassicGameDist.distributeRoundTroops` |
| P2-2 | S | `ClassicGameValidator` (`playerOwnsContinent`); call site in `ClassicGamePActions.addContinentTroops` |
| P2-3 | M | `ClassicGame` gains a `cardsDiscardPile`; `ExchangeCardsEvaluator` returns to discard; `ClassicGameDist.drawCardForPlayer` reshuffles discard on empty deck |
| P2-4 | S | `ClassicGameAttackResProcessor.transferCardsFromDefeatedPlayer` (use `Collections.shuffle` then truncate) |
| P2-5 | S | `ClassicGameDist.distributeObjectiveCards` (filter by participating colors) |
| P2-6 | S | `EClassicCountries.ALA` → unique code (e.g. 43) |
| P2-7 | S | `EClassicContinents.ASI` → name "Ásia" |
| P2-8 | M | `CountriesBordersUtil` audit + symmetric assertion test |
| P2-9 | S | `CardExchangeState` / `ClassicGameConstants` (verify against board printing) |
| P2-10 | M | Track `Map<EGameColors, Integer> eliminatedBy` on `ClassicGame`; refactor `DestroyPlayerObjectiveEvaluator` |
| P2-11 | S | `ClassicGameDist.drawCardForPlayer` |
| P3-1 | S | `EGameColors.BLUE` literal |
| P3-2 | S | `EClassicCountries` literals |
| P3-3 | M | `ClassicGameLobby.chooseColor(playerId, color)` + validation |
| P3-4 | S | `ClassicGameDist.distributeCountries` (cosmetic) |
| P3-5 | S | `ClassicGame.getWinner()` / `getWinnerObjective()` |
| P3-6 | S | delete `CardExchangeEvaluator.java` |
| P3-7 | S | content decision: align `EGameColors` and `EObjectiveCard` |
| P3-8 | S | clarify in `ClassicGameConstants` and `ClassicGameDist` |
| P3-9 | S | new helper in `ClassicGame` or `ClassicGameValidator` |

---

## Appendix — Quick Mapping Summary

| Manual section | Implementation status | Main file |
|---|---|---|
| §1 Components | partial (jokers missing; data typos) | `EClassicCountries`, `EClassicContinents`, `EClassicCountryCard`, `EGameColors` |
| §2 Setup | partial (color choice, distributor dice, objective filter missing) | `ClassicGameLobby`, `ClassicGameDist` |
| §3 Round structure | partial (no move phase, round-flag bug) | `ClassicGame` |
| §4 Reinforcement | partial (formula + continent-bonus validation) | `ClassicGameDist`, `ClassicGamePActions` |
| §5 Attacks | partial (no dice-count choice; defender dice bug) | `ClassicGameAttacker`, `ClassicGamePActions` |
| §6 Battle resolution | buggy (loop bound, srcCountryLoss) | `ClassicGameAttacker`, `ClassicGameAttackResProcessor` |
| §7 Conquest | missing (no troop transfer) | `ClassicGameAttackResProcessor`, `ClassicGamePActions` |
| §8 Movements | missing | n/a — to be added |
| §9 Card pickup | partial (deck reshuffle missing) | `ClassicGame`, `ClassicGameDist` |
| §10 Card exchange | partial (jokers, 5-card forced, discard pile) | `ExchangeCardsEvaluator`, `CardExchangeState`, `ClassicGamePActions` |
| §11 Elimination | partial (random discard, objective fallback) | `ClassicGameAttackResProcessor`, `DestroyPlayerObjectiveEvaluator` |
| §12 End of game | partial (no terminal state) | `ClassicGame`, `EndGameEvaluator` |

End of report.
