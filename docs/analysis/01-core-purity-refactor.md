# Core Purity Refactor Plan — `jwarsv-core`

**Document**: `docs/analysis/01-core-purity-refactor.md`
**Constitution**: `.specify/memory/constitution.md` v1.0.0 — Principle I "Game Logic Isolation"
**Tech audit reference**: `.ai/tech-best-practices.md:319-324`
**Status**: Proposal — no code changes performed.
**Author**: Analysis only; implementation deferred to a follow-up branch.

---

## 1. Goal & non-goals

### Goal
Bring `jwar-server/jwarsv-core` into compliance with Constitution Principle I
(`.specify/memory/constitution.md:30-38`), which requires the core module to
remain independent of web frameworks, HTTP, and infrastructure concerns. The
target end-state is:

- `jwarsv-core` depends on **nothing** except Java 17 standard library, Lombok
  (compile-only), and SLF4J API (logging facade only — no binding).
- `jwarsv-sboot` owns every Spring Boot starter, Firebase Admin, Jackson,
  Hibernate, MapStruct, OpenCSV, and persistence adapter currently leaking
  into core via either direct or root-level injection.

### Non-goals
- No game-rule semantics change. Behavior under `core/game/` is preserved
  bit-for-bit.
- No introduction of new domain features. This is mechanical scope reduction.
- No move to a third module. `jwar-commons` stays a placeholder per
  `CLAUDE.md` ("currently a placeholder").
- No DTO redesign. Wire-format DTOs already live in `jwarsv-sboot` (none in
  core today) and stay there.

---

## 2. Current-state inventory

### 2.1 Imports inside `jwarsv-core/src/main/java/` that are NOT pure Java + Lombok + SLF4J

A grep across every `.java` under
`jwar-server/jwarsv-core/src/main/java/` for non-`java.*`, non-`lombok.*`,
non-`br.com.bnuuy.jwar.*`, non-`org.slf4j.*` imports returns exactly **one**
hit:

| File:line | Import | Classification |
|---|---|---|
| `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:6` | `import static org.apache.commons.collections4.CollectionUtils.isEmpty;` | **Replace with pure-Java alternative** (`list == null \|\| list.isEmpty()`). The dependency is *not declared* by `jwarsv-core/build.gradle`; it leaks transitively from a Spring starter, so removing it costs nothing and breaks no contract. |

There are **zero** occurrences in `jwarsv-core/src/main/java/` of:
- `org.springframework.*`
- `jakarta.persistence.*` / `javax.persistence.*`
- `com.google.firebase.*`
- `jakarta.validation.*`
- `jakarta.servlet.*`
- `org.hibernate.*`
- `com.fasterxml.jackson.*`
- `io.sentry.*` / `io.micrometer.*`
- `org.mapstruct.*` / `com.opencsv.*`

Verification commands used (preserved here so a reviewer can re-run):
```bash
grep -rn -E "^import " jwar-server/jwarsv-core/src/main/java/ \
  | grep -E "(org\\.springframework|jakarta\\.persistence|com\\.google\\.firebase|jakarta\\.validation|jakarta\\.servlet|org\\.hibernate|com\\.fasterxml|io\\.sentry|io\\.micrometer|com\\.stripe|com\\.starkbank|org\\.mapstruct|com\\.opencsv)"

grep -rn -E "@(Service|Component|Repository|Configuration|Bean|Autowired|RestController|Entity|Table|Column|Id|GeneratedValue|MappedSuperclass)" \
  jwar-server/jwarsv-core/src/main/java/
```
Both commands return empty.

### 2.2 Empty / placeholder files that *suggest* infra coupling

| File:line | Content | Classification |
|---|---|---|
| `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java:1-4` | `public class User {}` — empty body, no annotations. | **Move to sboot** (or delete). The class is a stub. `.ai/tech-best-practices.md:319-324` references it as the canonical "should be a JPA entity in sboot" example. Since it currently holds zero state, it should either be deleted or relocated to `jwarsv-sboot` as a real `@Entity` when persistence is added. Keeping it in core wastes a package and signals the wrong layering intent. |

### 2.3 Build-file violations (`jwar-server/jwarsv-core/build.gradle`)

These are dependencies declared *on core itself* that core's source does not use:

