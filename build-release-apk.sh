#!/bin/bash

# Script pour générer l'APK signé de VinylCollection
# Usage: ./build-release-apk.sh

echo "🚀 Construction de l'APK VinylCollection en mode Release..."
echo ""

# Nettoyer le projet
echo "🧹 Nettoyage du projet..."
./gradlew clean

# Générer l'APK debug d'abord pour vérifier que tout fonctionne
echo "🔨 Génération de l'APK debug pour test..."
./gradlew assembleDebug

# Vérifier si l'APK debug a été généré
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ APK debug généré avec succès!"
    DEBUG_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "   Taille: $DEBUG_SIZE"
    echo "   Emplacement: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Échec de la génération de l'APK debug"
    exit 1
fi

echo ""
echo "🔒 Génération de l'APK release signé..."
./gradlew assembleRelease

# Vérifier si l'APK release a été généré
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo "✅ APK release généré avec succès!"
    RELEASE_SIZE=$(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)
    echo "   Taille: $RELEASE_SIZE"
    echo "   Emplacement: app/build/outputs/apk/release/app-release.apk"
    echo ""
    echo "📦 Vous pouvez maintenant installer l'APK sur votre appareil:"
    echo "   adb install app/build/outputs/apk/release/app-release.apk"
else
    echo "❌ Échec de la génération de l'APK release"
    echo "   Vérification des autres emplacements possibles..."
    find app/build -name "*.apk" -type f 2>/dev/null
    exit 1
fi

echo ""
echo "🎉 Build terminé avec succès!"

