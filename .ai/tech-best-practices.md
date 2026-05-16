# jWar - Technical Best Practices

## 1. Purpose

This document captures the **project-specific** coding, architecture, and testing
conventions for **jWar** (digital adaptation of the classic Brazilian board game
"War"). It is constitution-aligned and pairs with `tech-guide.md` (technology
overview) and `CLAUDE.md` (agent-facing instructions).

Scope: rules that ARE actually enforced or expected in this codebase. No generic
SOLID / Clean Code filler. Every rule below ties to a verifiable file or to
constitution principle `v1.0.0` (`.specify/memory/constitution.md`).

Audience: contributors, reviewers, and AI agents working on `jwar-server/`.

---

## 2. Constitution at a glance

`.specify/memory/constitution.md` v1.0.0 - five principles, all binding:

| #   | Principle                  | One-liner                                                                                                       |
| --- | -------------------------- | --------------------------------------------------------------------------------------------------------------- |
| I   | **Game Logic Isolation**   | `jwarsv-core` MUST NOT depend on Spring Web / HTTP / infra. Pure Java game engine.                              |
| II  | **Test-Driven Game Rules** | Every game rule MUST have a JUnit 5 + Mockito unit test before merge. No exceptions.                            |
| III | **Domain Fidelity**        | Implement the original Brazilian "War" faithfully. **Domain text in PT-BR** (territory names, errors, descs).   |
| IV  | **Modular Architecture**   | Dependencies flow inward: `sboot -> core`, never the reverse. Each module declares only what it directly uses.  |
| V   | **Simplicity & YAGNI**     | No speculative abstractions. Three similar lines beat a premature framework. No error handling for impossible. |

Full text: `/var/opt/workspaces/Spring_jWar_game/.specify/memory/constitution.md`.

---

## 3. Coding conventions

| Topic                  | Rule                                                                          | Why                                            | Example (file:line)                                                                       |
| ---------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Enum naming            | `E*` prefix, always                                                           | 100% consistent across `core/game/map`         | `EClassicCountries`, `EObjectiveType`, `EGameColors`                                      |
| Value Object naming    | `*VO` suffix, **immutable**, `@Builder` + `@AllArgsConstructor`               | Distinguishes immutable VOs from game entities | `core/game/domain/AttackResultVO.java`                                                    |
| Game entity naming     | No `VO` suffix, mutable, plain class                                          | These are state-carrying game pieces           | `ClassicGamePlayer`, `ClassicGameCountry`, `ClassicGameContinent` in `core/game/domain/`  |
| Utility naming         | `*Validator`, `*Evaluator`, `*Util`, `*Dist`                                  | Discoverability                                | `ClassicGameValidator`, `EndGameEvaluator`, `ShufflerUtil`, `ClassicGameDist`             |
| Package root           | `br.com.bnuuy.jwar`                                                           | Constitution-defined                           | every `.java` in `jwarsv-core/`, `jwarsv-sboot/`                                          |
| Identifier language    | **English**                                                                   | Standard Java practice                         | `hasPlayerWon`, `currentPrize`                                                            |
| Domain text language   | **Brazilian Portuguese**                                                      | Principle III - Domain Fidelity                | `ClassicGameValidator.java:25` - `"Não é possível iniciar um jogo clássico..."`           |
| Comments               | Prefer none; if needed, English. Javadoc OK for public APIs                   | Code should self-document; avoid AI clutter    | `EndGameEvaluator.java:16-18`                                                             |
| Lombok `@Slf4j`        | Apply to orchestrators only                                                   | Avoid noise on small classes                   | `ClassicGame`, `ClassicGamePActions`, `ClassicGameAttacker` (NOT validators / evaluators) |
| Lombok `@Data`         | **NEVER** on JPA entities                                                     | Constitution constraint                        | constitution.md:84                                                                        |
| Lombok `@Builder`      | Reserved for VOs                                                              | Signals immutability                           | `AttackResultVO`                                                                          |
| Lombok `@RequiredArgs` | Not used in current codebase; prefer explicit constructors                    | Consistency with existing style                | `EndGameEvaluator.java:30-43`                                                             |
| Constants              | Use class-level `public static final` constants; no enums for phase flags yet | Historical - see Common pitfalls               | `ClassicGameConstants.java:4-6`                                                           |

