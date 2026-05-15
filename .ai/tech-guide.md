# jWar — Technical Guide

> Primary technical reference for new developers and AI coding assistants
> working on the `Spring_jWar_game` codebase.

---

## 1. Purpose & Scope

This document is the **engineering onboarding reference** for the jWar
codebase. It explains *how the code is organized*, *which patterns it
uses*, and *why* — with concrete `file:line` pointers so that claims can
be verified directly against the source.

Audience:

- **New developers** joining the project who need a fast, accurate mental
  model of the engine.
- **AI assistants** (Claude Code, Copilot, etc.) that need a reliable
  context document to ground their suggestions in the real architecture.

Related documents — read in this order:

| Document | Purpose |
|---|---|
| [`CLAUDE.md`](../CLAUDE.md) | Quick-start cheat sheet, conventions, commands, permissions for AI agents. |
| [`.specify/memory/constitution.md`](../.specify/memory/constitution.md) | The five inviolable project principles (v1.0.0). Cited throughout this guide. |
| **This file** (`.ai/tech-guide.md`) | Long-form architectural reference. |

If `CLAUDE.md` and this guide disagree, **the constitution wins**, then
`CLAUDE.md`, then this guide.

---

## 2. Project Overview

**jWar** (a.k.a. `bnuuy-war`) is a digital implementation of the classic
Brazilian board game **"War"** — the Grow (1980s) localization of Risk.
It is a turn-based, multi-player strategy game.

| Field | Value |
|---|---|
| **Author** | Fernando H. Gomes |
| **Organization** | `br.com.bnuuy` |
| **Package root** | `br.com.bnuuy.jwar` |
| **License** | MIT (see [`LICENSE`](../LICENSE)) |
| **Players per match** | 3 – 6 |
| **Territories** | 42 |
| **Continents** | 6 (South America, North America, Europe, Africa, Oceania, Asia) |
| **Domain language** | Brazilian Portuguese (game text and error messages) |
| **Code language** | English identifiers, PT-BR strings |

The game faithfully reproduces the original rules (see Constitution
**Principle III — Domain Fidelity**), including the special first-two-rounds
troop distribution and the escalating card-exchange prize.

---

## 3. Tech Stack

All versions are pinned in
[`jwar-server/gradle.properties`](../jwar-server/gradle.properties).

| Concern | Tech | Version | Notes |
|---|---|---|---|
| Language | Java | **17** | `sourceCompatibility = VERSION_17` ([`build.gradle:31-32`](../jwar-server/build.gradle)); no preview features. |
| App framework | Spring Boot | **3.3.5** | Web server is **Undertow** — Tomcat is excluded project-wide ([`build.gradle:17-20`](../jwar-server/build.gradle)). |
| Build | Gradle | multi-module | Two subprojects: `jwarsv-core`, `jwarsv-sboot` ([`settings.gradle`](../jwar-server/settings.gradle)). |
| ORM | Hibernate | **6.6.2** | Plugin `org.hibernate.orm` 6.6.2.Final. |
| Data access | Spring Data JPA + REST | (BOM) | Used in both core and sboot. |
| DB (prod) | PostgreSQL | **42.7.4** | `runtimeOnly` in sboot. |
| DB (dev/test) | H2 | (BOM) | `runtimeOnly` in sboot. |
| Migrations | Flyway | **10.21.0** | `classpath:db.migration` ([`application.yml:40-42`](../jwar-server/jwarsv-sboot/src/main/resources/application.yml)). |
| Modularity | Spring Modulith | **1.2.5** | BOM imported and starters declared, **but no `@ApplicationModule` annotations yet** — refactor opportunity. |
| AuthN | Firebase Admin | **9.4.1** | Netty exclusion pinned for compatibility. |
| Metrics | Micrometer + Prometheus | (BOM) | Tracing bridge: Brave. |
| Error reporting | Sentry | **6.30.0** | `sentry-spring-boot-starter-jakarta`. |
| Payments (cards) | Stripe SDK | **28.3.0** | Configured in `app.providers.stripe`. |
| Payments (PIX) | StarkBank SDK | **2.13.0** | Configured in `app.providers.stark_bank`. |
| Codegen | Lombok | **1.18.30** | `@Getter/@Setter/@Slf4j/@Builder` widely used. Constitution forbids `@Data` on JPA entities. |
| Mapping | MapStruct | **1.5.5.Final** | Annotation processor. |
| CSV / JSON | OpenCSV **5.8**, Jackson **2.14.0** | | |
| HTTP client | OpenFeign (Spring Cloud) | **2023.0.3** | `commons-io` excluded to avoid collision. |
| Testing | JUnit 5 / Mockito | **5.10.0 / 5.6.0** | JUnit Vintage engine also present. |
| Native image | GraalVM Buildtools | **0.10.3** | `bootBuildImage` target supported. |

