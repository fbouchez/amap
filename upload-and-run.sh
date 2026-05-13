#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
CSV="$DIR/current.csv"
PACKAGE="com.amap.app"

# Check if any device is connected
DEVICE=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | grep "device$" | head -1 | awk '{print $1}' || true)

if [ -z "$DEVICE" ]; then
    echo "Aucun appareil connecté."
    echo "Branche ton téléphone et réessaye."
    exit 1
fi

echo "=== Build APK ==="
"$DIR/build.sh"

echo "=== Install sur $DEVICE ==="
adb -s "$DEVICE" install -r "$APK"

# Push current.csv via intent extra (base64) — fiable sur tous les appareils
EXTRA=""
if [ -f "$CSV" ]; then
    echo "=== Envoi de current.csv ==="
    CSV_B64=$(base64 -w0 "$CSV")
    EXTRA="--es csv_b64 $CSV_B64"
fi

echo "=== Lancement ==="
adb -s "$DEVICE" shell am start -n "$PACKAGE/.MainActivity" $EXTRA

echo ""
echo "App lancée !"