| File:line | Dependency | Classification | Action |
|---|---|---|---|
| `jwar-server/jwarsv-core/build.gradle:3` | `id "io.spring.dependency-management"` plugin | Should move to sboot | Remove from core; sboot already declares it (`jwarsv-sboot/build.gradle:5`). |
| `jwar-server/jwarsv-core/build.gradle:4` | `id "org.hibernate.orm"` plugin | Should move to sboot | Remove from core; sboot already declares it (`jwarsv-sboot/build.gradle:6`). |
| `jwar-server/jwarsv-core/build.gradle:10` | `implementation "org.springframework.boot:spring-boot-starter-data-jpa"` | Should move to sboot | Sboot already declares it (`jwarsv-sboot/build.gradle:26`). Drop from core. |
| `jwar-server/jwarsv-core/build.gradle:11` | `implementation "org.springframework.boot:spring-boot-starter-data-rest"` | Should move to sboot | Sboot already declares it (`jwarsv-sboot/build.gradle:27`). Drop from core. |
| `jwar-server/jwarsv-core/build.gradle:12` | `implementation "com.google.firebase:firebase-admin"` | Should move to sboot | Sboot already declares it (`jwarsv-sboot/build.gradle:38`). Drop from core. |
| `jwar-server/jwarsv-core/build.gradle:13` | `implementation "org.mapstruct:mapstruct"` | Should move to sboot | Core has no MapStruct mappers. Move only if/when sboot adds adapter mappers — `jwarsv-sboot/build.gradle` does NOT currently declare MapStruct, so flag it as "add to sboot when first mapper is written". |
| `jwar-server/jwarsv-core/build.gradle:14` | `implementation "com.opencsv:opencsv"` | Should move to sboot | Core has no CSV usage. Sboot does not currently declare it either; move on first use. |
| `jwar-server/jwarsv-core/build.gradle:16` | `annotationProcessor "org.mapstruct:mapstruct-processor"` | Should move to sboot | Co-located with the MapStruct move above. |

### 2.4 Root build leakage into every subproject (`jwar-server/build.gradle`)

The root `subprojects { ... }` block injects Spring, Jackson, and Hibernate
utilities into *every* module, including `jwarsv-core`:

| File:line | Injected dependency | Classification | Action |
|---|---|---|---|
| `jwar-server/build.gradle:40` | `implementation "org.springframework.boot:spring-boot-starter:${springBootVersion}"` | Should move to sboot | Bring Spring out of the `subprojects` block. Add explicitly to `jwarsv-sboot/build.gradle` only. |
| `jwar-server/build.gradle:41` | `implementation "commons-io:commons-io"` | Not used by core source | Same — move to sboot if needed there; otherwise drop. (`jwarsv-sboot/build.gradle:39-41` already pulls and re-excludes it for OpenFeign — verify it is added back explicitly where actually used.) |
| `jwar-server/build.gradle:43` | `implementation "com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations"` | Should move to sboot | Same. |
| `jwar-server/build.gradle:44` | `implementation "io.hypersistence:hypersistence-utils-hibernate-62"` | Should move to sboot | Same. Hibernate-coupled utility. |
| `jwar-server/build.gradle:45` | `implementation 'io.netty:netty-common:4.1.70.Final'` | Should move to sboot | Same. Required by Firebase Admin in sboot, not by game logic. |
| `jwar-server/build.gradle:57-69` | `dependencyManagement { mavenBom ... }` over spring-cloud, spring-boot, spring-modulith, sentry; `dependency com.google.firebase:firebase-admin` | Should move to sboot | The BOM imports are harmless on a module that declares nothing from them, but moving the BOM imports out of `subprojects` and into `jwarsv-sboot/build.gradle` removes the implicit promise that core "could" pull these. |
| `jwar-server/build.gradle:16-20` | `allprojects { configurations.configureEach { exclude ... spring-boot-starter-tomcat } }` | Keep (legitimate constitution constraint) | Constitution §"Technology Constraints" forbids Tomcat project-wide (`.specify/memory/constitution.md:91`). Leave this in `allprojects`. |
| `jwar-server/build.gradle:22-25` | `subprojects { apply plugin: 'java'; apply plugin: "io.spring.dependency-management"; ... -parameters ... sourceCompatibility=17 ... }` | Partial: keep Java plugin + `-parameters` + Java 17. Move `io.spring.dependency-management` apply into sboot only. | The Java compile config is generic; the dependency-management plugin is Spring-specific and should live on sboot. |
| `jwar-server/build.gradle:36-38` | `compileOnly "org.projectlombok:lombok"` + `annotationProcessor "org.projectlombok:lombok"` | Legitimate game-logic dep | Keep in `subprojects`. Lombok is explicitly allowed by Constitution Technology Constraints (`.specify/memory/constitution.md:83-84`). |
| `jwar-server/build.gradle:47-54` | JUnit / Mockito test deps | Legitimate game-logic dep | Keep in `subprojects`. Constitution Principle II requires JUnit 5 + Mockito. |

