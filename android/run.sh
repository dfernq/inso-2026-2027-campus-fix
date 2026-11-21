#!/usr/bin/env bash
set -e -o pipefail

cd "$(dirname "$0")/.."

local_setting() {
  if [[ -r android/local.properties ]]; then
    awk -v prefix="$1=" 'index($0, prefix) == 1 { print substr($0, length(prefix) + 1); exit }' \
      android/local.properties
  fi
}

sdk_dir=${ANDROID_HOME:-$(local_setting sdk.dir)}
if [[ -n "$sdk_dir" ]]; then
  export ANDROID_HOME="$sdk_dir"
  export PATH="$PATH:$sdk_dir/platform-tools:$sdk_dir/emulator"
fi

ensure_device() {
  if adb get-state 2>/dev/null | grep -qx device; then
    return
  fi
  local devices
  devices=$(adb devices | sed '1d; /^[[:space:]]*$/d')
  if [[ -n "$devices" ]]; then
    printf 'Connect one ready device or select it with ANDROID_SERIAL. See adb devices.\n' >&2
    return 1
  fi
  if ! command -v emulator >/dev/null; then
    printf 'Start a device or install the Android Emulator in the SDK Manager.\n' >&2
    return 1
  fi

  local avd_home avd
  avd_home=$(local_setting campusfix.avdHome)
  if [[ -n "$avd_home" ]]; then
    export ANDROID_AVD_HOME="${ANDROID_AVD_HOME:-$avd_home}"
  fi
  avd=$(local_setting campusfix.avd)
  if [[ -z "$avd" ]]; then
    avd=$(emulator -list-avds)
  fi
  if [[ -z "$avd" || "$avd" == *$'\n'* ]]; then
    printf 'Start an emulator in Device Manager or set campusfix.avd in android/local.properties.\n' >&2
    return 1
  fi

  printf 'Starting emulator %s...\n' "$avd"
  mkdir -p android/build
  nohup emulator -avd "$avd" </dev/null >>android/build/emulator.log 2>&1 &
  local emulator_pid=$!
  local attempt
  for ((attempt = 0; attempt < 90; attempt++)); do
    if adb shell -T getprop sys.boot_completed 2>/dev/null | grep -qx 1; then
      return
    fi
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
      break
    fi
    sleep 1
  done
  printf 'Emulator did not finish starting. See android/build/emulator.log.\n' >&2
  return 1
}

if ! command -v adb >/dev/null; then
  printf 'adb is unavailable. Install Android SDK Platform Tools and configure the SDK.\n' >&2
  exit 1
fi
if ! settings=$(grep -E '^CAMPUSFIX_DB_(URL|USER|PASSWORD)=' .env); then
  printf 'Create the root .env from .env.example and set the database connection values.\n' >&2
  exit 1
fi

ensure_device
docker compose up -d --wait
docker compose exec -T database sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$CAMPUSFIX_DB_USER" -d campusfix' \
  < database/002-accounts.sql
docker compose exec -T database sh -c \
  'psql -v ON_ERROR_STOP=1 -U "$CAMPUSFIX_DB_USER" -d campusfix' \
  < database/003-maintenance-reports.sql
./java/gradlew -p android :app:installDebug
adb reverse tcp:5432 tcp:5432
printf '%s\n' "$settings" | adb shell -T \
  'run-as org.campusfix.android sh -ec "mkdir -p files; cat > files/.env.tmp; mv files/.env.tmp files/.env"'
adb shell am start -S -W -n org.campusfix.android/.view.MainActivity
