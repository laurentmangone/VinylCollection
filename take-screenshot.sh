#!/bin/bash

# Script pour prendre des screenshots via ADB
# Usage: ./take-screenshot.sh <nom_fichier>
# Exemple: ./take-screenshot.sh 01_vinyl_list

# Couleurs pour la sortie
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier si ADB est installé
if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ ADB n'est pas installé ou n'est pas dans le PATH${NC}"
    echo -e "${YELLOW}💡 Installez Android Platform Tools ou utilisez Android Studio${NC}"
    exit 1
fi

# Vérifier qu'un nom de fichier est fourni
if [ -z "$1" ]; then
    echo -e "${RED}❌ Nom de fichier manquant${NC}"
    echo -e "${YELLOW}Usage: $0 <nom_fichier>${NC}"
    echo ""
    echo "Exemples:"
    echo "  $0 01_vinyl_list"
    echo "  $0 02_vinyl_list_search"
    echo "  $0 03_vinyl_list_empty"
    echo ""
    echo "Liste complète des screenshots à prendre:"
    echo "  01_vinyl_list"
    echo "  02_vinyl_list_search"
    echo "  03_vinyl_list_empty"
    echo "  04_vinyl_edit_create"
    echo "  05_vinyl_edit_fields"
    echo "  06_vinyl_edit_cover"
    echo "  07_vinyl_edit_rating"
    echo "  08_vinyl_edit_genre"
    echo "  09_vinyl_edit_condition"
    echo "  10_vinyl_card_detail"
    echo "  11_delete_confirmation"
    echo "  12_cover_preview"
    exit 1
fi

NAME=$1
TEMP_PATH="/sdcard/temp_screenshot.png"
OUTPUT_DIR="screenshots"
OUTPUT_FILE="${OUTPUT_DIR}/${NAME}.png"

# Créer le dossier screenshots s'il n'existe pas
mkdir -p "$OUTPUT_DIR"

# Vérifier qu'un appareil est connecté
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICES" -eq 0 ]; then
    echo -e "${RED}❌ Aucun appareil Android détecté${NC}"
    echo -e "${YELLOW}💡 Vérifiez que l'émulateur est lancé ou qu'un appareil est connecté${NC}"
    exit 1
fi

echo -e "${YELLOW}📸 Prise du screenshot...${NC}"

# Prendre le screenshot
adb shell screencap -p "$TEMP_PATH"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erreur lors de la capture d'écran${NC}"
    exit 1
fi

echo -e "${YELLOW}⬇️  Téléchargement du screenshot...${NC}"

# Récupérer le fichier
adb pull "$TEMP_PATH" "$OUTPUT_FILE" 2>/dev/null

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Erreur lors du téléchargement${NC}"
    exit 1
fi

# Nettoyer le fichier temporaire
adb shell rm "$TEMP_PATH" 2>/dev/null

# Vérifier que le fichier existe
if [ -f "$OUTPUT_FILE" ]; then
    FILE_SIZE=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    echo -e "${GREEN}✅ Screenshot sauvegardé : ${OUTPUT_FILE} (${FILE_SIZE})${NC}"

    # Afficher un récapitulatif
    EXISTING=$(ls -1 screenshots/*.png 2>/dev/null | wc -l | tr -d ' ')
    echo -e "${GREEN}📊 Screenshots pris : ${EXISTING}/12${NC}"
else
    echo -e "${RED}❌ Erreur : le fichier n'a pas été créé${NC}"
    exit 1
fi

