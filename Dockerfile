# syntax=docker/dockerfile:1.7
#
# jWar — multi-stage build:
#   1. frontend-builder  : Node 20, builds the Vite production bundle.
#   2. backend-builder   : JDK 17, copies the bundle as Spring static
#                          resources, builds the Spring Boot fat jar.
#   3. runtime           : JRE 17, non-root user, healthcheck on /api/health.
#
# Optional `runtime-native` target (FR-014 in spec 011) is intentionally
# omitted from v1 — add later once GraalVM build is stabilized.

############################################
# Stage 1: frontend-builder
############################################
FROM node:20-alpine AS frontend-builder

WORKDIR /build

# Cache npm install layer
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund

# Build frontend
COPY frontend/ ./
# Vite defaults to ./dist; we deliberately override outDir so a stand-alone
# `docker build` produces the bundle in a known location regardless of how
# `vite.config.ts` is configured by the frontend agent.
RUN npx vite build --outDir dist --emptyOutDir

############################################
# Stage 2: backend-builder
############################################
FROM eclipse-temurin:17-jdk-alpine AS backend-builder

# Gradle wrapper needs bash; alpine ships only ash. The wrapper works with
# ash too, but install bash + curl for diagnostics anyway.
RUN apk add --no-cache bash curl

WORKDIR /build

# Copy the gradle wrapper and configuration first so the Gradle dependency
# resolution layer is cached when only source files change.
COPY jwar-server/gradlew jwar-server/gradlew.bat ./
COPY jwar-server/gradle ./gradle
COPY jwar-server/settings.gradle jwar-server/build.gradle jwar-server/gradle.properties ./
COPY jwar-server/jwarsv-core/build.gradle ./jwarsv-core/
COPY jwar-server/jwarsv-sboot/build.gradle ./jwarsv-sboot/

# Pre-warm the dependency cache (best-effort; ignore failure if offline)
RUN chmod +x ./gradlew && ./gradlew --no-daemon --version

# Copy full source
COPY jwar-server/ ./

# Embed the production frontend bundle as Spring Boot static resources.
COPY --from=frontend-builder /build/dist ./jwarsv-sboot/src/main/resources/static

# Build the fat jar
RUN ./gradlew --no-daemon :jwarsv-sboot:bootJar -x test

############################################
# Stage 3: runtime
############################################
FROM eclipse-temurin:17-jre-alpine AS runtime

# `curl` is needed by HEALTHCHECK; `tini` for proper signal handling.
RUN apk add --no-cache curl tini \
    && addgroup -S jwar \
    && adduser -S -G jwar -h /app jwar

WORKDIR /app

COPY --from=backend-builder --chown=jwar:jwar /build/jwarsv-sboot/build/libs/*.jar /app/app.jar

USER jwar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Dserver.shutdown=graceful"

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=4 \
    CMD curl -fsS http://localhost:8080/api/health || exit 1

ENTRYPOINT ["/sbin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
