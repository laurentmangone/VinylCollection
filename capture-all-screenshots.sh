#!/bin/bash

# Script interactif pour prendre tous les screenshots
# Ce script guide l'utilisateur à travers chaque screenshot

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║     📸 Guide de capture de screenshots - Vinyl App    ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Liste des screenshots avec instructions
declare -A SCREENSHOTS
SCREENSHOTS["01_vinyl_list"]="Liste principale avec plusieurs vinyles (4-6)"
SCREENSHOTS["02_vinyl_list_search"]="Recherche active avec texte 'rock' et résultats"
SCREENSHOTS["03_vinyl_list_empty"]="Liste vide (supprimer tous les vinyles)"
SCREENSHOTS["04_vinyl_edit_create"]="Bottom sheet vide pour créer un vinyle"
SCREENSHOTS["05_vinyl_edit_fields"]="Formulaire complet avec tous les champs remplis"
SCREENSHOTS["06_vinyl_edit_cover"]="Section pochette avec les 4 icônes visibles"
SCREENSHOTS["07_vinyl_edit_rating"]="RatingBar avec 4 étoiles sélectionnées"
SCREENSHOTS["08_vinyl_edit_genre"]="Liste déroulante de genres ouverte"
SCREENSHOTS["09_vinyl_edit_condition"]="Liste déroulante d'état ouverte"
SCREENSHOTS["10_vinyl_card_detail"]="Zoom sur une belle carte vinyle dans la liste"
SCREENSHOTS["11_delete_confirmation"]="Dialog de confirmation de suppression affiché"
SCREENSHOTS["12_cover_preview"]="Dialog de prévisualisation plein écran d'une pochette"

# Ordre des screenshots
ORDER=("01_vinyl_list" "02_vinyl_list_search" "03_vinyl_list_empty"
       "04_vinyl_edit_create" "05_vinyl_edit_fields" "06_vinyl_edit_cover"
       "07_vinyl_edit_rating" "08_vinyl_edit_genre" "09_vinyl_edit_condition"
       "10_vinyl_card_detail" "11_delete_confirmation" "12_cover_preview")

echo -e "${YELLOW}📱 Assurez-vous que l'application est lancée sur l'émulateur/appareil${NC}"
echo -e "${YELLOW}⏸️  Vous serez guidé pour chaque screenshot${NC}"
echo ""
read -p "Appuyez sur Entrée pour commencer..."

COUNT=0
TOTAL=${#ORDER[@]}

for SCREENSHOT in "${ORDER[@]}"; do
    COUNT=$((COUNT + 1))
    INSTRUCTION="${SCREENSHOTS[$SCREENSHOT]}"

    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}📸 Screenshot ${COUNT}/${TOTAL}: ${SCREENSHOT}.png${NC}"
    echo -e "${YELLOW}📋 ${INSTRUCTION}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    # Vérifier si le screenshot existe déjà
    if [ -f "screenshots/${SCREENSHOT}.png" ]; then
        echo -e "${GREEN}✅ Ce screenshot existe déjà${NC}"
        read -p "Voulez-vous le remplacer? (o/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Oo]$ ]]; then
            echo -e "${YELLOW}⏭️  Screenshot ignoré${NC}"
            continue
        fi
    fi

    echo -e "${YELLOW}Préparez l'écran selon les instructions ci-dessus${NC}"
    read -p "Appuyez sur Entrée quand vous êtes prêt..."

    # Prendre le screenshot
    ./take-screenshot.sh "$SCREENSHOT"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Screenshot ${COUNT}/${TOTAL} réussi !${NC}"
    else
        echo -e "${YELLOW}⚠️  Erreur, mais on continue...${NC}"
    fi

    # Pause sauf pour le dernier
    if [ $COUNT -lt $TOTAL ]; then
        echo -e "${CYAN}Préparez le prochain screenshot...${NC}"
        sleep 1
    fi
done

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           ✅ Capture terminée !                        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Récapitulatif
CAPTURED=$(ls -1 screenshots/*.png 2>/dev/null | wc -l | tr -d ' ')
echo -e "${CYAN}📊 Récapitulatif:${NC}"
echo -e "   Screenshots capturés: ${GREEN}${CAPTURED}/12${NC}"
echo ""

if [ "$CAPTURED" -eq 12 ]; then
    echo -e "${GREEN}🎉 Tous les screenshots sont pris !${NC}"
    echo -e "${CYAN}📝 Prochaines étapes:${NC}"
    echo -e "   1. Vérifiez la qualité des images dans screenshots/"
    echo -e "   2. Le README.md affichera automatiquement les images"
    echo -e "   3. Pour publier sur GitHub, voir SCREENSHOTS_GUIDE.md"
else
    echo -e "${YELLOW}⚠️  Il manque ${RED}$((12 - CAPTURED))${YELLOW} screenshot(s)${NC}"
    echo -e "${CYAN}Liste des screenshots manquants:${NC}"
    for SCREENSHOT in "${ORDER[@]}"; do
        if [ ! -f "screenshots/${SCREENSHOT}.png" ]; then
            echo -e "   ${RED}✗${NC} ${SCREENSHOT}.png - ${SCREENSHOTS[$SCREENSHOT]}"
        fi
    done
fi

echo ""