### 2.5 Classification summary

**Legitimate game-logic deps (KEEP in core):**
- `org.projectlombok:lombok` — compile-only + annotation processor.
- SLF4J API (`org.slf4j.Logger`, surfaced through Lombok's `@Slf4j`). Note:
  core source uses `@Slf4j` but only the API; sboot supplies the binding.
- JUnit 5 + Mockito for tests.
- Java 17 standard library.

**Should move to sboot:**
- Spring Data JPA, Spring Data REST starters.
- Firebase Admin SDK.
- Hibernate ORM plugin + `hypersistence-utils-hibernate-62`.
- Jackson XML-bind module.
- Netty-common (only Firebase needs it).
- `io.spring.dependency-management` plugin (sboot-only).
- BOM imports (spring-cloud, spring-boot, spring-modulith, sentry).
- MapStruct + OpenCSV (declared but unused; move at first use).

**Should be replaced with pure-Java alternative:**
- `org.apache.commons.collections4.CollectionUtils.isEmpty` →
  `players == null || players.isEmpty()` in
  `core/game/utils/ClassicGameValidator.java:6,24`.

---

## 3. Target package layout

After the refactor, the directory shape is unchanged for `core/game/...`. Only
the `core/domain/` placeholder is relocated and the build file is slimmed.

```
jwar-server/
├── jwarsv-core/                       # pure POJOs, enums, validators, evaluators
│   ├── build.gradle                   # ONLY Lombok + JUnit + Mockito + SLF4J API
│   └── src/main/java/br/com/bnuuy/jwar/core/
│       ├── exceptions/
│       │   └── GameRulesException.java
│       └── game/
│           ├── ClassicGame.java
│           ├── ClassicGameAttacker.java
│           ├── ClassicGameAttackResProcessor.java
│           ├── ClassicGameConstants.java
│           ├── ClassicGameLobby.java
│           ├── ClassicGamePActions.java
│           ├── domain/                # mutable game state + immutable VOs
│           ├── map/                   # E* enums for board, cards, colors
│           └── utils/
│               ├── ClassicGameDist.java
│               ├── ClassicGameValidator.java
│               ├── CardExchangeEvaluator.java
│               ├── CardExchangeState.java
│               ├── EndGameEvaluator.java
│               ├── ExchangeCardsEvaluator.java
│               ├── ShufflerUtil.java
│               └── objective/
│                   ├── ContinentObjectiveEvaluator.java
│                   ├── DestroyPlayerObjectiveEvaluator.java
│                   ├── ObjectiveEvaluator.java
│                   └── TerritoryObjectiveEvaluator.java
└── jwarsv-sboot/                      # ALL Spring / persistence / HTTP / Firebase
    ├── build.gradle                   # adds spring-boot-starter, BOMs, hypersistence, jackson-xmlbind, netty
    └── src/main/java/br/com/bnuuy/jwar/server/
        ├── JWarBackCoreApplication.java   # existing
        ├── domain/
        │   └── User.java                  # MOVED FROM core; will become real @Entity when persistence ships
        └── persistence/                   # NEW (placeholder; populate on first repository)
            └── (PlayerJpaEntity.java, PlayerJpaRepository.java, PlayerPersistenceAdapter.java, ...)
```

Key boundary rules:

- **Direction of dependency**: `jwarsv-sboot → jwarsv-core` only. Enforced by
  `jwarsv-sboot/build.gradle:17` (`implementation project(":jwarsv-core")`).
  Per `.ai/tech-best-practices.md:65-66`, the core build file MUST NOT
  reference sboot. Already the case; refactor must not regress this.
- **Adapter pattern for persistence**: if/when `User` (or any game state)
  needs persistence, sboot defines:
  - A JPA entity (`UserJpaEntity` with `@Entity`, `@Table`, etc.).
  - A Spring Data repository (`UserJpaRepository extends JpaRepository`).
  - A `UserPersistenceAdapter` that maps the JPA entity ↔ core POJO.
  Core *never* sees the JPA entity. This mirrors the hexagonal/ports pattern
  Spring Modulith already implies (`.ai/tech-best-practices.md:93-100`).
- **Logging**: core uses Lombok `@Slf4j`, which generates a private static
  `org.slf4j.Logger`. SLF4J API is on the classpath transitively via Lombok
  generation. Confirm via build that the API is present **without** any
  binding (no `logback-classic`, no `slf4j-simple` in core). The binding
  lives in sboot through Spring Boot starter.

---

## 4. Migration steps (ordered)

Each step is the smallest unit that leaves the build green. Run the
verification commands in §6 between every step.

### Step 1 — Replace `org.apache.commons.collections4` usage

- **Files modified**:
  `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java`
- **Change**:
  - Remove `import static org.apache.commons.collections4.CollectionUtils.isEmpty;` (line 6).
  - Replace the call at line 24 (`if (isEmpty(players) || players.size() < 3)`)
    with `if (players == null || players.isEmpty() || players.size() < 3)`.
- **Wrapper/adapter needed**: none.
- **Rationale**: this is the only non-pure-Java import in core source. Once
  removed, core has zero external runtime references (other than Lombok and
  SLF4J).
- **Risk**: trivially low. The `isEmpty` static is a one-liner equivalent.
- **Verify**: `./gradlew :jwarsv-core:test` from `jwar-server/`.

### Step 2 — Relocate `User.java` to `jwarsv-sboot`

- **Files moved**:
  - From `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java`
  - To `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/domain/User.java`
- **Package rename**: `br.com.bnuuy.jwar.core.domain` → `br.com.bnuuy.jwar.server.domain`.
- **Annotations stripped**: none (file is empty today).
- **Wrapper/adapter needed**: none yet — the class is a placeholder body.
  When real persistence work begins, add `@Entity`, `@Table("users")`, `@Id`
  there. Core never re-imports it.
- **Delete the now-empty directory** `core/domain/` (Gradle will not complain
  about an empty package; cleanup is for hygiene).
- **Risk**: zero — no code references `User` today (verified by
  `grep -rn "core.domain.User\|import .*User;" jwar-server/`).
- **Verify**: `./gradlew :jwarsv-core:build :jwarsv-sboot:compileJava`.

### Step 3 — Slim `jwar-server/jwarsv-core/build.gradle`

Replace the file's `dependencies` block. Target shape:

```groovy
plugins {
    id "java"
}

dependencies {
    // Lombok and JUnit/Mockito are injected by the root subprojects { ... } block.
    // Nothing else belongs here.
}

test {
    useJUnitPlatform()
}

jar {
    manifest {
        attributes 'Implementation-Title': 'jwarsv-core',
                'Implementation-Version': version
    }
}
```

- **Annotations stripped**: removed plugins `io.spring.dependency-management`
  (`jwar-server/jwarsv-core/build.gradle:3`) and `org.hibernate.orm`
  (`jwar-server/jwarsv-core/build.gradle:4`).
- **Dependencies removed**: lines 10–16 in
  `jwar-server/jwarsv-core/build.gradle` (spring-boot-starter-data-jpa,
  spring-boot-starter-data-rest, firebase-admin, mapstruct,
  opencsv, mapstruct-processor).
- **Wrapper/adapter needed**: none in this step. Subsequent steps move the
  removed deps into sboot only if/when sboot actually consumes them.
- **Risk**: medium — if the root `subprojects` block still injects Spring,
  Jackson, Hibernate utilities (Step 4), the core JAR will still carry that
  baggage at runtime. Step 4 closes that gap. Until Step 4 lands the core
  module *compiles* identically; only the explicit declarations differ.
- **Verify**: `./gradlew :jwarsv-core:build` from `jwar-server/`.

### Step 4 — Pull Spring/Jackson/Hibernate baggage out of the root `subprojects` block

- **File modified**: `jwar-server/build.gradle`.
- **Inside the `subprojects { ... }` block (lines 22-71), remove**:
  - `apply plugin: "io.spring.dependency-management"` at line 24.
  - `implementation "org.springframework.boot:spring-boot-starter:${springBootVersion}"` at line 40.
  - `implementation "commons-io:commons-io:${commonsIOVersion}"` at line 41 (verify no core usage — already verified empty).
  - `implementation "com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations:${jakartaXmlbindAnnotations}"` at line 43.
  - `implementation "io.hypersistence:hypersistence-utils-hibernate-62:${hypersistenceUtilsHibernateVersion}"` at line 44.
  - `implementation 'io.netty:netty-common:4.1.70.Final'` at line 45.
  - The entire `dependencyManagement { imports { mavenBom ... } dependencies { dependency firebase-admin } }` block at lines 57-69.
- **Inside `jwarsv-sboot/build.gradle`, add explicitly**:
  - The `io.spring.dependency-management` plugin apply (already present at
    line 5 of sboot — verify, no action).
  - The Spring Boot starter (`spring-boot-starter`) — already pulled
    transitively by `spring-boot-starter-web` etc. at lines 25-33; no
    explicit add required.
  - `jackson-module-jakarta-xmlbind-annotations`, `hypersistence-utils-hibernate-62`,
    `netty-common`, and `commons-io` — only if sboot code actually references
    them. Grep sboot first; current sboot has only one file
    (`JWarBackCoreApplication.java`) so most of these can simply be removed
    project-wide until a real consumer surfaces.
  - The Spring/Cloud/Modulith/Sentry BOM imports — move into the sboot
    `dependencyManagement { imports { ... } }` block.
- **Wrapper/adapter needed**: none.
- **Risk**: highest single step in the plan.
  - Tests in core that transitively used SLF4J binding from `spring-boot-starter`
    may fail to log; they should not fail to *run*.
  - Sboot may stop compiling if some BOM-managed version was relied upon
    elsewhere — mitigated because sboot already imports the Spring Boot BOM
    via its plugin.
  - The Tomcat exclusion in `allprojects { ... }` at lines 16-20 stays — it
    is constitutional (`.specify/memory/constitution.md:91`).
- **Verify**:
  1. `./gradlew :jwarsv-core:dependencies --configuration runtimeClasspath`
     — confirm output lists *only* Lombok (compileOnly drops out), SLF4J,
     and Java module deps. Spring should not appear.
  2. `./gradlew clean build` from `jwar-server/`.

### Step 5 — Confirm no Spring annotations re-enter core (guard rail)

Add a CI/local check (no code change required if the developer prefers a
manual gate). Option A: a Gradle `check` task that fails when grep finds
`org.springframework` in `jwarsv-core/src/main/java/`. Option B: rely on the
existing reviewer checklist in `.ai/tech-best-practices.md:291`.

- **Suggested Gradle snippet** (drop into `jwarsv-core/build.gradle`):

  ```groovy
  tasks.register('verifyCorePurity') {
      doLast {
          def offenders = fileTree("src/main/java")
              .matching { include "**/*.java" }
              .filter { f ->
                  def t = f.text
                  return t.contains("org.springframework")
                      || t.contains("jakarta.persistence")
                      || t.contains("com.google.firebase")
                      || t.contains("jakarta.servlet")
                      || t.contains("org.hibernate")
              }
          if (!offenders.isEmpty()) {
              throw new GradleException("jwarsv-core purity violated by: " + offenders.files)
          }
      }
  }
  check.dependsOn verifyCorePurity
  ```

- **Risk**: zero behavioral risk. Adds a hard floor against regression.
- **Verify**: `./gradlew :jwarsv-core:check`.

### Step 6 — Documentation refresh

- Update `.ai/tech-best-practices.md:319-324` ("known gotchas") — strike the
  "jwarsv-core is not a pure domain layer" entry once Step 4 lands.
- Update `.ai/tech-best-practices.md:73-75` ("What may live in jwarsv-core")
  to reflect that Spring Data JPA, Spring Data REST, and Firebase Admin are
  **no longer** acceptable in core.
- Update `CLAUDE.md` "Project Structure" comment about `jwarsv-core` to
  reinforce the pure-Java constraint.

These are doc-only changes; no Gradle re-run needed.

---

## 5. Risk callouts

### 5.1 Tests that may break
- `jwar-server/jwarsv-core/src/test/java/.../game/utils/ExchangeCardsEvaluatorTest.java`
  and the three other core tests do not reference any Spring or Jackson
  classes (verified by grep). They should compile and pass unmodified.
- However, if any test transitively relied on the `spring-boot-starter` SLF4J
  binding for visible log output, log lines will silently disappear from
  console — *not a test failure*, just a behavioral curiosity worth noting.

### 5.2 Modules that may need updating
- `jwar-commons/` is currently empty (`CLAUDE.md` "currently a placeholder"
  and `jwar-server/settings.gradle:1-5` does not include it). No action.
- `jwarsv-sboot/` may need to **add** Jackson/Hibernate utilities to its own
  `build.gradle` if it later relies on them. Today it doesn't (only one
  Java file: `JWarBackCoreApplication.java:1-13`), so nothing changes
  immediately.

