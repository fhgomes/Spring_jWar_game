<!--
  Sync Impact Report
  ==================
  Version change: 0.0.0 (template) → 1.0.0 (initial ratification)

  Added principles:
    - I. Game Logic Isolation
    - II. Test-Driven Game Rules
    - III. Domain Fidelity
    - IV. Modular Architecture
    - V. Simplicity & YAGNI

  Added sections:
    - Technology Constraints
    - Development Workflow
    - Governance

  Templates requiring updates:
    ✅ .specify/templates/plan-template.md — no changes needed (generic)
    ✅ .specify/templates/spec-template.md — no changes needed (generic)
    ✅ .specify/templates/tasks-template.md — no changes needed (generic)

  Follow-up TODOs: none
-->

# jWar Game Constitution

## Core Principles

### I. Game Logic Isolation

The core game engine (`jwarsv-core`) MUST remain independent of web
frameworks, HTTP, or infrastructure concerns. All game rules, mechanics,
and state transitions live in pure Java classes with no Spring Web
dependencies. The `jwarsv-sboot` module is the only place where Spring
Boot web, persistence, and infrastructure bindings are allowed.
This separation ensures the game engine is independently testable,
portable, and free from framework coupling.

### II. Test-Driven Game Rules

Every game rule implementation MUST have corresponding unit tests.
Game mechanics (attack dice, troop distribution, card exchange,
objective evaluation, turn flow) are deterministic logic that MUST
be verified through automated tests. Tests use JUnit 5 and Mockito.
New game rules MUST NOT be merged without passing test coverage.

### III. Domain Fidelity

The game MUST faithfully implement the rules of the classic Brazilian
"War" board game. All territory names, continent compositions, card
shapes, objective types, and troop distribution rules MUST match the
original board game. Game text and descriptions MUST be in Brazilian
Portuguese. Any deviation from the original rules MUST be explicitly
documented and justified.

### IV. Modular Architecture

The project follows Gradle multi-module structure with clear
responsibility boundaries:
- `jwarsv-core`: Game engine, domain objects, validators, evaluators
- `jwarsv-sboot`: Spring Boot application, REST endpoints, persistence
- `jwar-commons`: Shared utilities (when needed)

Each module MUST declare only the dependencies it directly uses.
Cross-module dependencies MUST flow inward (sboot depends on core,
never the reverse). Spring Modulith enforces module boundaries at
the application level.

### V. Simplicity & YAGNI

Code MUST solve the current requirement without speculative
abstractions. Prefer three similar lines of code over a premature
abstraction. Do not add error handling for impossible scenarios.
Do not design for hypothetical future requirements. Every piece of
complexity MUST justify its existence against a simpler alternative.

## Technology Constraints

- **Java 17**: Source and target compatibility. Do not use preview features.
- **Spring Boot 3.3.x**: With Undertow (Tomcat excluded project-wide).
- **Gradle**: Multi-module build. Versions centralized in `gradle.properties`.
- **Lombok**: Use for boilerplate reduction (`@Getter`, `@Setter`, `@Slf4j`).
  Do not use `@Data` on JPA entities.
- **Naming conventions**:
  - Enums: `E` prefix (e.g., `EClassicCountries`)
  - Value Objects: `VO` suffix (e.g., `AttackResultVO`)
  - Package root: `br.com.bnuuy.jwar`
- **Database**: PostgreSQL for production, H2 for development/testing.
  Schema managed by Flyway migrations.
- **No Tomcat**: The entire project excludes `spring-boot-starter-tomcat`.

## Development Workflow

- All code changes MUST compile and pass existing tests before being
  considered complete.
- Build command: `./gradlew build` (from `jwar-server/` directory).
- Git operations (commit, push, PR) are handled manually by the developer.
  Automated agents MUST NOT perform git operations.
- New features SHOULD follow the spec-driven development flow:
  constitution -> specify -> plan -> tasks -> implement.

## Governance

This constitution is the authoritative reference for all development
decisions on the jWar project. It supersedes informal conventions and
ad-hoc practices.

- **Amendments**: Any change to this constitution MUST be documented
  with a version bump, rationale, and updated date.
- **Versioning**: Follows semantic versioning (MAJOR.MINOR.PATCH).
  MAJOR for principle removals/redefinitions, MINOR for additions,
  PATCH for clarifications.
- **Compliance**: All code contributions MUST be reviewed against
  the principles defined here. Violations MUST be resolved before
  the code is accepted.

**Version**: 1.0.0 | **Ratified**: 2025-05-15 | **Last Amended**: 2025-05-15