---

## 4. Module Architecture

```
            ┌──────────────────────────────────────┐
            │            jwarsv-sboot              │
            │  (Spring Boot app, web, persistence) │
            │  - JWarBackCoreApplication.java      │
            │  - application.yml                   │
            │  - Flyway, Undertow, Actuator,       │
            │    Sentry, Stripe, StarkBank,        │
            │    Firebase wiring                   │
            └────────────────┬─────────────────────┘
                             │  depends on
                             ▼
            ┌──────────────────────────────────────┐
            │            jwarsv-core               │
            │       (pure game engine)             │
            │  - br.com.bnuuy.jwar.core.*          │
            │  - NO Spring Web                     │
            │  - Spring Data JPA + Firebase only   │
            │    for User domain entity            │
            └──────────────────────────────────────┘
                             │
                             ▼
            ┌──────────────────────────────────────┐
            │            jwar-commons              │
            │   (placeholder, currently empty)     │
            └──────────────────────────────────────┘
```

### Responsibilities

| Module | Owns | Forbids |
|---|---|---|
| `jwarsv-core` | Game rules, domain entities, enums, validators, evaluators, dice math, deck shuffling, distribution. | **No Spring Web** (Constitution **Principle I — Game Logic Isolation**). |
| `jwarsv-sboot` | The Spring Boot entry point, REST endpoints (future), persistence config, Flyway, integrations (Stripe/StarkBank/Firebase), observability. | Game-rule logic. |
| `jwar-commons` | Shared cross-cutting utilities (currently empty). | — |

### Dependency direction

**`sboot → core`** only. Never the reverse. Enforced by Gradle:
[`jwarsv-sboot/build.gradle:17`](../jwar-server/jwarsv-sboot/build.gradle)
declares `implementation project(":jwarsv-core")`. Constitution
**Principle IV — Modular Architecture** mandates this inward flow.

### Spring Modulith — declared but unused

The Modulith BOM and starters are present
([`jwarsv-sboot/build.gradle:42-43`](../jwar-server/jwarsv-sboot/build.gradle)),
but no `@ApplicationModule` or `package-info.java` declarations exist
yet. **This is a refactor opportunity**: once the core is split into
sub-domains (lobby, match, scoring, payments), each should be annotated
to let Modulith verify the boundaries at compile/test time.

---

## 5. Package Layout

Annotated tree of the game engine
(`jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/`):

