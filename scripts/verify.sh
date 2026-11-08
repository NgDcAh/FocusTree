#!/usr/bin/env sh
set -eu

SCOPE=${1:-full}
case "$SCOPE" in
  fast|full) ;;
  *) printf '%s\n' "Usage: ./scripts/verify.sh [fast|full]" >&2; exit 2 ;;
esac

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_ROOT"

if [ ! -f "./gradlew" ]; then
  printf '%s\n' "Gradle wrapper is missing. Implement FT-FND-001 before product features." >&2
  exit 1
fi

if [ "$SCOPE" = "fast" ]; then
  set -- ktlintCheck detekt testDebugUnitTest
else
  set -- ktlintCheck detekt testDebugUnitTest lintDebug assembleDebug
fi

./gradlew "$@" --stacktrace
printf '%s\n' "Verification passed. Record this command and result as feature evidence."
