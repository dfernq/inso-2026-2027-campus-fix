#!/usr/bin/env bash
set -e

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  printf 'Create the root .env from .env.example and set the database passwords.\n' >&2
  exit 1
fi

python_command=(python3)
venv_python="$PWD/python/.venv/bin/python"
if [[ "$OSTYPE" == msys* ]]; then
  python_command=(py -3)
  venv_python="$PWD/python/.venv/Scripts/python.exe"
fi

if [[ ! -x "$venv_python" ]]; then
  "${python_command[@]}" -m venv python/.venv
fi
if ! "$venv_python" -c 'import tkinter'; then
  printf 'Install Tkinter for the Python version used by python/.venv, then rerun the launcher.\n' >&2
  exit 1
fi
"$venv_python" -m pip --disable-pip-version-check install -r python/requirements.txt

docker compose up -d --wait
for migration in \
  database/002-accounts.sql \
  database/003-maintenance-reports.sql \
  database/004-report-cancellation.sql \
  database/005-report-resolution.sql; do
  docker compose exec -T database sh -c \
    'psql -v ON_ERROR_STOP=1 -U "$CAMPUSFIX_DB_USER" -d campusfix' < "$migration"
done

cd python
exec "$venv_python" -m campusfix