### 5.3 Order traps
- Doing **Step 4 before Step 3** would surface compile errors in core (Spring
  jars vanish before the explicit declarations vanish) — invert order and
  the build stays green at each checkpoint.
- Doing **Step 1 after Step 4** would also fail, because `commons-collections4`
  flows in via Spring transitively; removing Spring from core first would
  break the validator's `isEmpty` import. Replace the import first.
- Doing **Step 2 before Step 1** is safe (the empty `User.java` doesn't
  interact with the validator) but doing it after Step 3 is also safe; order
  among Steps 1 and 2 is interchangeable.

### 5.4 What is NOT a risk
- No production code path consumes `User` today. Moving it has zero blast
  radius beyond the file path.
- No MapStruct mappers exist anywhere in the repo (`grep -rn "@Mapper" .`
  returns nothing). MapStruct can be dropped outright.
- No OpenCSV usage exists (`grep -rn "opencsv\|CSVReader\|CSVWriter" .`
  returns nothing). OpenCSV can be dropped outright.

### 5.5 Constitutional alignment
- Step 1 enforces Principle I.
- Step 2 enforces Principle I + IV (`.specify/memory/constitution.md:30-38`,
  `:57-68`).
- Steps 3-4 enforce "Each module MUST declare only the dependencies it
  directly uses" (`.specify/memory/constitution.md:65`).
