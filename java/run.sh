#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  printf 'Create the root .env from .env.example and set the database passwords.\n' >&2
  exit 1
fi

docker compose up -d --wait
for migration in \
  database/002-accounts.sql \
  database/003-maintenance-reports.sql \
  database/004-report-cancellation.sql \
  database/005-report-resolution.sql; do
  docker compose exec -T database sh -c \
    'psql -v ON_ERROR_STOP=1 -U "$CAMPUSFIX_DB_USER" -d campusfix' < "$migration"
done

exec ./java/gradlew -p java :desktop:run
