#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

cd "$PROJECT_ROOT"
printf '%s\n' "Focus Tree harness initialization"
printf 'Project root: %s\n' "$PROJECT_ROOT"

if command -v java >/dev/null 2>&1; then
  java -version
else
  printf '%s\n' "WARNING: Java was not found on PATH."
fi

if [ -f "./gradlew" ]; then
  ./gradlew --version
else
  printf '%s\n' "WARNING: Gradle wrapper is not present yet; implement FT-FND-001 first."
fi

if command -v git >/dev/null 2>&1; then
  git status --short
fi

printf '%s\n' "Read AGENTS.md, product/architecture docs, feature_list.json, progress.md, and session-handoff.md before editing."