- Step 5 enforces ongoing compliance per Governance §"Compliance"
  (`.specify/memory/constitution.md:114-116`).
- Step 6 keeps the developer docs in sync with the lived state.

---

## 6. Build verification commands

Run from `jwar-server/`:

| After step | Command(s) | Expected outcome |
|---|---|---|
| Step 1 | `./gradlew :jwarsv-core:test` | All 4 existing test classes pass. |
| Step 2 | `./gradlew :jwarsv-core:build :jwarsv-sboot:compileJava` | Both compile clean. |
| Step 3 | `./gradlew :jwarsv-core:build` | Core still builds with no Spring deps explicitly declared on itself. |
| Step 4 | `./gradlew clean build` and `./gradlew :jwarsv-core:dependencies --configuration runtimeClasspath` | Core runtime classpath shows only Lombok + SLF4J + Java; full build green. |
| Step 5 | `./gradlew :jwarsv-core:check` | The new `verifyCorePurity` task passes. |
| Step 6 | n/a (doc-only) | Manual review. |

Optional belt-and-suspenders check (any step):

```bash
grep -rn "org.springframework\|jakarta.persistence\|com.google.firebase" \
  jwar-server/jwarsv-core/src/main/java/ \
  && echo "PURITY VIOLATION" || echo "OK"
```