```
core/
├── domain/
│   └── User.java                                  # JPA user entity (only Spring Data JPA usage in core)
├── exceptions/
│   └── GameRulesException.java                    # Single domain exception, raised by validators
└── game/
    ├── ClassicGame.java                           # 5.1  Facade / orchestrator (holds match state)
    ├── ClassicGameAttacker.java                   # 5.2  Static dice-rolling and per-die combat math
    ├── ClassicGameAttackResProcessor.java         # 5.3  Post-attack state mutations (losses, conquest, card transfer)
    ├── ClassicGameConstants.java                  # 5.4  Turn-phase ints, max cards, exchange thresholds
    ├── ClassicGameLobby.java                      # 5.5  Pre-match player registration
    ├── ClassicGamePActions.java                   # 5.6  Player-facing action dispatcher (validate → mutate)
    ├── domain/                                    # In-memory match entities (NOT JPA)
    │   ├── AttackResultVO.java                    # @Builder VO returned by ClassicGameAttacker.attack(...)
    │   ├── ClassicGameContinent.java
    │   ├── ClassicGameCountry.java
    │   └── ClassicGamePlayer.java
    ├── map/                                       # 7 enums modelling the board
    │   ├── ContinentCountriesUtil.java
    │   ├── CountriesBordersUtil.java              # Border adjacency lookup
    │   ├── ECardShape.java                        # circle / triangle / square / joker
    │   ├── EClassicContinents.java                # 6 continents
    │   ├── EClassicCountries.java                 # 42 countries
    │   ├── EClassicCountryCard.java               # Country card (country + shape)
    │   ├── EGameColors.java                       # 6 colors with PT-BR display names
    │   ├── EObjectiveCard.java                    # Concrete objective definitions
    │   └── EObjectiveType.java                    # Objective category (used as Strategy key)
    └── utils/
        ├── CardExchangeState.java                 # Mutable counter + escalating prize
        ├── ClassicGameDist.java                   # Distribution (countries, troops, cards, colors)
        ├── ClassicGameValidator.java              # Static validators (all throw GameRulesException)
        ├── EndGameEvaluator.java                  # Strategy registry for objective evaluation
        ├── ExchangeCardsEvaluator.java            # Card-trio validation + troop calculation
        ├── ShufflerUtil.java                      # Deck shuffling helper
        └── objective/
            ├── ObjectiveEvaluator.java            # Strategy interface
            ├── ContinentObjectiveEvaluator.java   # "Conquer continents X + Y"
            ├── DestroyPlayerObjectiveEvaluator.java # "Destroy player of color X"
            └── TerritoryObjectiveEvaluator.java   # "Conquer N territories" (± min troops)
```

The Spring Boot module is intentionally tiny:

```
jwarsv-sboot/
└── src/main/
    ├── java/br/com/bnuuy/jwar/server/
    │   └── JWarBackCoreApplication.java           # @SpringBootApplication entry point
    └── resources/
        └── application.yml                        # All runtime config
```

---

## 6. Game Domain Model

| Game concept | Java type | File |
|---|---|---|
| Match / game instance | `ClassicGame` | [`ClassicGame.java`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java) |
| Match ID | `UUID matchId` | [`ClassicGame.java:59`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java) |
| Player (in-memory) | `ClassicGamePlayer` | `game/domain/ClassicGamePlayer.java` |
| Territory | `ClassicGameCountry` | `game/domain/ClassicGameCountry.java` |
| Continent | `ClassicGameContinent` | `game/domain/ClassicGameContinent.java` |
| Color | `EGameColors` (PT-BR: `Cinza/Amarelo/Vermelho/Verde/Roxo/Blue`) | [`EGameColors.java:7-12`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/map/EGameColors.java) |
| Country reference | `EClassicCountries` (42 entries) | `game/map/EClassicCountries.java` |
| Continent reference | `EClassicContinents` (6 entries) | `game/map/EClassicContinents.java` |
| Country card | `EClassicCountryCard` (country + `ECardShape`) | `game/map/EClassicCountryCard.java` |
| Card shape | `ECardShape` | `game/map/ECardShape.java` |
| Objective card | `EObjectiveCard` | `game/map/EObjectiveCard.java` |
| Objective category | `EObjectiveType` (5 types) | `game/map/EObjectiveType.java` |
| Attack outcome | `AttackResultVO` | [`game/domain/AttackResultVO.java:13-35`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/AttackResultVO.java) |
| Turn phase | `int` (1/2/3) | [`ClassicGameConstants.java:4-6`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameConstants.java) |
| Card-exchange counter | `CardExchangeState` | [`CardExchangeState.java:11-49`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/CardExchangeState.java) |
| Persistent user | `User` (JPA) | `core/domain/User.java` |

