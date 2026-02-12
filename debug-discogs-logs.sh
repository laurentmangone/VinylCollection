#!/bin/bash

# Script pour afficher les logs liés à Discogs et au chargement d'images

echo "📱 Démarrage des logs Discogs..."
echo "Appuyez sur Ctrl+C pour arrêter"
echo ""

ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"

# Vider les logs
"$ADB_PATH" logcat -c

# Afficher les logs filtrés
"$ADB_PATH" logcat -v color \
  VinylApp:D \
  Discogs:D \
  DiscogsAdapter:D \
  VinylEdit:D \
  CropCover:D \
  AndroidRuntime:E \
  *:S

