#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
CSV="$DIR/current.csv"
PACKAGE="com.amap.app"

# Check connected devices
DEVICES=$(adb devices | grep -v "List of devices attached" | grep -v "^$" | grep "device$" | awk '{print $1}' || true)
COUNT=$(echo "$DEVICES" | grep -c . || true)

if [ "$COUNT" -eq 0 ]; then
    echo "Aucun appareil connecté."
    echo "Branche ton téléphone et réessaye."
    exit 1
fi

if [ "$COUNT" -eq 1 ]; then
    DEVICE="$DEVICES"
else
    echo "Plusieurs appareils détectés :"
    i=1
    while IFS= read -r d; do
        echo "  $i) $d"
        i=$((i + 1))
    done <<< "$DEVICES"
    echo ""
    echo -n "Choisis un appareil (1-$COUNT) : "
    read -r CHOICE
    DEVICE=$(echo "$DEVICES" | sed -n "${CHOICE}p")
    if [ -z "$DEVICE" ]; then
        echo "Choix invalide."
        exit 1
    fi
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