> **Important:** the per-match state — players, countries, continents,
> card deck — is **in-memory only** (Java `HashMap`s, see
> [`ClassicGame.java:33-48`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java)).
> Only the `User` entity is persisted via JPA. Match persistence/replay
> is a future concern.

### Naming conventions (Constitution Technology Constraints)

- Enums: prefix `E` — `EClassicCountries`, `EGameColors`.
- Value Objects: suffix `VO` — `AttackResultVO`.
- Domain text and error messages: **Brazilian Portuguese**, verbatim
  (e.g., `"Não é possível atacar um país que te pertence"`).

---

## 7. Design Patterns in the Codebase

### 7.1 Facade / Orchestrator — `ClassicGame`

**Where:** [`ClassicGame.java:28-258`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java)

**What:** A single class owns the entire match aggregate (players,
countries, continents, continent ownership, color index, attack
processor, end-game evaluator, exchange evaluator, deck, turn phase,
first/second-round flags, match UUID) and exposes high-level operations:

| Method | Behavior |
|---|---|
| `startMatch(List<ClassicGamePlayer>)` ([line 97](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java)) | Validates lobby, builds deck, instantiates evaluators, distributes everything. |
| `turnToNextPlayer()` ([line 146](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java)) | Checks win, draws card if conquest, distributes round troops, advances `currentPlayer`. |
| `attack(srcCountry, tgtCountry)` ([line 221](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java)) | Delegates to `ClassicGameAttacker`, lets `ClassicGameAttackResProcessor` mutate state, checks win. |
| `endTurnAddPhase()` / `endTurnAttackPhase()` | Phase transitions (`ADD → ATTACK → MOVE`). |
| `exchangeCards(player, codes)` | Delegates to `ExchangeCardsEvaluator`. |

**Why:** Centralizes the aggregate root so that callers (currently
`ClassicGamePActions`, in future REST controllers) get one cohesive
entry point. Aligns with Constitution **Principle V — Simplicity &
YAGNI**: no premature service layering.

### 7.2 Strategy + Registry — Objective evaluation

**Interface:**
[`ObjectiveEvaluator.java:9-17`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/objective/ObjectiveEvaluator.java)

```java
boolean hasCompletedObjective(ClassicGamePlayer player, EObjectiveCard objective);
```

**Concrete strategies** in `core/game/utils/objective/`:

- `TerritoryObjectiveEvaluator` — "conquer N territories" with/without min-troops constraint.
- `ContinentObjectiveEvaluator` — "conquer continents X, Y[, + any other]".
- `DestroyPlayerObjectiveEvaluator` — "destroy the player of color C".

**Registry / dispatcher:**
[`EndGameEvaluator.java:20-43`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/EndGameEvaluator.java):

```java
evaluators.put(EObjectiveType.CONQUER_TERRITORIES, new TerritoryObjectiveEvaluator());
evaluators.put(EObjectiveType.CONQUER_TERRITORIES_WITH_TROOPS, new TerritoryObjectiveEvaluator());
evaluators.put(EObjectiveType.CONQUER_CONTINENTS, new ContinentObjectiveEvaluator(continentOwners));
evaluators.put(EObjectiveType.CONQUER_CONTINENTS_PLUS_ONE, new ContinentObjectiveEvaluator(continentOwners));
evaluators.put(EObjectiveType.DESTROY_PLAYER, new DestroyPlayerObjectiveEvaluator(playersByColor, playersById));
```

`hasPlayerWon(player)` ([line 51](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/EndGameEvaluator.java))
reads the player's objective, looks up the evaluator by
`EObjectiveType`, and delegates.

**To add a new objective type:**

