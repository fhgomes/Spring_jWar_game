# Feature Specification: Core Purity Refactor — `jwarsv-core` Constitution Compliance

**Feature Branch**: `001-core-purity-refactor`

**Created**: 2026-05-15

**Status**: Draft

**Input**: User description: "Bring `jwar-server/jwarsv-core` into full compliance with Constitution Principle I (Game Logic Isolation). The module must depend only on Java 17, Lombok (compile-only), SLF4J API, JUnit, and Mockito. All Spring Boot, JPA, Firebase, Hibernate, MapStruct, OpenCSV, and Apache Commons baggage currently leaking into core via direct declarations or the root `subprojects {}` block must move to `jwarsv-sboot` or be removed outright. A Gradle verification task must guard against regression."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Strip Forbidden Build Dependencies from `jwarsv-core` (Priority: P1)

As a maintainer of the jWar codebase, I need `jwarsv-core/build.gradle` and the root `jwar-server/build.gradle` `subprojects {}` block to stop declaring Spring Boot Data JPA, Spring Boot Data REST, Firebase Admin, MapStruct, OpenCSV, Hibernate ORM plugin, Hibernate utilities, Jackson XML-bind, and Netty on the core module, so that the core engine compiles and runs without any framework or persistence baggage on its runtime classpath.

**Why this priority**: This is the foundational step of Constitution Principle I compliance (`.specify/memory/constitution.md:30-38`). Until the build files are clean, every other purity guarantee is fictional — core can transitively depend on anything it wants. Per `docs/analysis/01-core-purity-refactor.md:76-107`, the build files are the bulk of the violation. Without this fix, `verifyCorePurity` (US2) cannot pass and the documentation update (US3) is premature.

**Independent Test**: Run `./gradlew :jwarsv-core:dependencies --configuration runtimeClasspath` from `jwar-server/` and confirm the dependency tree lists only Lombok (compileOnly drops out at runtime), SLF4J API, and JDK modules. No `org.springframework`, no `jakarta.persistence`, no `com.google.firebase`, no `org.hibernate`, no `com.fasterxml.jackson`, no `org.mapstruct`, no `com.opencsv`, no `commons-collections4`, no `io.netty`, no `io.hypersistence`.

**Acceptance Scenarios**:

