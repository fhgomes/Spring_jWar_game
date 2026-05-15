# jWar Game - Project CLAUDE.md

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->

## Project Overview

**jWar** (bnuuy-war) is a digital implementation of the classic Brazilian board game "War" (similar to Risk). It is a multiplayer turn-based strategy game where players conquer territories, exchange cards, and complete secret objectives to win.

**Author**: Fernando H. Gomes (br.com.bnuuy)
**License**: MIT

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.5 (with Undertow, not Tomcat)
- **Build Tool**: Gradle (multi-module)
- **ORM**: Hibernate 6.6 / Spring Data JPA
- **Database**: PostgreSQL (runtime) / H2 (dev/test)
- **Migrations**: Flyway 10.x
- **Architecture**: Spring Modulith
- **Auth**: Firebase Admin SDK
- **Monitoring**: Micrometer + Prometheus + Sentry
- **Payments**: Stripe, StarkBank (PIX)
- **Other**: Lombok, MapStruct, OpenCSV, Jackson, OpenFeign
- **Testing**: JUnit 5, Mockito
- **Cloud**: GraalVM Native Image support

## Project Structure

```
Spring_jWar_game/
├── jwar-commons/          # Shared common library (currently a placeholder)
├── jwar-server/           # Main server (Gradle multi-module)
│   ├── build.gradle       # Root build config (all subprojects config)
│   ├── gradle.properties  # Versions and settings
│   ├── settings.gradle    # Modules: jwarsv-core, jwarsv-sboot
│   ├── jwarsv-core/       # Core game engine (pure game logic, no Spring web)
│   │   └── src/main/java/br/com/bnuuy/jwar/core/
│   │       ├── domain/         # User domain entity
│   │       ├── exceptions/     # GameRulesException
│   │       └── game/           # Core game engine
│   │           ├── ClassicGame.java          # Main game orchestrator
│   │           ├── ClassicGameAttacker.java  # Attack dice mechanics
│   │           ├── ClassicGameAttackResProcessor.java  # Post-attack processing
│   │           ├── ClassicGameConstants.java # Phase constants
│   │           ├── ClassicGameLobby.java     # Lobby/matchmaking
│   │           ├── ClassicGamePActions.java  # Player actions
│   │           ├── domain/      # Game domain (Player, Country, Continent, AttackResultVO)
│   │           ├── map/         # Enums: countries, continents, cards, colors, objectives
│   │           └── utils/       # Validators, distributors, evaluators
│   │               └── objective/  # Objective evaluators
│   └── jwarsv-sboot/     # Spring Boot application entry point
│       └── src/main/
│           ├── java/.../JWarBackCoreApplication.java
│           └── resources/application.yml
├── .specify/              # Spec-Kit project structure
└── .claude/               # Claude Code configuration
```

## Game Domain

The game implements the classic Brazilian "War" board game:
- **42 territories** across 6 continents (South America, North America, Europe, Africa, Oceania, Asia)
- **3-6 players** with color assignments (Red, Blue, Green, Yellow, Purple, Gray)
- **Turn phases**: Add troops -> Attack -> Move troops
- **Card system**: Country cards with shapes (circle, triangle, square) + jokers; exchangeable for bonus troops
- **Objectives**: Destroy player, conquer continents, conquer X territories (with min troops)
- **First two rounds**: Special troop distribution rules

## Coding Conventions

- Package: `br.com.bnuuy.jwar`
- Use Lombok annotations (`@Getter`, `@Setter`, `@Slf4j`, etc.)
- Enums prefixed with `E` (e.g., `EClassicCountries`, `EGameColors`)
- Value Objects suffixed with `VO` (e.g., `AttackResultVO`)
- Game text/descriptions in Portuguese (Brazilian)
- Tests use JUnit 5 + Mockito
- Keep game core logic independent of Spring Web (jwarsv-core has no web dependencies)

## Build Commands

```bash
# Run from jwar-server/ directory
./gradlew clean                              # Clean
./gradlew build                              # Build with tests
./gradlew bootRun -Dspring.profiles.active=dev  # Run locally
./gradlew clean bootBuildImage               # Build Docker image
```

## Permissions

Claude has full autonomy on this project for:
- Reading, writing, and editing any files
- Running build and test commands
- Creating new files and directories
- Searching and exploring the codebase
- Web searches for technical references

## Git Workflow

- **NEVER** create git commits, push changes, or create pull requests
- User handles all git operations manually
- Only make code changes when requested

## Spec-Kit Integration

This project uses [GitHub Spec-Kit](https://github.com/github/spec-kit) for spec-driven development.
- Constitution: `.specify/memory/constitution.md`
- Templates: `.specify/templates/`
- Skills: `.claude/skills/speckit-*/`