1. Add a new constant to `EObjectiveType`.
2. Create concrete `EObjectiveCard` entries that reference it.
3. Implement `ObjectiveEvaluator` in `core/game/utils/objective/`.
4. Register it in `EndGameEvaluator`'s constructor map.
5. Add unit tests (Constitution **Principle II — Test-Driven Game Rules**).

No other code needs to change — this is textbook Open/Closed.

### 7.3 Value Object with Builder — `AttackResultVO`

**Where:**
[`AttackResultVO.java:13-35`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/domain/AttackResultVO.java)

```java
@Builder
@Getter
@ToString
@AllArgsConstructor
public class AttackResultVO {
    private final int srcCountry;
    private final int targetCountry;
    private final int[] attackers;
    private final int[] defense;
    @Builder.Default @Setter private int srcCountryLoss = 0;
    @Builder.Default @Setter private int targetCountryLoss = 0;
    @Builder.Default @Setter private boolean conquered = false;
    @Builder.Default @Setter private boolean playerDestroyed = false;
}
```

**Why:** captures the immutable inputs of an attack (dice rolls and
country IDs) plus the four mutable outcomes set by
`ClassicGameAttackResProcessor`. The `VO` suffix is mandated by the
constitution.

### 7.4 Static Validator — `ClassicGameValidator`

**Where:**
[`ClassicGameValidator.java:16-92`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java)

**What:** A collection of **independent static `public static void` methods**, each
checking one precondition and throwing `GameRulesException` (with a
**PT-BR message**) when violated. Examples:

| Method | Throws when |
|---|---|
| `validatePlayersToStart(List)` | < 3 or > 6 players, or duplicates. |
| `isCountryOwner(srcPlayer, country)` | Player doesn't own the source country. |
| `playerHasAvailableTroopsToAdd(player, qty)` | Player wants to add more troops than available. |
| `continentHasAvailableTroopsToAdd(continent, qty)` | Continent-bonus troop pool insufficient. |
| `isMyTurn(srcPlayer, currentPlayer)` | Action outside the player's turn. |
| `countryCanBeTarget(src, target)` | Same owner, or no shared border (via `CountriesBordersUtil.hasBorder`). |
| `countryHasAttackTroops(country)` | Country has < 2 troops (cannot attack with a single soldier). |
| `isAddPhase` / `isAttackPhase` / `isMovePhase` | Action in wrong phase. |

**Note — not a Chain of Responsibility.** Each method is called
**explicitly** by the action layer via `import static` (see
[`ClassicGamePActions.java:3-10`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java)).
There is no shared validator pipeline — the call site decides the order
and which validators apply. This is intentional simplicity (Principle V).

### 7.5 Mutable State Object — `CardExchangeState`

**Where:**
[`CardExchangeState.java:11-49`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/CardExchangeState.java)

Unlike `AttackResultVO`, this object is **intentionally mutable**. It
tracks two fields that escalate over the match lifetime:

- `exchangeCount` — number of card exchanges so far.
- `currentPrize` — troops awarded on the *next* exchange.

`incrementExchangeCount()` returns the prize the player *just* earned,
then increments `currentPrize` using thresholded steps from
`ClassicGameConstants`:

```
INITIAL_EXCHANGE_TROOPS         = 4
EXCHANGE_INCREMENT_UNTIL_THRESHOLD = 2    // +2 per exchange until ...
EXCHANGE_INCREMENT_THRESHOLD       = 10   // ... currentPrize hits 10, then ...
EXCHANGE_INCREMENT_AFTER_THRESHOLD  = 5   // ... +5 per exchange.
```

`reset()` is called from `ClassicGame.startMatch()`
([line 103](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java))
so the state is per-match.

### 7.6 Action Dispatcher — `ClassicGamePActions`

**Where:**
[`ClassicGamePActions.java:23-123`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java)

Players never call `ClassicGame` directly. Instead they go through
`ClassicGamePActions`, which wraps each action in a **validate → mutate**
sequence:

