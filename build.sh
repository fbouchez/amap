#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
export PATH="$HOME/Installs/gradle/bin:$PATH"

echo "=== AMAP - Build ==="
echo "Gradle: $(gradle --version 2>&1 | grep "^Gradle")"
echo ""

cd "$DIR"
gradle assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx4096m" "$@"

echo ""
echo "APK: $DIR/app/build/outputs/apk/debug/app-debug.apk"