---

## 7. References

- Constitution Principle I: `.specify/memory/constitution.md:30-38`
- Constitution Principle IV (modular architecture):
  `.specify/memory/constitution.md:57-68`
- Constitution Tomcat exclusion: `.specify/memory/constitution.md:91`
- Tech audit "not a pure domain layer": `.ai/tech-best-practices.md:319-324`
- What may live in core: `.ai/tech-best-practices.md:68-82`
- Reviewer checklist: `.ai/tech-best-practices.md:291`
- Core build file (current violations):
  `jwar-server/jwarsv-core/build.gradle:3-16`
- Root build leakage:
  `jwar-server/build.gradle:22-71` (specifically lines 24, 40-45, 57-69)
- Sboot build (target home for moved deps):
  `jwar-server/jwarsv-sboot/build.gradle:5-55`
- The one non-pure import in core source:
  `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:6`
- Empty placeholder to relocate:
  `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java:1-4`

---

## 8. Summary

The refactor is far smaller than the audit text suggests. Source code is
already nearly pure: only **one** import (Apache Commons `isEmpty`) and
**one** misplaced empty class (`User.java`) need to move. The bulk of the
work is **build-file hygiene** — removing dependencies that are declared but
not used, and pulling Spring/Jackson/Hibernate plumbing out of the root
`subprojects` block so it lands only on `jwarsv-sboot`. Six ordered steps,
each independently verifiable, deliver full Principle I compliance with no
behavioral change to game logic.
