#!/bin/bash

# Script pour synchroniser les dépendances et résoudre les erreurs "Unresolved reference"

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║   🔧 Synchronisation des dépendances Gradle                  ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "Ce script va rafraîchir les dépendances Gradle pour résoudre"
echo "les erreurs 'Unresolved reference coil' dans Android Studio."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔄 Étape 1/3 : Nettoyage du projet..."
./gradlew clean

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📦 Étape 2/3 : Rafraîchissement des dépendances..."
./gradlew --refresh-dependencies :app:dependencies

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔨 Étape 3/3 : Compilation Kotlin..."
./gradlew :app:compileDebugKotlin

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Synchronisation terminée !"
echo ""
echo "📝 Prochaines étapes dans Android Studio :"
echo ""
echo "1. File > Sync Project with Gradle Files (🐘 icône Sync)"
echo "2. Si les erreurs persistent : File > Invalidate Caches / Restart"
echo "3. Attendez que l'indexation se termine"
echo ""
echo "Les erreurs 'Unresolved reference coil' devraient disparaître."
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