```java
public void attack(int srcPlayer, int srcCountryId, int tgtCountryId) {
    isMyTurn(srcPlayer, classicGame.getCurrentPlayer());
    isAttackPhase(classicGame.getTurnPhase());

    ClassicGameCountry srcCountry = classicGame.getCountry(srcCountryId);
    isCountryOwner(srcPlayer, srcCountry);
    countryHasAttackTroops(srcCountry);

    ClassicGameCountry tgtCountry = classicGame.getCountry(tgtCountryId);
    countryCanBeTarget(srcCountry, tgtCountry);

    AttackResultVO attackRes = classicGame.attack(srcCountry, tgtCountry);
    // logging ...
}
```

It is **not a Command pattern** (no undo/redo, no command objects) —
just a thin, explicit dispatcher. Adding new player actions = adding a
new method here.

### 7.7 Enum-Driven Type System

The board is modelled entirely in `core/game/map/` enums (no JPA, no
database tables). All seven enums are prefixed `E` per constitution
convention:

| Enum | Role |
|---|---|
| `EClassicCountries` | The 42 territories. |
| `EClassicContinents` | The 6 continents. |
| `EClassicCountryCard` | Country cards (territory + shape). |
| `ECardShape` | Card shape (circle / triangle / square / joker). |
| `EGameColors` | 6 player colors with PT-BR display names (`Cinza`, `Amarelo`, `Vermelho`, `Verde`, `Roxo`, `Blue`). |
| `EObjectiveCard` | Concrete objective cards. |
| `EObjectiveType` | Objective category — the dispatch key for Strategy. |

Helper enum-adjacent classes:

- `CountriesBordersUtil` — adjacency lookup (`hasBorder(a, b)`).
- `ContinentCountriesUtil` — continent ↔ countries mapping.

### 7.8 Turn Phases as Integer Constants

**Where:**
[`ClassicGameConstants.java:4-6`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameConstants.java)

```java
public static final int TURN_PHASE_ADD    = 1;
public static final int TURN_PHASE_ATTACK = 2;
public static final int TURN_PHASE_MOVE   = 3;
```

Checked via guards (`isAddPhase`/`isAttackPhase`/`isMovePhase` in the
validator). **Deliberately not a State pattern object hierarchy** —
constitution Principle V (Simplicity & YAGNI) discourages building a
class hierarchy when three int constants suffice. If phase-specific
behavior multiplies, this becomes a candidate refactor target (extract
to an enum `ETurnPhase` first; only then consider polymorphism).

---

## 8. Lifecycle of a Match