---

## 4. Architecture rules

### 4.1 Dependency direction

```
jwarsv-sboot  ───▶  jwarsv-core
   (Spring Boot, REST,        (game engine, validators,
    persistence wiring)        evaluators, domain enums)
```

The reverse is **forbidden**. `jwarsv-sboot/build.gradle` declares
`implementation project(':jwarsv-core')`. The core build file
(`jwar-server/jwarsv-core/build.gradle`) MUST NOT reference `jwarsv-sboot`.

### 4.2 What may live in `jwarsv-core`

Allowed (today, by precedent):

- Pure game logic, domain entities, enums, validators, evaluators, distributors.
- `spring-boot-starter` (no web), `spring-boot-starter-data-jpa`,
  `spring-boot-starter-data-rest`, Firebase Admin (see
  `jwar-server/jwarsv-core/build.gradle:10-12`).
- Lombok, MapStruct, OpenCSV, hypersistence utils.

**Forbidden**:

- `spring-boot-starter-web` (would violate Principle I).
- `spring-boot-starter-tomcat` - globally excluded at
  `jwar-server/build.gradle:18` (`exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'`).
  Do not re-add.
- HTTP controllers, `@RestController`, `WebClient`, Spring Security web filters.

### 4.3 What may live in `jwarsv-sboot`

- `JWarBackCoreApplication` (entrypoint), `application.yml`.
- REST endpoints, Spring Web + Undertow, Firebase token verification filters,
  Sentry integration, Stripe / StarkBank wiring.
- Anything that requires the servlet container.

### 4.4 Spring Modulith

The project **declares** `spring-modulith-bom` and starters
(`jwar-server/jwarsv-sboot/build.gradle:42-43`) but currently has **zero
`@ApplicationModule` annotations**. Module boundaries are therefore enforced
only by Gradle multi-module separation, not by Modulith itself.

When new sub-packages stabilize inside `core` or `sboot`, annotate
`package-info.java` with `@ApplicationModule` and add a Modulith verification
test. Until then, do not pretend boundaries are enforced - they aren't.

### 4.5 Decision tree: "Where does this code go?"

```
Is it game rules / mechanics / domain state?
  yes -> jwarsv-core/.../game/...
Is it HTTP / REST / auth wiring / config?
  yes -> jwarsv-sboot/...
Is it a generic util needed by both modules?
  yes -> jwarsv-core/.../utils  (jwar-commons is currently a placeholder)
Is it persistence (entity + repository)?
  yes -> jwarsv-core if used by game engine; jwarsv-sboot if pure infra.
         Default to core (per current precedent).
```

---

## 5. Design pattern conventions

### 5.1 Strategy - Objective evaluation (`EndGameEvaluator`)

`EndGameEvaluator` (`core/game/utils/EndGameEvaluator.java:20-43`) holds a
`Map<EObjectiveType, ObjectiveEvaluator>`. Each objective type maps to its
evaluator. **Do not introduce `if/switch` chains over `EObjectiveType` outside
this map.**

**Three-step checklist to add a new objective**:

1. Add a value to `EObjectiveType` (`core/game/map/EObjectiveType.java`).
2. Implement `ObjectiveEvaluator` (in `core/game/utils/objective/`).
3. Register it in `EndGameEvaluator`'s constructor map at
   `core/game/utils/EndGameEvaluator.java:38-42`.

Plus: add a unit test (Principle II). No fourth step.

### 5.2 Facade - `ClassicGame`

`ClassicGame` orchestrates the lobby, player actions, attack, post-attack, and
end-game logic. It is the **only** game-state facade. Two rules:

- If a new responsibility is **player-facing** (a new action / phase
  transition), extend `ClassicGamePActions` / `ClassicGame`.
- If a new responsibility is **rule evaluation** (does X qualify? did Y win?),
  introduce a `*Validator` or `*Evaluator` collaborator and inject it. Do not
  swell `ClassicGame` itself.

### 5.3 Validators

`ClassicGameValidator` (`core/game/utils/ClassicGameValidator.java`) is a
collection of **static methods** that throw `GameRulesException` with PT-BR
messages.

