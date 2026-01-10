#!/usr/bin/env bash
set -euo pipefail

# ルートで実行されている想定
# gradlew 実行権
chmod +x ./gradlew || true

echo "== Android UnitTest =="
./gradlew --no-daemon :primus2:testDebugUnitTest

echo "== Lint(debug) =="
./gradlew --no-daemon :primus2:lintDebug
echo "Lint HTML => primus2/build/reports/lint-results-debug.html"

echo "== Assemble(debug) =="
./gradlew --no-daemon :primus2:assembleDebug

echo "== DONE =="
