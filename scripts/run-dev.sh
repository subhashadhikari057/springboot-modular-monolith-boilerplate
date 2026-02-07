#!/usr/bin/env sh
set -eu

if [ -f ./.env ]; then
  # shellcheck disable=SC1091
  set -a
  . ./.env
  set +a
fi

./mvnw spring-boot:run