- Keep validators static. No instance state.
- Do not introduce a `Validator` interface or abstract base class.
- Error messages: **Brazilian Portuguese**, full sentences, present tense.
  Example: `"Não é possível iniciar um jogo clássico com menos de 3 players"`
  (`core/game/utils/ClassicGameValidator.java:25`).
- Throw `GameRulesException` (`core/exceptions/GameRulesException.java`), never
  `IllegalArgumentException` or `IllegalStateException`.

### 5.4 Value Objects

Suffix with `VO`. Immutable. `@Builder` + `@AllArgsConstructor` + `@Getter`.
No setters. No mutation methods. Example: `AttackResultVO`.

If a class needs to mutate, **do not** suffix it `VO`. See `ClassicGamePlayer`
(mutable game entity, no suffix) vs `AttackResultVO` (immutable result, suffix).

### 5.5 Mutable state exception: `CardExchangeState`

`CardExchangeState` (`core/game/utils/CardExchangeState.java:11-49`) intentionally
violates the "VOs are immutable" guideline. It tracks the running exchange count
and prize across a single game and exposes `incrementExchangeCount()` which
mutates two fields and returns the previous prize.

**Do not "fix" this by making it immutable.** The call site in `ClassicGame`
relies on the mutation. Document mutation explicitly in any similar future class.

### 5.6 Enum-driven board

The 42 territories, 6 continents, card shapes, objective types, and player
colors are all **`E*` enums**, not JPA entities. The board is constant by
design (matches the physical board game).

- Do not add JPA `@Entity` mapping to these enums.
- Do not store the board topology in a database table.
- Persisted game state references enums by name/code, not by FK rows.

A change here requires a constitution amendment (Principle III + V).

---

## 6. Testing standards

Per **Constitution Principle II**, this is non-negotiable: every game rule MUST
have a unit test before merge.

### 6.1 Stack and layout

- JUnit 5 (Jupiter) + Mockito 3+. See `jwar-server/build.gradle:47-49`.
- Tests live in `jwar-server/jwarsv-core/src/test/java/br/com/bnuuy/jwar/core/`.
- Mirror production package layout.

### 6.2 Annotations and style

| Annotation                                  | When                                                    |
| ------------------------------------------- | ------------------------------------------------------- |
| `@ExtendWith(MockitoExtension.class)`       | Default for unit tests                                  |
| `@MockitoSettings(strictness = LENIENT)`    | Only when justified (e.g., card exchange test setup)    |
| `@Spy`                                      | Partial mocks - typically real `ClassicGamePlayer`s     |
| `@Mock`                                     | Collaborators / boundary types                          |
| `@BeforeEach`                               | Common setup; keep tiny                                 |
| `@DisplayName("...")`                       | Required on each `@Test`; readable EN or PT (pick one per class) |
| `assertThrows(GameRulesException.class, ...)` | All negative paths                                      |

Reference: `core/test/.../ClassicGameTest.java:16-49` shows the canonical
pattern (`@ExtendWith` + multiple `@Spy` players + `@BeforeEach` + `@DisplayName`).

### 6.3 What MUST be tested

- Every public method on `ClassicGame` and `ClassicGamePActions`.
- Every branch in validators (positive + each `GameRulesException` path).
- Every `ObjectiveEvaluator` (one happy path + one near-miss).
- Distributors (`ClassicGameDist`) for territory, color, and objective dealing.
- Dice / attack outcome (`ClassicGameAttacker`) - inject randomness explicitly,
  do not assert on `Math.random()`.

### 6.4 Test template

```java
@ExtendWith(MockitoExtension.class)
class MyRuleTest {

    @Spy
    private ClassicGamePlayer p1 = new ClassicGamePlayer(1, EGameColors.RED, ...);

    @Mock
    private SomeCollaborator collaborator;

    private SubjectUnderTest sut;

    @BeforeEach
    void setUp() {
        sut = new SubjectUnderTest(collaborator);
    }

    @Test
    @DisplayName("Should reject action when player is not on turn")
    void rejectsOffTurnAction() {
        GameRulesException ex = assertThrows(
            GameRulesException.class,
            () -> sut.doAction(p1)
        );
        assertEquals("Não é possível executar esta ação fora do seu turno",
                     ex.getMessage());
    }
}
```

