#!/usr/bin/env bash
set -euo pipefail

# Simple deploy script for VPS
# Usage: copy `javaMusicApp/.env` to the server (do NOT commit it), then run this script from repo root.

ENV_FILE="./javaMusicApp/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing $ENV_FILE — copy your real .env to that path (based on .env.template)" >&2
  exit 1
fi

echo "Building and deploying app using docker compose (env file: $ENV_FILE)"

export COMPOSE_DOCKER_CLI_BUILD=1
export DOCKER_BUILDKIT=1

# Build only the app image first (faster iterative deploys)
docker compose --env-file "$ENV_FILE" build app

# Start/recreate the app service without touching other services
docker compose --env-file "$ENV_FILE" up -d --no-deps --remove-orphans app

echo "Deployment finished. Tail logs with: docker compose logs -f app"