```
┌───────────────────────────────────────────────────────────────┐
│ 1.  Lobby   ClassicGameLobby.joinLobby(player)                │
│             - rejects when full (>= 6) or duplicate           │
│             - lobby.startMatch() → ClassicGame.startMatch()   │
└────────────────────────────┬──────────────────────────────────┘
                             ▼
┌───────────────────────────────────────────────────────────────┐
│ 2.  Setup   ClassicGame.startMatch(lobbyPlayers)              │
│             - validatePlayersToStart (3..6, unique)           │
│             - cardExchangeState.reset()                       │
│             - ClassicGameDist:                                │
│                 initializeRoundCardsDeck                      │
│                 initializeContinents                          │
│                 distributeSeq        (turn order)             │
│                 distributeColors                              │
│                 distributeObjectiveCards                      │
│                 distributeCountries  (42 ÷ N players)         │
│                 initializeContinentOwners                     │
│             - Instantiate EndGameEvaluator,                   │
│               ClassicGameAttackResProcessor,                  │
│               ExchangeCardsEvaluator                          │
│             - currentPlayer = 1; turnToNextPlayer()           │
└────────────────────────────┬──────────────────────────────────┘
                             ▼
┌───────────────────────────────────────────────────────────────┐
│ 3.  First two rounds (special troop distribution)             │
│     ClassicGameDist.distributeFirstRoundsTroops(qtdPlayers,   │
│                                                  player)      │
│     - firstRound and secondRound flags flip in                │
│       turnToNextPlayer() at end of each full cycle            │
└────────────────────────────┬──────────────────────────────────┘
                             ▼
┌───────────────────────────────────────────────────────────────┐
│ 4.  Normal turn cycle (per player, per round)                 │
│                                                               │
│     ADD phase (TURN_PHASE_ADD = 1)                            │
│       - addTroops / addContinentTroops                        │
│       - exchangeCards (if >= 3 cards & valid combo)           │
│       - endCurrentTurnAddPhase  → ATTACK                      │
│                                                               │
│     ATTACK phase (TURN_PHASE_ATTACK = 2)                      │
│       - attack(...) →                                         │
│            ClassicGameAttacker (dice rolls)                   │
│            ClassicGameAttackResProcessor (apply losses,       │
│                                            conquest, card     │
│                                            transfer, player   │
│                                            destruction)       │
│       - EndGameEvaluator.hasPlayerWon(attacker) on conquest   │
│       - endCurrentTurnAttackPhase → MOVE                      │
│                                                               │
│     MOVE phase (TURN_PHASE_MOVE = 3)                          │
│       - troop movement between owned, bordering countries     │
│                                                               │
│     endCurrentTurn → turnToNextPlayer()                       │
│       - hasConqueredCountryThisTurn → drawCardForPlayer       │
│       - distributeRoundTroops (territories ÷ 2 + continent    │
│                                bonuses)                       │
│       - EndGameEvaluator.hasPlayerWon at start of next turn   │
└───────────────────────────────────────────────────────────────┘
```

Key entry points: `ClassicGameLobby.joinLobby`, `ClassicGameLobby.startMatch`,
then everything routes through `ClassicGamePActions`.

---

## 9. Build & Run

All commands run from the `jwar-server/` directory.

```bash
# Clean
./gradlew clean

# Build + tests
./gradlew build

# Run unit tests only
./gradlew test

# Run the app locally (dev profile)
./gradlew bootRun -Dspring.profiles.active=dev

# Build a container image
./gradlew clean bootBuildImage

# Print the project version
./gradlew printAppVersion
```

The active Spring profile is sourced from
`springProfile` in [`gradle.properties`](../jwar-server/gradle.properties)
and wired into `bootRun` at
[`jwarsv-sboot/build.gradle:73-75`](../jwar-server/jwarsv-sboot/build.gradle).

### Test layout

Tests live alongside core in
`jwar-server/jwarsv-core/src/test/java/...`. Current tests:

- `ClassicGameTest`
- `ClassicGameDistSeqColorTest`
- `ClassicGameDistObjectivesTest`
- `ClassicGameDistCountriesTest`
- `utils/ExchangeCardsEvaluatorTest`

Constitution **Principle II — Test-Driven Game Rules** requires every
new rule to land with unit tests in JUnit 5 + Mockito.

---

## 10. External Integrations

All third-party bindings live in **`jwarsv-sboot`**, never in core
(Constitution Principle I).

| Integration | Where it binds | Config key (in `application.yml`) |
|---|---|---|
| **Firebase Auth** | `jwarsv-sboot` (`com.google.firebase:firebase-admin`) | `app.migration.*` (sync flags) |
| **Stripe** (credit/debit) | `jwarsv-sboot` (Stripe SDK 28.3.0) | `app.providers.stripe.*` (account, key, API version, webhook secret) |
| **StarkBank PIX** | `jwarsv-sboot` (StarkBank SDK 2.13.0) | `app.providers.stark_bank.*` |
| **Sentry** | `jwarsv-sboot` (`sentry-spring-boot-starter-jakarta`) | (env-var driven) |
| **Prometheus** | `jwarsv-sboot` (`micrometer-registry-prometheus`) | `management.endpoints.web.exposure.include` |
| **SMTP mail** | `jwarsv-sboot` (`spring-boot-starter-mail`) | `spring.mail.*` |
| **OpenFeign** | `jwarsv-sboot` (Spring Cloud) | — |