---

## 7. Do / Don't quick reference

| Do                                                                              | Don't                                                                                       |
| ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Prefix enums with `E`                                                           | Use ad-hoc names like `CountryEnum`, `Country`                                              |
| Suffix immutable VOs with `VO`                                                  | Suffix mutable game entities with `VO`                                                      |
| Throw `GameRulesException` with **PT-BR** messages                              | Throw `IllegalArgumentException` or English messages                                        |
| Put new game rules in `jwarsv-core`                                             | Drag Spring Web / REST into `jwarsv-core`                                                   |
| Register new objectives in the `EndGameEvaluator` map                           | Add `if`/`switch` on `EObjectiveType` outside `EndGameEvaluator`                            |
| Keep `ClassicGameValidator` methods static                                      | Convert validators to a class hierarchy or DI bean                                          |
| Use `@Spy` for real player instances, `@Mock` for collaborators                 | Use `@MockBean` (it pulls Spring context unnecessarily)                                     |
| Write `@DisplayName` on every test                                              | Leave default JUnit method-name display                                                     |
| Use Undertow                                                                    | Re-introduce Tomcat (globally excluded `jwar-server/build.gradle:18`)                       |
| Add tests **with** the rule, same PR                                            | Defer tests "for later" - Principle II blocks merge                                         |
| Keep `CardExchangeState` mutable                                                | "Refactor" it to immutable without reading `ClassicGame` call site                          |
| Inject `Random` (or a seeded source) for dice                                   | Call `Math.random()` directly in production code                                            |
| Use `@Getter`, `@Setter`, `@Slf4j` liberally on orchestrators                   | Spread `@Slf4j` onto every tiny utility                                                     |
| Treat board (countries / continents) as enum-only                               | Add JPA mapping to `EClassicCountries` / `EClassicContinents`                               |

---

## 8. Pull Request checklist

Run before requesting review:

- [ ] `./gradlew build` (from `jwar-server/`) passes cleanly.
- [ ] All new game rules have JUnit 5 + Mockito unit tests (Principle II).
- [ ] No new code in `jwarsv-core` imports `org.springframework.web.*`, servlet
      API, or anything REST-related (Principle I).
- [ ] No `if`/`switch` on `EObjectiveType` outside `EndGameEvaluator`.
- [ ] New domain text (error messages, country/continent labels, objective
      descriptions) is in **Brazilian Portuguese** (Principle III).
- [ ] Identifiers, method names, and code comments are in English.
- [ ] No `@Data` on JPA entities.
- [ ] New value-object-style classes use `*VO` suffix + `@Builder`; new game
      entities do **not** use `*VO`.
- [ ] No `IllegalArgumentException` / `IllegalStateException` for rule
      violations - use `GameRulesException`.
