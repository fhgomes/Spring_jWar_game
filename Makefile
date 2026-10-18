# jWar — local dev shortcuts. See docs/docker.md for the full guide.
#
# Quick start:
#   cp .env.example .env
#   make up        # boots Postgres + app, exposes http://localhost:8080
#   make logs      # tails the app logs
#   make down      # stops everything

# Load .env so its variables are available to `make psql` etc.
# (silently ignored if .env doesn't exist)
-include .env
export

SHELL := /bin/bash
COMPOSE ?= docker compose

.PHONY: help up down logs rebuild psql shell dev-tools-up dev-tools-down build ps clean

help:
	@echo "jWar — make targets"
	@echo "  make up              Bring up postgres + app (detached)."
	@echo "  make down            Stop and remove containers (keeps the volume)."
	@echo "  make logs            Tail the app logs."
	@echo "  make rebuild         Rebuild the image without cache, then up."
	@echo "  make build           Build the image (with cache)."
	@echo "  make psql            Open psql against the running database."
	@echo "  make shell           Open an interactive shell inside the app container."
	@echo "  make ps              Show the running compose services."
	@echo "  make dev-tools-up    Start the optional dev-tools profile (pgAdmin)."
	@echo "  make dev-tools-down  Stop the dev-tools profile."
	@echo "  make clean           Stop everything and DELETE the Postgres volume."

up:
	$(COMPOSE) up -d

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f app

rebuild:
	$(COMPOSE) build --no-cache
	$(COMPOSE) up -d

build:
	$(COMPOSE) build

psql:
	$(COMPOSE) exec postgres psql -U $${DB_USERNAME:-jwar} -d $${DB_NAME:-jwar}

shell:
	$(COMPOSE) exec app sh

ps:
	$(COMPOSE) ps

dev-tools-up:
	$(COMPOSE) --profile dev-tools up -d

dev-tools-down:
	$(COMPOSE) --profile dev-tools down

clean:
	$(COMPOSE) down -v