Sensitive values come from environment variables with safe-default
fallbacks (e.g., `${JWARSV_APP_STRIPE_KEY:abc}` at
[`application.yml:87`](../jwar-server/jwarsv-sboot/src/main/resources/application.yml)).
**Never commit real secrets.**

---

## 11. Database & Migrations

| Concern | Value |
|---|---|
| Production DB | PostgreSQL (`postgresql 42.7.4`, `driverClassName: org.postgresql.Driver`) |
| Dev/test DB | H2 (`runtimeOnly` in sboot) |
| JPA `ddl-auto` | **`none`** — schema is owned exclusively by Flyway ([`application.yml:26-27`](../jwar-server/jwarsv-sboot/src/main/resources/application.yml)) |
| Migration tool | Flyway 10.21.0 (`flyway-core` + `flyway-database-postgresql`) |
| Migration location | `classpath:db.migration` ([`application.yml:40-42`](../jwar-server/jwarsv-sboot/src/main/resources/application.yml)) |
| Connection pool | HikariCP — `maxPoolSize: 2`, `maxLifetime: 180s`, `idleTimeout: 50s` |
| Hibernate logging | `show_sql: true`, `format_sql: true` (dev only — turn off in prod) |
| Caching | Caffeine, `maximumSize=500, expireAfterAccess=120s` |

Currently only `User` is JPA-mapped (in `core/domain/`). Match state
persistence is not implemented.

---

## 12. Spec-Kit Workflow

This project uses **[GitHub Spec-Kit](https://github.com/github/spec-kit)**
for spec-driven development.

Layout:

```
.specify/
├── memory/
│   └── constitution.md     # The 5 principles (v1.0.0)
├── templates/
│   ├── plan-template.md
│   ├── spec-template.md
│   └── tasks-template.md
├── scripts/
├── workflows/
└── integrations/
```

The companion **Claude skills** (under `.claude/skills/`) wire the flow
into Claude Code:

| Skill | Purpose |
|---|---|
| `speckit-constitution` | Create/update the project constitution. |
| `speckit-specify` | Turn a natural-language feature description into a spec. |
| `speckit-clarify` | Ask up to 5 targeted clarifying questions and fold the answers back into the spec. |
| `speckit-plan` | Generate design artifacts from the spec. |
| `speckit-tasks` | Generate `tasks.md` with dependency-ordered actionable items. |
| `speckit-taskstoissues` | Push tasks to GitHub Issues. |
| `speckit-analyze` | Non-destructive cross-artifact consistency check. |
| `speckit-checklist` | Generate a custom checklist for the feature. |
| `speckit-implement` | Execute `tasks.md` end-to-end. |

Typical loop: **constitution → specify → (clarify) → plan → tasks → implement → analyze**.

---

## 13. Pointers

- [Constitution](../.specify/memory/constitution.md) — the five principles, technology constraints, governance.
- [CLAUDE.md](../CLAUDE.md) — AI agent quick reference.
- [`build.gradle` (root)](../jwar-server/build.gradle) — Tomcat exclusion, BOMs, subproject conventions.
- [`gradle.properties`](../jwar-server/gradle.properties) — all pinned versions.
- [`application.yml`](../jwar-server/jwarsv-sboot/src/main/resources/application.yml) — runtime configuration.
- [`ClassicGame.java`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGame.java) — the orchestrator. Start reading here.
- [`ClassicGamePActions.java`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGamePActions.java) — player-facing API.
- [`ClassicGameValidator.java`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java) — the canonical list of game-rule preconditions (all in PT-BR).
- [`EndGameEvaluator.java`](../jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/EndGameEvaluator.java) — Strategy registry; extend here to add new objective types.

---

### Living document

When the architecture changes, this guide MUST be updated in the same
change-set. Every concrete claim should keep a `path:line` citation so
that future readers can trust — and re-verify — what is written here.
