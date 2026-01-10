#!/usr/bin/env bash
set -euo pipefail

chmod +x ./gradlew || true

# 依存解決の健全性（キャッシュ壊れ検知）
./gradlew --no-daemon :primus2:dependencies > /dev/null

# UnitTest/Lint/ビルド
./scripts/test_linux.sh