1. **Given** the current `jwar-server/jwarsv-core/build.gradle` declaring `id "io.spring.dependency-management"`, `id "org.hibernate.orm"`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-rest`, `firebase-admin`, `mapstruct`, `opencsv`, and `mapstruct-processor` (lines 3-16 per `docs/analysis/01-core-purity-refactor.md:82-89`), **When** the refactor is applied, **Then** all eight declarations are removed from `jwarsv-core/build.gradle` and the file contains only the `java` plugin and the `test`/`jar` configuration blocks.
2. **Given** the current `jwar-server/build.gradle` root `subprojects {}` block injecting `io.spring.dependency-management` plugin, `spring-boot-starter`, `commons-io`, `jackson-module-jakarta-xmlbind-annotations`, `hypersistence-utils-hibernate-62`, `netty-common`, and the BOM imports for spring-cloud / spring-boot / spring-modulith / sentry plus `firebase-admin` (`jwar-server/build.gradle:22-69`), **When** the refactor is applied, **Then** none of those declarations remain inside `subprojects {}`; only the `java` plugin apply, Java 17 source/target compatibility, `-parameters` compile flag, Lombok compileOnly + annotationProcessor, and the JUnit/Mockito test dependencies stay (per `docs/analysis/01-core-purity-refactor.md:105-107`).
3. **Given** the `allprojects { configurations.configureEach { exclude module: "spring-boot-starter-tomcat" } }` block at `jwar-server/build.gradle:16-20`, **When** the refactor is applied, **Then** the Tomcat exclusion remains untouched — it is constitutional per `.specify/memory/constitution.md:91`.
4. **Given** `jwarsv-sboot/build.gradle` already declares the Spring Boot starters it needs (lines 25-33), **When** the BOM imports and `io.spring.dependency-management` plugin are relocated from the root `subprojects {}` block, **Then** `./gradlew :jwarsv-sboot:build` continues to compile and run with no version-resolution errors.
5. **Given** the existing core test classes (`ExchangeCardsEvaluatorTest` and the three other tests under `jwarsv-core/src/test/java/`), **When** the build files are slimmed, **Then** `./gradlew :jwarsv-core:test` continues to pass all tests with no recompilation errors.

---

### User Story 2 - Remove the One Non-Pure Import from Core Source (Priority: P1)

As a contributor reading core source code, I need `ClassicGameValidator.java` to use only pure Java collection idioms so that the file is self-contained and the line `import static org.apache.commons.collections4.CollectionUtils.isEmpty;` no longer leaks an Apache Commons dependency through Spring's transitive closure.

**Why this priority**: P1 because (a) it is the **only** non-pure-Java import anywhere in `jwarsv-core/src/main/java/` (verified by the grep in `docs/analysis/01-core-purity-refactor.md:60-68`), and (b) it must land **before** US1 step 4 — removing Spring from `subprojects {}` removes the transitive Apache Commons jar, which would break the import. Order matters: replace the import first, then strip the deps.

**Independent Test**: Run `grep -rn -E "^import " jwar-server/jwarsv-core/src/main/java/ | grep -v -E "^.*: ?import (java\.|lombok\.|org\.slf4j\.|br\.com\.bnuuy\.jwar\.)"` and confirm the output is empty. Run `./gradlew :jwarsv-core:test` and confirm `ClassicGameValidatorTest` still passes.

**Acceptance Scenarios**:

1. **Given** `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:6` containing `import static org.apache.commons.collections4.CollectionUtils.isEmpty;`, **When** the refactor is applied, **Then** the import is removed and the file has zero non-java/non-lombok/non-slf4j/non-project imports.
2. **Given** the call at `ClassicGameValidator.java:24` reading `if (isEmpty(players) || players.size() < 3) throw ...`, **When** the refactor is applied, **Then** the condition reads `if (players == null || players.isEmpty() || players.size() < 3) throw ...` and produces identical behavior.
3. **Given** the existing tests covering `ClassicGameValidator.validatePlayers(...)`, **When** the change lands, **Then** all tests pass without modification.

---

### User Story 3 - Relocate the Empty `User.java` Placeholder out of Core (Priority: P1)

As a contributor, I need the empty `User` class placeholder removed from `jwarsv-core/.../core/domain/` so that the core module signals the right layering intent: persistence-bound types live in `jwarsv-sboot`, not core.

**Why this priority**: P1 because `.ai/tech-best-practices.md:319-324` calls out this exact file as the canonical "wrong-layer placeholder" example, and Constitution Principle IV (`.specify/memory/constitution.md:57-68`) requires "Cross-module dependencies MUST flow inward (sboot depends on core, never the reverse)". A `User` entity belongs in sboot, where JPA annotations can be added when persistence ships.

**Independent Test**: Run `find jwar-server/jwarsv-core/src/main/java -name 'User.java'` and confirm the file is gone (or relocated). Run `find jwar-server/jwarsv-sboot/src/main/java -name 'User.java'` to confirm the new home if relocated. Run `./gradlew :jwarsv-core:build :jwarsv-sboot:compileJava` and confirm both compile.

**Acceptance Scenarios**:

1. **Given** `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java` containing `public class User {}` (empty body, no annotations) per `docs/analysis/01-core-purity-refactor.md:71-74`, **When** the refactor is applied, **Then** the file is either deleted outright or relocated to `jwar-server/jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/domain/User.java` with its package renamed to `br.com.bnuuy.jwar.server.domain`.
2. **Given** that `grep -rn "core.domain.User\|import .*\.User;" jwar-server/` returns zero hits today (confirmed in `docs/analysis/01-core-purity-refactor.md:233`), **When** the file moves or is deleted, **Then** no compilation error appears anywhere in the project.
3. **Given** the now-empty `br/com/bnuuy/jwar/core/domain/` directory after the move, **When** the refactor is applied, **Then** the empty directory is also removed for hygiene.

---

### User Story 4 - Add a `verifyCorePurity` Gradle Guard Task (Priority: P2)

As a maintainer, I need a Gradle `check`-bound task that scans `jwarsv-core/src/main/java/` for forbidden package references and fails the build when any reappear, so that the purity guarantee survives future code changes without relying solely on manual code review.

**Why this priority**: P2 because the guarantee is meaningful only after US1, US2, and US3 are complete. It is a regression gate, not a fix. Constitution Governance §"Compliance" (`.specify/memory/constitution.md:114-116`) says violations MUST be resolved before code is accepted — an automated gate operationalizes that. Per `docs/analysis/01-core-purity-refactor.md:316-347`, this gate is a one-paragraph Gradle snippet.

**Independent Test**: Drop the `verifyCorePurity` task into `jwarsv-core/build.gradle`, wire `check.dependsOn verifyCorePurity`, then artificially introduce `import org.springframework.stereotype.Component;` into any file under `jwarsv-core/src/main/java/`. Run `./gradlew :jwarsv-core:check` and confirm the build fails with a clear "purity violated" message naming the offending file. Remove the artificial import; the build passes.

**Acceptance Scenarios**:

1. **Given** the slimmed `jwarsv-core` from US1/US2/US3, **When** `./gradlew :jwarsv-core:check` runs, **Then** the `verifyCorePurity` task executes and reports success (no offenders).
2. **Given** the same project, **When** a contributor adds `import org.springframework.beans.factory.annotation.Autowired;` to any `.java` file under `jwarsv-core/src/main/java/`, **When** `./gradlew :jwarsv-core:check` runs, **Then** the build fails with `GradleException` naming the offending file path.
3. **Given** the forbidden-packages list per `docs/analysis/01-core-purity-refactor.md:325-345`, **When** the task is invoked, **Then** it flags any occurrence of `org.springframework`, `jakarta.persistence`, `com.google.firebase`, `jakarta.servlet`, or `org.hibernate` in core source.
4. **Given** that Lombok and SLF4J are explicitly allowed by Constitution Technology Constraints (`.specify/memory/constitution.md:83-84`), **When** the task scans source, **Then** Lombok annotations and SLF4J calls do NOT trigger violations.

---

### User Story 4b - Relocate Spring/Cloud/Modulith/Sentry BOM Imports to `jwarsv-sboot` (Priority: P2)

As a maintainer, I need the `dependencyManagement { imports { mavenBom ... } }` block currently at `jwar-server/build.gradle:57-69` to move from the root `subprojects {}` context into `jwarsv-sboot/build.gradle` so that BOM-managed versions are declared at the module that actually consumes them, not at the project root where they implicitly apply to `jwarsv-core` as well.

**Why this priority**: P2 because the BOM imports do not pull any actual jars onto core's classpath today — they only declare managed versions. But the *presence* of `spring-boot-dependencies`, `spring-cloud-dependencies`, `spring-modulith-bom`, and `sentry-bom` in core's effective build script implies that core "could" pull anything those BOMs manage. That implication contradicts Principle I and confuses contributors. Per `docs/analysis/01-core-purity-refactor.md:103`, the BOM imports plus the managed `firebase-admin` dependency in `dependencies {}` are part of the same hygiene cleanup as US1 step 4, but they merit their own scenarios because the failure mode (silent version drift, not compilation error) is qualitatively different.

**Independent Test**: After the relocation, `./gradlew :jwarsv-core:dependencyManagement --configuration runtimeClasspath` shows no managed Spring/Cloud/Modulith/Sentry versions; the same command on `:jwarsv-sboot` shows them. `./gradlew :jwarsv-sboot:bootRun` continues to work with no manual version specification.

**Acceptance Scenarios**:

1. **Given** `jwar-server/build.gradle:57-69` contains `dependencyManagement { imports { mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"; mavenBom "org.springframework.boot:spring-boot-dependencies:${springBootVersion}"; mavenBom "org.springframework.modulith:spring-modulith-bom:${springModulithVersion}"; mavenBom "io.sentry:sentry-bom:${sentryVersion}" } dependencies { dependency "com.google.firebase:firebase-admin:${firebaseAdminVersion}" } }` inside the root `subprojects {}` block, **When** the refactor is applied, **Then** the entire block is removed from the root file.
2. **Given** `jwarsv-sboot/build.gradle` already applies the `io.spring.dependency-management` plugin at line 5, **When** the four `mavenBom` imports relocate into sboot's own `dependencyManagement {}` block, **Then** `./gradlew :jwarsv-sboot:dependencies` shows the same set of managed versions for sboot only.
3. **Given** the managed `firebase-admin` dependency, **When** the refactor is applied, **Then** it relocates from the root-level `dependencyManagement { dependencies { ... } }` block into `jwarsv-sboot/build.gradle` (because sboot is the only module that declares `firebase-admin` as an implementation dependency per `docs/analysis/01-core-purity-refactor.md:86`).
4. **Given** the relocated BOMs, **When** `./gradlew :jwarsv-sboot:build` is run, **Then** all Spring Boot, Spring Cloud, Spring Modulith, and Sentry artifacts resolve to the same versions as before the refactor — confirmed by comparing `./gradlew :jwarsv-sboot:dependencies` output diff before and after.

---

### User Story 5 - Update Developer Documentation to Reflect the Cleaned State (Priority: P3)

As a contributor reading project documentation, I need `.ai/tech-best-practices.md` and `CLAUDE.md` to reflect the post-refactor reality so that the documented "gotchas" no longer mention violations that have been fixed and the "what may live in core" list is accurate.

**Why this priority**: P3 because docs lag code, not the other way around. The refactor is correct without doc updates; doc drift is annoying but non-blocking. Per `docs/analysis/01-core-purity-refactor.md:349-359`, the doc changes are: strike the "jwarsv-core is not a pure domain layer" entry from `.ai/tech-best-practices.md:319-324`; update `.ai/tech-best-practices.md:73-75` ("What may live in jwarsv-core") to remove Spring Data JPA, Spring Data REST, and Firebase Admin from the acceptable list; refresh the `CLAUDE.md` "Project Structure" comment to reinforce the pure-Java constraint.

**Independent Test**: Read `.ai/tech-best-practices.md` and confirm no surviving reference to "jwarsv-core is not a pure domain layer". Read `CLAUDE.md` "Project Structure" section and confirm the `jwarsv-core` comment says "Core game engine (pure game logic, no Spring web, no persistence, no Firebase)".

**Acceptance Scenarios**:

1. **Given** the entry at `.ai/tech-best-practices.md:319-324` describing the pre-refactor pitfall, **When** the doc update lands, **Then** the entry is either deleted or rewritten in the past tense ("historically, ..."), making it unambiguous that the situation is fixed.
2. **Given** the list at `.ai/tech-best-practices.md:73-75` describing acceptable core dependencies, **When** the doc update lands, **Then** the list explicitly excludes Spring Data JPA, Spring Data REST, Firebase Admin, MapStruct, OpenCSV, and Hibernate.
3. **Given** the `CLAUDE.md` "Project Structure" comment about `jwarsv-core`, **When** the doc update lands, **Then** the comment reads "Core game engine (pure game logic, no Spring web)" or a stricter equivalent.
4. **Given** the reviewer checklist in `.ai/tech-best-practices.md:291`, **When** the doc update lands, **Then** the checklist item about "verify no Spring imports in core" can be replaced or supplemented with "verify `./gradlew :jwarsv-core:check` passes" since the `verifyCorePurity` task now mechanizes the check.

---

### Edge Cases

- **What happens when `jwarsv-sboot` actually consumes a dependency that used to live in `subprojects {}`?** Per `docs/analysis/01-core-purity-refactor.md:288-298`, the dependency is added explicitly to `jwarsv-sboot/build.gradle` only if and when sboot code references it. Today sboot has only one file (`JWarBackCoreApplication.java`) so most relocated deps are simply dropped project-wide.
- **What happens when a test transitively relied on Spring Boot's SLF4J binding for log output?** Per `docs/analysis/01-core-purity-refactor.md:367-371`, log lines may silently disappear from test console output. This is not a test failure — tests still pass. Sboot's Spring Boot starter still supplies the binding at integration time.
- **What if `verifyCorePurity` flags a false positive (e.g., the string `"org.springframework"` inside a Javadoc comment)?** Per the implementation pattern in `docs/analysis/01-core-purity-refactor.md:325-345`, the task scans raw file text. A javadoc-only mention would be a false positive. Acceptable mitigation: scope the substring match to `import ` lines (`f.text.readLines().any { it.startsWith("import ") && it.contains("org.springframework") }`).
- **What if a developer reintroduces `commons-collections4` later via a different path?** The `verifyCorePurity` task in US4 covers the five Spring/JPA/Firebase/servlet/Hibernate packages. Add `org.apache.commons.collections4` to the forbidden list to extend the guard.
- **What happens if the developer wants to add a *new* legitimate dependency to core (e.g., Apache Commons Lang for `StringUtils`)?** Per Constitution Principle V (Simplicity & YAGNI), prefer a 3-line pure-Java equivalent. If genuinely required, document the dependency in `.ai/tech-best-practices.md` "What may live in jwarsv-core", justify against Principle I, and update the forbidden-list comment in `verifyCorePurity` to acknowledge the allowed addition.
- **What happens if `jwarsv-sboot` legitimately needs a class currently inside `jwarsv-core`?** The dependency flows inward only (sboot → core per `.specify/memory/constitution.md:66-67`). Sboot already declares `implementation project(":jwarsv-core")` so any public type in core is consumable from sboot without code change. Core types MUST NOT be moved to sboot solely for convenience — they belong wherever the rule isolation principle places them.
- **What happens if a step in the six-step migration is performed out of order?** Per `docs/analysis/01-core-purity-refactor.md:381-390`, two ordering traps exist: (a) removing Spring from `subprojects {}` before replacing the `commons-collections4.isEmpty` import breaks compilation (the import flows in via Spring transitively); (b) removing the explicit core declarations *after* the `subprojects {}` cleanup means Spring jars vanish from core before core's own build file stops referencing them. The agreed step order in `docs/analysis/01-core-purity-refactor.md:200-360` avoids both.
- **What happens to test logging if the SLF4J binding is no longer on the core classpath?** Per `docs/analysis/01-core-purity-refactor.md:367-371`, log lines silently drop from console output during `./gradlew :jwarsv-core:test`. Tests still pass — this is a console-quietness side effect, not a regression. Sboot retains its binding through Spring Boot starter.
- **What happens to GraalVM Native Image support after the refactor?** Native Image is configured for sboot per `CLAUDE.md` "Cloud" tech stack note. Core never participated in native compilation directly. Removing Spring from core does not affect native build feasibility on sboot.
- **What happens when a contributor opens `jwarsv-core` in an IDE that auto-imports?** IntelliJ and VS Code Java extensions may suggest `org.springframework.util.CollectionUtils.isEmpty` as an alternative to `java.util.Collection.isEmpty()`. The `verifyCorePurity` task catches this on build, but reviewers SHOULD watch for it during PR review per `.ai/tech-best-practices.md:291`.
- **What happens when `jwarsv-core/build/libs/jwarsv-core.jar` is consumed downstream by `jwarsv-sboot`?** Per Gradle project-dependency resolution, sboot pulls core's compile-time and runtime classpath into its own. The refactor reduces core's classpath but does not break this resolution because sboot independently declares its own Spring/Hibernate/Firebase deps.
- **What happens if `./gradlew :jwarsv-core:check` is invoked while `verifyCorePurity` is intentionally absent (e.g., during step 5 of the migration before the task lands)?** `check` simply runs the existing test phase. The build remains green. The task is additive; its absence is not a failure.
- **What happens when the BOM relocation lands but `jwarsv-sboot` references a BOM-managed version that is no longer available because the BOM moved?** Pre-refactor verification: run `./gradlew :jwarsv-sboot:dependencies` and snapshot the resolved versions. Post-refactor verification: run the same command and diff. Any version drift identifies a relocation bug.
- **What happens when `gradle.properties` defines a version property that is no longer consumed by any module?** The property remains as dead config until a cleanup pass removes it. Per Constitution Principle V, future cleanup is acceptable; this spec does not mandate it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `jwarsv-core/build.gradle` MUST NOT declare the `io.spring.dependency-management` plugin (currently at line 3 per `docs/analysis/01-core-purity-refactor.md:82`).
- **FR-002**: `jwarsv-core/build.gradle` MUST NOT declare the `org.hibernate.orm` plugin (currently at line 4 per `docs/analysis/01-core-purity-refactor.md:83`).
- **FR-003**: `jwarsv-core/build.gradle` MUST NOT declare `org.springframework.boot:spring-boot-starter-data-jpa`, `spring-boot-starter-data-rest`, `com.google.firebase:firebase-admin`, `org.mapstruct:mapstruct`, `com.opencsv:opencsv`, or `org.mapstruct:mapstruct-processor` (currently lines 10-16 per `docs/analysis/01-core-purity-refactor.md:84-89`).
- **FR-004**: The root `jwar-server/build.gradle` `subprojects {}` block MUST NOT inject `org.springframework.boot:spring-boot-starter`, `commons-io:commons-io`, `com.fasterxml.jackson.module:jackson-module-jakarta-xmlbind-annotations`, `io.hypersistence:hypersistence-utils-hibernate-62`, or `io.netty:netty-common` into every subproject (currently `build.gradle:40-45` per `docs/analysis/01-core-purity-refactor.md:98-102`).
- **FR-005**: The root `jwar-server/build.gradle` `subprojects {}` block MUST NOT apply the `io.spring.dependency-management` plugin to every subproject (currently `build.gradle:24` per `docs/analysis/01-core-purity-refactor.md:105`).
- **FR-006**: The `dependencyManagement { imports { mavenBom ... } }` BOM imports for spring-cloud, spring-boot, spring-modulith, and sentry, plus the managed `com.google.firebase:firebase-admin` dependency (currently `jwar-server/build.gradle:57-69` per `docs/analysis/01-core-purity-refactor.md:103`), MUST move to `jwarsv-sboot/build.gradle` or be removed if unused.
- **FR-007**: `jwar-server/build.gradle` MUST preserve the `allprojects { configurations.configureEach { exclude module: "spring-boot-starter-tomcat" } }` block (lines 16-20) — this is constitutional per `.specify/memory/constitution.md:91`.
- **FR-008**: `jwar-server/build.gradle` MUST preserve in `subprojects {}` the `java` plugin apply, Java 17 source/target compatibility, the `-parameters` compile flag, Lombok `compileOnly` + `annotationProcessor`, and the JUnit 5 / Mockito test dependencies (lines 22-25, 36-38, 47-54 per `docs/analysis/01-core-purity-refactor.md:106-107`).
- **FR-009**: `ClassicGameValidator.java` MUST NOT import `org.apache.commons.collections4.CollectionUtils.isEmpty` (currently line 6 per `docs/analysis/01-core-purity-refactor.md:47`).
- **FR-010**: `ClassicGameValidator.java` MUST use pure-Java null-and-empty checks (`list == null || list.isEmpty()`) instead of `CollectionUtils.isEmpty(...)` at the call site currently on line 24.
- **FR-011**: `br/com/bnuuy/jwar/core/domain/User.java` MUST be deleted from `jwarsv-core/src/main/java/` or relocated to `jwarsv-sboot/src/main/java/br/com/bnuuy/jwar/server/domain/User.java` with its package renamed accordingly (per `docs/analysis/01-core-purity-refactor.md:220-234`).
- **FR-012**: After relocation/deletion, the directory `jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/` MUST be removed.
- **FR-013**: A Gradle task `verifyCorePurity` MUST be registered in `jwarsv-core/build.gradle` and wired to `check.dependsOn verifyCorePurity` so that the standard `./gradlew check` flow exercises it.
- **FR-014**: The `verifyCorePurity` task MUST fail the build when any `.java` file under `jwarsv-core/src/main/java/` contains an `import` line referencing `org.springframework`, `jakarta.persistence`, `com.google.firebase`, `jakarta.servlet`, or `org.hibernate`.
- **FR-015**: The `verifyCorePurity` task MUST emit a Gradle error message that names each offending file by path, so the developer can locate and fix violations without re-running.
- **FR-016**: The forbidden-package list in `verifyCorePurity` MUST NOT include Lombok or SLF4J (which are explicitly allowed by Constitution Technology Constraints, `.specify/memory/constitution.md:83-84`).
- **FR-017**: After the refactor, `./gradlew :jwarsv-core:dependencies --configuration runtimeClasspath` MUST display only Lombok (compileOnly drops out at runtime, so SLF4J API + Java module deps remain), JUnit Jupiter (test-scoped), and Mockito (test-scoped). No `org.springframework.*`, `jakarta.persistence.*`, `com.google.firebase.*`, `org.hibernate.*`, `com.fasterxml.jackson.*`, `org.mapstruct.*`, `com.opencsv.*`, `commons-collections4`, `io.netty.*`, or `io.hypersistence.*` may appear.
- **FR-018**: `.ai/tech-best-practices.md:319-324` ("known gotchas") MUST be updated to remove or rewrite the entry stating "jwarsv-core is not a pure domain layer".
- **FR-019**: `.ai/tech-best-practices.md:73-75` ("What may live in jwarsv-core") MUST be updated to explicitly exclude Spring Data JPA, Spring Data REST, Firebase Admin, MapStruct, OpenCSV, and Hibernate.
- **FR-020**: `CLAUDE.md` "Project Structure" section MUST be updated so the `jwarsv-core` line reflects the pure-Java constraint.
- **FR-021**: The refactor MUST NOT change the direction of inter-module dependencies. `jwarsv-sboot/build.gradle` continues to declare `implementation project(":jwarsv-core")` (per `docs/analysis/01-core-purity-refactor.md:182-184`); `jwarsv-core/build.gradle` MUST NOT reference `:jwarsv-sboot` in any configuration.
- **FR-022**: The refactor MUST NOT alter any `.java` file under `jwarsv-core/src/main/java/` beyond the single edit to `ClassicGameValidator.java` (lines 6 and 24) and the deletion or relocation of `User.java` per FR-011. No method bodies, no class signatures, no Lombok annotations change.
- **FR-023**: The refactor MUST NOT alter any `.java` file under `jwarsv-core/src/test/java/`. Test code is left intact to demonstrate behavioral preservation.
- **FR-024**: The six migration steps documented in `docs/analysis/01-core-purity-refactor.md:200-360` SHOULD be followed in the order listed. Each step leaves the build green per the verification commands in `docs/analysis/01-core-purity-refactor.md:412-431`.
- **FR-025**: Spring Modulith, Sentry, Spring Cloud, and Spring Boot BOM imports MAY be retained inside `jwarsv-sboot/build.gradle` `dependencyManagement {}` if sboot continues to consume managed versions. They MUST NOT remain in the root `subprojects {}` block.
- **FR-026**: `commons-io:commons-io` MUST be removed from `subprojects {}`. If `jwarsv-sboot` legitimately needs it (verify via grep before refactor), it is added explicitly to `jwarsv-sboot/build.gradle`; otherwise it is dropped project-wide.
- **FR-027**: `io.netty:netty-common` MUST be removed from `subprojects {}`. If Firebase Admin (in sboot) needs it at runtime, it is added explicitly to `jwarsv-sboot/build.gradle`; otherwise it is dropped project-wide.
- **FR-028**: The `spring-cloud-dependencies` BOM, `spring-boot-dependencies` BOM, `spring-modulith-bom`, and `sentry-bom` MUST relocate to `jwarsv-sboot/build.gradle` `dependencyManagement { imports { mavenBom ... } }` block. After relocation, `./gradlew :jwarsv-core:dependencyManagement` shows no Spring/Cloud/Modulith/Sentry-managed versions.
- **FR-029**: The managed `com.google.firebase:firebase-admin:${firebaseAdminVersion}` declaration MUST relocate from the root-level `dependencyManagement { dependencies { ... } }` block into `jwarsv-sboot/build.gradle`. After relocation, sboot's `implementation "com.google.firebase:firebase-admin"` continues to resolve to the same version via the BOM-style dependency declaration.
- **FR-030**: Version properties referenced by relocated BOMs (e.g., `springCloudVersion`, `springModulithVersion`, `sentryVersion`, `firebaseAdminVersion` in `gradle.properties`) MAY remain in `gradle.properties` since they are project-wide constants. They become consumed only by `jwarsv-sboot/build.gradle` after relocation.
- **FR-031**: The reviewer checklist in `.ai/tech-best-practices.md:291` MAY be augmented with an explicit "run `./gradlew :jwarsv-core:check` to confirm purity" item once `verifyCorePurity` lands. This is documentation hygiene, not a binding requirement.

### Non-Functional Requirements

- **NFR-001**: The refactor MUST be completable in six discrete commits (one per migration step per `docs/analysis/01-core-purity-refactor.md:200-360`). Each commit independently leaves `./gradlew build` green.
- **NFR-002**: The `verifyCorePurity` task MUST execute in under 1 second on a project with ~30 Java files. Implementation is a single-pass file scan; no Gradle dependency-graph traversal is required.
- **NFR-003**: The refactor MUST NOT introduce any new third-party Gradle plugin. The `java` plugin (already present) suffices.
- **NFR-004**: The refactor MUST be reversible. Reverting the six commits restores the pre-refactor state with no residual artifacts in `gradle.properties` or `settings.gradle`.

### Key Entities

- **Core module (`jwarsv-core`)**: A Gradle subproject containing pure game logic (POJOs, enums, validators, evaluators). Post-refactor, depends only on Java 17 stdlib, Lombok (compile-only), SLF4J API, JUnit 5, Mockito. No Spring, no JPA, no Firebase, no HTTP, no persistence adapters. Owns the directory tree `src/main/java/br/com/bnuuy/jwar/core/{exceptions,game}` and `src/test/java/...`.
- **Sboot module (`jwarsv-sboot`)**: The Spring Boot application module. Owns all framework, persistence, web, and infrastructure bindings. Depends on `:jwarsv-core` (one-way only). Will host the relocated `User.java` (or its successor `@Entity`) when persistence ships.
- **`verifyCorePurity` task**: A Gradle build task registered in `jwarsv-core/build.gradle`, wired to `check.dependsOn`. Scans `src/main/java/` for forbidden import patterns and fails fast with a named-offenders error message.
- **Forbidden-packages list**: The set `{ org.springframework, jakarta.persistence, com.google.firebase, jakarta.servlet, org.hibernate }`. May extend over time (e.g., `org.apache.commons.collections4` if regression risk grows).
- **Allowed-packages list** (implicit): `java.*`, `lombok.*`, `org.slf4j.*`, `br.com.bnuuy.jwar.*`, plus JUnit 5 / Mockito at test scope. Anything else inside `jwarsv-core/src/main/java/` is a violation.
- **Migration step sequence**: Six discrete steps as documented in `docs/analysis/01-core-purity-refactor.md:200-360`. Each step is the smallest unit that leaves `./gradlew build` green. The sequence is: (1) replace `commons-collections4.isEmpty`; (2) relocate `User.java`; (3) slim `jwarsv-core/build.gradle`; (4) pull baggage out of root `subprojects {}`; (5) add `verifyCorePurity`; (6) refresh documentation.

### Out of Scope

The following items are explicitly OUT OF SCOPE of this spec to keep the refactor mechanical and reversible:

- Introducing a hexagonal/ports-and-adapters package layout inside `jwarsv-sboot`. The persistence-adapter pattern described in `docs/analysis/01-core-purity-refactor.md:182-191` is referenced as future work, not mandated here.
- Creating a real `UserJpaEntity` / `UserJpaRepository` / `UserPersistenceAdapter` triad. The `User.java` placeholder is deleted or relocated; the JPA work follows when persistence ships.
- Adding MapStruct mappers or OpenCSV consumers. These dependencies are dropped outright; they re-enter only when a real consumer surfaces, and only on `jwarsv-sboot`.
- Introducing a Gradle version catalog (`libs.versions.toml`). The existing `gradle.properties`-based version management stays.
- Refactoring the existing core tests. Test code is left untouched per FR-023.
- Auditing `jwarsv-sboot` for its own purity. Sboot is the framework module and is permitted (encouraged) to use Spring, JPA, Firebase, etc.
- Modifying the `jwar-commons` placeholder module. It stays untouched per `CLAUDE.md`.
- Renaming any class, package, or method in `jwarsv-core`. Only the one import in `ClassicGameValidator.java` and the `User.java` placeholder change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `./gradlew :jwarsv-core:dependencies --configuration runtimeClasspath` lists at most 5 distinct artifacts at runtime scope: Lombok-generated code carries no runtime dep, so the visible runtime tree shows only the project's own jar plus SLF4J API and the JDK. Zero Spring, JPA, Firebase, Hibernate, Jackson, MapStruct, OpenCSV, Netty, Commons-IO, or Hypersistence artifacts appear.
- **SC-002**: `grep -rn -E "^import " jwar-server/jwarsv-core/src/main/java/ | grep -v -E "^.*: ?import (java\.|lombok\.|org\.slf4j\.|br\.com\.bnuuy\.jwar\.)"` returns zero lines.
- **SC-003**: `grep -rn -E "@(Service|Component|Repository|Configuration|Bean|Autowired|RestController|Entity|Table|Column|Id|GeneratedValue|MappedSuperclass)" jwar-server/jwarsv-core/src/main/java/` returns zero lines (consistent with `docs/analysis/01-core-purity-refactor.md:65-68`).
- **SC-004**: `./gradlew :jwarsv-core:test` passes 100% of pre-existing tests without any test modification.
- **SC-005**: `./gradlew :jwarsv-sboot:build` succeeds with no version-resolution errors after BOM imports relocate.
- **SC-006**: `./gradlew clean build` from `jwar-server/` succeeds with the same overall pass/fail outcome as the pre-refactor baseline.
- **SC-007**: The Tomcat exclusion remains in `allprojects {}` after the refactor (per `.specify/memory/constitution.md:91`). Verified by `grep -n "spring-boot-starter-tomcat" jwar-server/build.gradle` returning at least one match in the `allprojects {}` block.
- **SC-008**: `./gradlew :jwarsv-core:check` runs `verifyCorePurity` automatically and passes when core is clean.
- **SC-009**: Artificially introducing `import org.springframework.stereotype.Component;` into any file under `jwarsv-core/src/main/java/` causes `./gradlew :jwarsv-core:check` to fail with a Gradle error naming the offending file.
- **SC-010**: `find jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/ -type f 2>/dev/null` returns no `.java` files (the directory is empty or absent).
- **SC-011**: Reading `.ai/tech-best-practices.md` after the refactor returns no surviving statement of the form "jwarsv-core is not a pure domain layer".
- **SC-012**: `CLAUDE.md` "Project Structure" section describes `jwarsv-core` as a pure-Java module with no Spring web/persistence dependencies.
- **SC-013**: The refactor introduces zero behavioral changes to game logic. All pre-existing `jwarsv-core` tests pass byte-for-byte equivalent assertions; no test bodies are modified.
- **SC-014**: `grep -c "implementation project" jwar-server/jwarsv-core/build.gradle` returns 0 (core never depends on any other module).
- **SC-015**: `grep -c "implementation project(\":jwarsv-core\")" jwar-server/jwarsv-sboot/build.gradle` returns 1 (sboot depends on core, exactly once, per `.specify/memory/constitution.md:66-67`).
- **SC-016**: Removing the `verifyCorePurity` task from `jwarsv-core/build.gradle` followed by `./gradlew :jwarsv-core:check` continues to pass — the task is a guard rail, not a load-bearing test. Reinstating it must not break the build.
- **SC-017**: The core jar produced by `./gradlew :jwarsv-core:jar` weighs less after the refactor than before, because Spring/Hibernate/Jackson/Firebase classes no longer participate in transitive dependency resolution. Concretely, comparing `ls -la jwarsv-core/build/libs/*.jar` before and after, the after-size is unchanged or smaller (the jar itself contains only core's own classes; the change shows up as fewer entries on the runtime classpath, not in the jar size — verified via SC-001).
- **SC-018**: `./gradlew :jwarsv-core:dependencyManagement --configuration runtimeClasspath` (or equivalent task showing managed versions) shows no Spring Cloud, Spring Boot, Spring Modulith, or Sentry managed versions on core. The same task on `:jwarsv-sboot` shows all four BOMs active.
- **SC-019**: A diff of `./gradlew :jwarsv-sboot:dependencies` output before and after the refactor shows the same resolved versions for every artifact sboot depends on. No silent version drift.
- **SC-020**: The Constitution Principle I compliance can be verified by an independent reviewer in under 60 seconds via the grep commands in SC-002 and SC-003.
- **SC-021**: The six-commit migration sequence (NFR-001) produces a `git log --oneline -6` that maps 1:1 to the six migration steps in `docs/analysis/01-core-purity-refactor.md:200-360`. Each commit message references the step number for traceability.
- **SC-022**: `./gradlew :jwarsv-core:check` runtime stays under 5 seconds end-to-end (existing tests + `verifyCorePurity`).
- **SC-023**: Reverting the refactor via `git revert <six commits>` restores the original behavior; `./gradlew clean build` continues to pass on the reverted tree (demonstrating refactor reversibility per NFR-004).

## Assumptions

- The `jwar-commons` module remains a placeholder per `CLAUDE.md` "Project Structure" and `jwar-server/settings.gradle:1-5`. No action required there.
- `jwarsv-sboot/build.gradle` already declares `implementation project(":jwarsv-core")` (line 17 per `docs/analysis/01-core-purity-refactor.md:182-184`). The refactor must not change the direction of this dependency.
- Lombok `@Slf4j` generates a private static `org.slf4j.Logger` reference. SLF4J API is provided transitively by Lombok generation at compile time; no explicit `slf4j-api` declaration is added to core. The binding (logback-classic or equivalent) is owned by sboot's Spring Boot starter and is not needed in core for compilation or test execution.
- MapStruct and OpenCSV are dropped outright from the repo. `grep -rn "@Mapper" .` returns nothing and `grep -rn "opencsv\|CSVReader\|CSVWriter" .` returns nothing as of the analysis snapshot (`docs/analysis/01-core-purity-refactor.md:395-398`). They re-enter only when a real consumer surfaces, and only in `jwarsv-sboot`.
- Spring Modulith and Sentry BOMs may be retained inside `jwarsv-sboot/build.gradle` `dependencyManagement {}` if sboot continues to consume managed versions; otherwise removed.
- The migration is performed in the ordered six steps documented in `docs/analysis/01-core-purity-refactor.md:200-360`. Each step leaves the build green at each checkpoint; verification commands are listed in `docs/analysis/01-core-purity-refactor.md:412-431`.
- Constitution Principle I (`.specify/memory/constitution.md:30-38`) is the authoritative target end-state. Any future ambiguity about "what may live in core" defers to Principle I plus Principle V (Simplicity & YAGNI) — add a dependency only when a real consumer exists.
- Git operations (commit, push, PR) are performed manually by the user per `CLAUDE.md` "Git Workflow" — not by any automated agent.
- The user accepts that the `verifyCorePurity` task uses simple substring matching on import lines. False positives (e.g., `"org.springframework"` mentioned in a Javadoc) can be mitigated by scoping the scan to `import ` lines per the edge case above.
- The constitution at `.specify/memory/constitution.md` is version 1.0.0 as of 2025-05-15. This spec aligns with Principle I (Game Logic Isolation, lines 30-38), Principle IV (Modular Architecture, lines 57-68), and Principle V (Simplicity & YAGNI, lines 70-76). No constitution amendment is required to land this refactor.
- The `.ai/tech-best-practices.md` document is treated as project-internal best-practice guidance that lags reality. Updating it (US5) is a courtesy to future contributors, not a binding gate.
- The single non-pure import is verified absent by re-running the grep commands documented in `docs/analysis/01-core-purity-refactor.md:60-68` after each migration step. The grep is the authoritative compliance check; the Gradle task `verifyCorePurity` mechanizes it.
- No downstream consumer of `jwarsv-core` exists today besides `jwarsv-sboot` (per `jwar-server/settings.gradle:1-5` listing only the two modules). Future consumers (e.g., a CLI or a desktop UI) inherit the purity guarantee for free.
- The `User.java` placeholder is currently empty (`public class User {}` per `docs/analysis/01-core-purity-refactor.md:71-74`). Deletion is preferred over relocation if no immediate persistence work is scheduled. The user may choose either path per US3 acceptance #1.
- The `verifyCorePurity` Gradle snippet in `docs/analysis/01-core-purity-refactor.md:325-345` is a reference implementation. The actual implementation may use a different DSL idiom (e.g., a Groovy closure, a Kotlin function, or a third-party plugin) so long as the behavior contract in FR-014 through FR-016 is honored.
- After the refactor, the project's Gradle version-catalog (if/when one is introduced) MAY centralize version constants further. This is out of scope for the present spec.
- The user accepts that the refactor's primary risk surface is the root `build.gradle` `subprojects {}` block restructure (step 4 per `docs/analysis/01-core-purity-refactor.md:277-314`). Per the risk callouts in `docs/analysis/01-core-purity-refactor.md:302-309`, the highest-impact verification is `./gradlew clean build` from `jwar-server/` immediately after step 4 lands.
- The Constitution version 1.0.0 was ratified 2025-05-15 (`.specify/memory/constitution.md:118`). This spec was authored 2026-05-15, one year later. No constitutional amendments are implied; the spec aligns to the existing ratified principles.

## References

- Constitution Principle I (Game Logic Isolation): `.specify/memory/constitution.md:30-38`
- Constitution Principle IV (Modular Architecture): `.specify/memory/constitution.md:57-68`
- Constitution Technology Constraints (Lombok allowed, Tomcat excluded): `.specify/memory/constitution.md:78-91`
- Constitution Governance Compliance: `.specify/memory/constitution.md:114-116`
- Analysis document (authoritative): `docs/analysis/01-core-purity-refactor.md`
- Tech audit gotchas: `.ai/tech-best-practices.md:319-324`
- "What may live in jwarsv-core": `.ai/tech-best-practices.md:68-82`
- Reviewer checklist: `.ai/tech-best-practices.md:291`
- The one non-pure import in core source: `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/game/utils/ClassicGameValidator.java:6`
- The empty placeholder to relocate: `jwar-server/jwarsv-core/src/main/java/br/com/bnuuy/jwar/core/domain/User.java`
- Core build file (current violations): `jwar-server/jwarsv-core/build.gradle:3-16`
- Root build leakage: `jwar-server/build.gradle:22-71`
- Sboot build (target for relocated deps): `jwar-server/jwarsv-sboot/build.gradle:5-55`
