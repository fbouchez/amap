#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
CSV="$DIR/example.csv"
PACKAGE="com.amap.app"
DEVICES_CONF="${HOME}/.adb-devices"

resolve_serial() {
    awk -v name="$1" '$1 == name {print $2}' "$DEVICES_CONF" 2>/dev/null || true
}

resolve_name() {
    awk -v serial="$1" '$2 == serial {print $1}' "$DEVICES_CONF" 2>/dev/null || true
}

get_connected_serials() {
    adb devices 2>/dev/null | awk '/device$/ {print $1}'
}

if [ $# -ge 1 ]; then
    DEVICE="$1"
    SERIAL=$(resolve_serial "$DEVICE")
    if [ -z "$SERIAL" ]; then
        echo "Device '$DEVICE' introuvable dans $DEVICES_CONF."
        echo "Ajoute une ligne : echo '$DEVICE  SERIAL' >> $DEVICES_CONF"
        exit 1
    fi
else
    mapfile -t CONNECTED < <(get_connected_serials)
    COUNT=${#CONNECTED[@]}

    if [ "$COUNT" -eq 0 ]; then
        echo "Aucun appareil connecté."
        echo "Branche ton téléphone et réessaye."
        echo "Usage direct : $0 <device-name>"
        exit 1
    fi

    if [ "$COUNT" -eq 1 ]; then
        SERIAL="${CONNECTED[0]}"
        NAME=$(resolve_name "$SERIAL")
        if [ -n "$NAME" ]; then
            DEVICE="$NAME"
            echo "=== Appareil : $DEVICE ($SERIAL) ==="
        else
            DEVICE="$SERIAL"
            echo "=== Appareil : $SERIAL ==="
        fi
    else
        echo "Plusieurs appareils détectés :"
        for i in "${!CONNECTED[@]}"; do
            s="${CONNECTED[$i]}"
            NAME=$(resolve_name "$s")
            if [ -n "$NAME" ]; then
                echo "  $((i + 1))) $NAME ($s)"
            else
                echo "  $((i + 1))) $s"
            fi
        done
        echo ""
        echo -n "Choisis un appareil (1-$COUNT) : "
        read -r CHOICE || true
        if ! [[ "$CHOICE" =~ ^[0-9]+$ ]] || [ "$CHOICE" -lt 1 ] || [ "$CHOICE" -gt "$COUNT" ]; then
            echo "Choix invalide."
            exit 1
        fi
        SERIAL="${CONNECTED[$((CHOICE - 1))]}"
        NAME=$(resolve_name "$SERIAL")
        if [ -n "$NAME" ]; then
            DEVICE="$NAME"
        else
            DEVICE="$SERIAL"
        fi
    fi
fi

echo "=== Build APK ==="
"$DIR/build.sh"

echo "=== Install sur $DEVICE ($SERIAL) ==="
adb -s "$SERIAL" install -r "$APK"

EXTRA=""
if [ -f "$CSV" ]; then
    echo "=== Envoi de example.csv ==="
    CSV_B64=$(base64 -w0 "$CSV")
    EXTRA="--es csv_b64 $CSV_B64"
fi

echo "=== Lancement ==="
adb -s "$SERIAL" shell am start -n "$PACKAGE/.MainActivity" $EXTRA

echo ""
echo "App lancée sur $DEVICE ($SERIAL) !"
