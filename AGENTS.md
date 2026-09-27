# AGENTS.md — jWar Game

> Tool-agnostic entry point for AI coding agents (Claude Code, Codex,
> Cursor, Cline, Aider, etc.) working on the `Spring_jWar_game` repository.
>
> Follows the open [agents.md](https://agents.md/) convention. Coexists
> with the Claude-specific [`CLAUDE.md`](./CLAUDE.md) and the deeper
> references under [`.ai/`](./.ai/).

---

## 1. Project at a glance

**jWar** (`bnuuy-war`) is a digital implementation of the classic
Brazilian board game **"War"** — Grow's 1980s localization of Risk.
Turn-based multiplayer strategy game.

| Field | Value |
|---|---|
| Package root | `br.com.bnuuy.jwar` |
| Author | Fernando H. Gomes |
| License | MIT |
| Players | 3–6 |
| Territories | 42 across 6 continents |
| Domain language | **Brazilian Portuguese** (territory names, error messages, card/objective descriptions) |
| Code language | English (identifiers, method names) |

---

## 2. Tech stack

- **Java 17** (no preview features)
- **Spring Boot 3.3.5** with **Undertow** (Tomcat globally excluded)
- **Gradle multi-module** (Groovy DSL)
- **Spring Modulith** (starter on classpath; `@ApplicationModule` annotations not yet applied)
- **Spring Data JPA** · **Hibernate 6.6** · **Flyway 10.x**
- **PostgreSQL** (prod) · **H2** (dev/test)
- **Firebase Admin SDK** (auth) · **Stripe** + **StarkBank** (PIX) · **Micrometer + Prometheus + Sentry**
- **Lombok** · **MapStruct** · **OpenCSV** · **Jackson** · **OpenFeign**
- **JUnit 5 + Mockito** (testing)
- **GraalVM Native Image** support

---

## 3. Repository layout

```
Spring_jWar_game/
├── AGENTS.md                       ← this file (tool-agnostic agent contract)
├── CLAUDE.md                       ← Claude-Code-specific cheat sheet
├── .ai/
│   ├── tech-guide.md               ← architecture + design patterns reference
│   └── tech-best-practices.md      ← project-specific conventions + PR checklist
├── .specify/
│   ├── memory/constitution.md      ← v1.0.0 — five binding principles
│   ├── templates/                  ← Spec-Kit templates
│   └── workflows/speckit/          ← spec → plan → tasks → implement
├── .claude/skills/speckit-*/       ← nine Spec-Kit skills (specify, plan, tasks, etc.)
├── jwar-commons/                   ← shared lib placeholder
└── jwar-server/                    ← main Gradle multi-module server
    ├── jwarsv-core/                ← game engine (NO Spring Web)
    └── jwarsv-sboot/               ← Spring Boot entry point
```

Full tree and module responsibilities: see [`.ai/tech-guide.md`](./.ai/tech-guide.md) §4–5.

---

## 4. Build & run

Run from `jwar-server/`:

```bash
./gradlew clean                                  # clean
./gradlew build                                  # compile + run all tests
./gradlew test                                   # tests only
./gradlew bootRun -Dspring.profiles.active=dev   # run app locally (H2)
./gradlew clean bootBuildImage                   # build OCI image
```

Any code change MUST pass `./gradlew build` before being considered done
(Constitution principle II + Development Workflow).

---

## 5. The five constitution principles (read these before touching code)

Source: [`.specify/memory/constitution.md`](./.specify/memory/constitution.md) v1.0.0.

| # | Principle | One-liner |
|---|---|---|
| I | **Game Logic Isolation** | `jwarsv-core` MUST NOT depend on Spring Web / HTTP. Pure(-ish) Java game engine. |
| II | **Test-Driven Game Rules** | Every game rule needs a JUnit 5 + Mockito test before merge. No exceptions. |
| III | **Domain Fidelity** | Faithfully implement the original Brazilian "War" rules. Domain text in **Portuguese (BR)**. |
| IV | **Modular Architecture** | Dependencies flow inward: `sboot → core`. Never the reverse. |
| V | **Simplicity & YAGNI** | No speculative abstractions. Three similar lines beat a premature framework. |

When these rules conflict with anything else (including this file, `CLAUDE.md`,
or your own judgment), **the constitution wins**.

---

## 6. Design patterns in this codebase

The engine uses concrete, idiomatic patterns. Don't refactor them without
understanding why they're there. Full discussion in [`.ai/tech-guide.md`](./.ai/tech-guide.md) §7.

| Pattern | Where | One-line summary |
|---|---|---|
| **Facade / Orchestrator** | `core/game/ClassicGame.java` | Central game state + high-level operations |
| **Strategy + Registry** | `core/game/utils/objective/` + `EndGameEvaluator` | Pluggable win-condition evaluators dispatched by `EObjectiveType` |
| **Value Object + Builder** | `core/game/domain/AttackResultVO.java` | Immutable attack-outcome carrier |
| **Static Validator** | `core/game/utils/ClassicGameValidator.java` | Independent static methods, PT-BR errors via `GameRulesException`. **Not** a chain. |
| **Mutable state machine** | `core/game/utils/CardExchangeState.java` | Tracks escalating card-exchange prize. **Intentionally mutable.** |
| **Action dispatcher** | `core/game/ClassicGamePActions.java` | Player commands (`attack`, `addTroops`, `endTurn…`). **No undo/redo.** |
| **Enum-driven type system** | `core/game/map/E*.java` | 7 enums modeling the entire board. Game state is in-memory per match. |

---

## 7. Conventions (must-follow)

Full table in [`.ai/tech-best-practices.md`](./.ai/tech-best-practices.md) §3.

- **Naming**: `E*` for enums, `*VO` for immutable Value Objects, `*Validator` / `*Evaluator` / `*Util` / `*Dist` for utilities.
- **Lombok**: `@Getter`/`@Setter`/`@Slf4j` freely. `@Builder` + `@AllArgsConstructor` for VOs. **Never `@Data` on JPA entities** (constitution).
- **Language**: code identifiers in **English**; domain text (errors, country names, descriptions) in **Brazilian Portuguese**. Example: `throw new GameRulesException("Não é possível iniciar um jogo clássico com menos de 3 players")`.
- **Comments**: prefer none; if used, English. Don't narrate WHAT — only non-obvious WHY.
- **No new abstractions** unless they justify themselves against a simpler alternative (Principle V).

---

## 8. How to extend (the high-leverage extension points)

### Add a new objective (Strategy pattern)

1. Add an entry to `core/game/map/EObjectiveType.java`.
2. Implement `core/game/utils/objective/ObjectiveEvaluator` with the new logic.
3. Register it in the `evaluators` map inside `core/game/utils/EndGameEvaluator.java`.
4. Write a unit test in `jwarsv-core/src/test/java/.../utils/objective/` (Principle II).

Do **not** add `if`/`switch` on `EObjectiveType` outside `EndGameEvaluator` — that breaks the pattern.

### Add a new validator

Add a static method to `ClassicGameValidator`. Throw `GameRulesException` with a **Portuguese** message. Call it explicitly from `ClassicGamePActions` (no auto-invocation, no chain-of-responsibility).

### Add a new player action

Add a method to `ClassicGamePActions` that: (1) validates via `ClassicGameValidator`, (2) mutates `ClassicGame` state, (3) optionally returns a VO. Update `ClassicGame` only if a new piece of game-wide state is needed.

### Add a new external integration

Bind it in `jwarsv-sboot` (config, beans, controllers). **Never** import HTTP frameworks into `jwarsv-core`.

---

## 9. Hard rules for AI agents

These rules apply to **every** AI coding agent operating in this repo,
regardless of which tool is invoking it:

1. **Never `git commit`, `git push`, or open pull requests.** All git
   operations are performed manually by the developer. This is explicit
   in `CLAUDE.md` and project policy.
2. **Never reintroduce Tomcat.** It is globally excluded at
   `jwar-server/build.gradle` (`exclude module: 'spring-boot-starter-tomcat'`).
   Undertow is the chosen server.
3. **Never add `spring-boot-starter-web` to `jwarsv-core`.** (Principle I)
4. **Never write English domain text** where Portuguese is expected
   (country names, error messages, objective descriptions, card
   shape labels). (Principle III)
5. **Never merge code without tests** when the change touches a game
   rule (attack dice, distribution, validation, exchange, objective).
   (Principle II)
6. **Never refactor `CardExchangeState` to be immutable** without
   reading the call site in `ClassicGame` first — it's intentionally
   mutable.
7. **Never add JPA `@Entity` mappings to game-board enums.** Game state
   is per-match, in-memory by design.
8. **Don't speculate.** If the constitution doesn't authorize a pattern
   or abstraction and the current code doesn't need it, don't add it.
   (Principle V)

---

## 10. Spec-driven workflow

This project uses [GitHub Spec-Kit](https://github.com/github/spec-kit).
New features SHOULD follow:

```
constitution → /speckit-specify → /speckit-clarify (optional)
            → /speckit-plan → /speckit-tasks → /speckit-implement
            → /speckit-analyze (optional)
```

Skills are installed under `.claude/skills/speckit-*/`. Templates live
in `.specify/templates/`.

---

## 11. Where to look next

| If you need… | Read |
|---|---|
| Quick command cheat sheet + Claude-specific notes | [`CLAUDE.md`](./CLAUDE.md) |
| Full architecture, module responsibilities, design pattern walkthroughs, lifecycle of a match | [`.ai/tech-guide.md`](./.ai/tech-guide.md) |
| Coding conventions, do/don't tables, PR checklist, common pitfalls | [`.ai/tech-best-practices.md`](./.ai/tech-best-practices.md) |
| The 5 binding principles | [`.specify/memory/constitution.md`](./.specify/memory/constitution.md) |
| Game lobby / start logic | `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/ClassicGameLobby.java` |
| Attack mechanics | `…/core/game/ClassicGameAttacker.java` + `ClassicGameAttackResProcessor.java` |
| Player actions API | `…/core/game/ClassicGamePActions.java` |
| Objective evaluation | `…/core/game/utils/EndGameEvaluator.java` + `utils/objective/` |
| Card exchange rules | `…/core/game/utils/ExchangeCardsEvaluator.java` + `CardExchangeState.java` |
| Spring Boot entry point | `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/JWarBackCoreApplication.java` |
| App configuration | `jwar-server/jwarsv-sboot/src/main/resources/application.yml` |

---

**Document version**: 1.0.0 · **Aligned with**: Constitution v1.0.0