- [ ] No `Math.random()` calls in production code paths; randomness is injected.
- [ ] Did not re-add `spring-boot-starter-tomcat` (it's globally excluded).
- [ ] Constitution version not changed by this PR (unless this PR is an
      explicit amendment with sync-impact report).
- [ ] No commit / push / PR was performed by an automated agent - developer
      runs git operations manually.

---

## 9. Common pitfalls (landmines already in the code)

1. **Spring Modulith declared but unenforced.** `spring-modulith-starter-core`
   is on the classpath (`jwar-server/jwarsv-sboot/build.gradle:42`), yet no
   `package-info.java` carries `@ApplicationModule`. Module boundaries are
   currently enforced **only** by Gradle's multi-project setup. Adding Modulith
   annotations is a future opportunity, not a current guarantee.

2. **`jwarsv-core` is not a "pure" domain layer despite the name.** It pulls
   in Spring Data JPA, Spring Data REST, Firebase Admin, and OpenCSV
   (`jwar-server/jwarsv-core/build.gradle:10-14`). The constitution's "no web
   framework" rule has been interpreted as "no `spring-boot-starter-web`", not
   "no Spring at all". Honor that interpretation; do not push further infra
   into core.

3. **Phase constants are ints, not an enum.**
   `ClassicGameConstants.java:4-6`:
   ```java
   public static final int TURN_PHASE_ADD = 1;
   public static final int TURN_PHASE_ATTACK = 2;
   public static final int TURN_PHASE_MOVE = 3;
   ```
   This is historical. Don't refactor casually - it would ripple through
   every comparison site. If you do refactor, do it in a dedicated PR with
   full test coverage.

4. **`CardExchangeState` is intentionally mutable.** See section 5.5. Do not
   "fix" it.

5. **Two country display names slipped to English.** In
   `EClassicCountries.java`:
   - Line 29: `SWD(17, "Sweden", EClassicContinents.EUR)` - should be
     `"Suécia"`.
   - Line 31: `MOS(19, "Moscow", EClassicContinents.EUR)` - should be
     `"Moscou"`.

   This is a Domain Fidelity (Principle III) regression worth fixing in a
   small dedicated PR with a unit test asserting all 42 country names are
   Portuguese.

6. **`@MockitoSettings(strictness = LENIENT)`** sneaks past test rigor when
   over-used. Only apply it when the strict mode would force unrelated
   stubbing - never to silence warnings broadly.

7. **`jwar-commons` is a placeholder.** Don't dump shared utils there without
   a clear consumer in both `jwarsv-core` and `jwarsv-sboot`. Otherwise place
   the util in `core/game/utils/`.

---

## 10. Spec-Kit workflow

This project uses GitHub Spec-Kit for spec-driven development.

```
constitution  ->  /speckit-specify   spec.md
                  /speckit-clarify   (questions resolved into spec)
                  /speckit-plan      plan.md + design artifacts
                  /speckit-tasks     tasks.md (dependency-ordered)
                  /speckit-implement (execute tasks)
                  /speckit-analyze   (cross-artifact consistency)
```

Skill commands live under `.claude/skills/speckit-*/`. Constitution at
`.specify/memory/constitution.md`. Templates at `.specify/templates/`.

For non-trivial features, follow the full flow. Tiny fixes (typo, single-method
bug) may bypass it.

---

## 11. Performance and ops notes

Keep this section brief - operational concerns are mostly inherited:

- **Web server**: Undertow, not Tomcat (`jwar-server/build.gradle:18` excludes
  Tomcat globally). Don't add Tomcat back.
- **Metrics**: Micrometer + Prometheus. Expose new game counters via
  `MeterRegistry` injection from `jwarsv-sboot` only.
- **Error tracking**: Sentry. Game-rule violations (`GameRulesException`) are
  user-facing and **should not** trigger Sentry events; only unexpected
  exceptions should.
- **Database**: PostgreSQL in prod, H2 in dev/test. Schema is Flyway-managed -
  add new migrations under `jwarsv-sboot/src/main/resources/db/migration/` (or
  the project's chosen location) and never edit applied migrations.
- **GraalVM Native Image**: supported. Avoid reflection-heavy patterns in new
  code; if unavoidable, register hints.

---

## 12. Git rules

**AI and automated agents MUST NOT**:

- Run `git commit`.
- Run `git push`.
- Open pull requests (`gh pr create`).
- Modify `.git/` config.

The developer (Fernando Gomes) handles every git operation manually. This is a
hard rule from `CLAUDE.md` and the constitution's Development Workflow section.

When work is "done" from an agent's perspective, leave the working tree dirty
and stop. Do not stage. Do not commit.

---

## 13. Pointers

- Constitution: `/var/opt/workspaces/Spring_jWar_game/.specify/memory/constitution.md`
- Agent instructions: `/var/opt/workspaces/Spring_jWar_game/CLAUDE.md`
- Tech overview: `/var/opt/workspaces/Spring_jWar_game/.ai/tech-guide.md` (companion document)
- Game engine: `/var/opt/workspaces/Spring_jWar_game/jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/`
- Spring Boot entry: `/var/opt/workspaces/Spring_jWar_game/jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/JWarBackCoreApplication.java`
- Tests: `/var/opt/workspaces/Spring_jWar_game/jwar-server/jwarsv-core/src/test/java/br/com/bnuuy/jwar/core/`
- Build config: `/var/opt/workspaces/Spring_jWar_game/jwar-server/build.gradle`,
  `gradle.properties`, `settings.gradle`
- Spec-Kit memory & templates: `/var/opt/workspaces/Spring_jWar_game/.specify/`
