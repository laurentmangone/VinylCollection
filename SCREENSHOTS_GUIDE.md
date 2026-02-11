# 📸 Guide pour capturer des screenshots

Ce guide explique comment prendre des captures d'écran de l'application pour la documentation.

## Méthode 1 : Android Studio (Recommandée)

### 1. Lancer l'application
```bash
./gradlew :app:installDebug
```

### 2. Ouvrir l'appareil dans Android Studio
- Cliquer sur l'icône du Device Manager dans la barre latérale
- Ou `Tools > Device Manager`

### 3. Prendre des screenshots
- Lancer l'app sur l'émulateur
- Naviguer vers l'écran à capturer
- Cliquer sur l'icône caméra 📷 dans la barre d'outils de l'émulateur
- Ou utiliser le raccourci `Cmd + Shift + S` (macOS) / `Ctrl + Shift + S` (Windows/Linux)

### 4. Enregistrer les fichiers
- Les screenshots sont sauvegardés automatiquement
- Les renommer selon la convention : `01_vinyl_list.png`, `02_vinyl_list_search.png`, etc.
- Les déplacer vers le dossier `screenshots/`

---

## Méthode 2 : ADB (Command Line)

### Prendre un screenshot
```bash
# Capturer l'écran
adb shell screencap -p /sdcard/screenshot.png

# Récupérer le fichier
adb pull /sdcard/screenshot.png screenshots/01_vinyl_list.png

# Nettoyer
adb shell rm /sdcard/screenshot.png
```

### Script automatisé (macOS/Linux)
Créer un fichier `take-screenshot.sh` :
```bash
#!/bin/bash
NAME=$1
if [ -z "$NAME" ]; then
    echo "Usage: ./take-screenshot.sh <name>"
    echo "Example: ./take-screenshot.sh 01_vinyl_list"
    exit 1
fi

adb shell screencap -p /sdcard/temp.png
adb pull /sdcard/temp.png screenshots/${NAME}.png
adb shell rm /sdcard/temp.png
echo "Screenshot saved to screenshots/${NAME}.png"
```

Utilisation :
```bash
chmod +x take-screenshot.sh
./take-screenshot.sh 01_vinyl_list
```

---

## Méthode 3 : Appareil physique

### Android 4.0+
- **Raccourci** : `Volume Bas + Power` simultanément
- **Localisation** : Les screenshots sont dans `Pictures/Screenshots/`
- **Transfert** : Connecter l'appareil en USB et copier les fichiers

---

## 📋 Liste des screenshots à prendre

### Écrans principaux
- [ ] `01_vinyl_list.png` - Liste avec plusieurs vinyles
- [ ] `02_vinyl_list_search.png` - Recherche active avec résultats
- [ ] `03_vinyl_list_empty.png` - État vide (supprimer tous les vinyles temporairement)

### Formulaire de création/édition
- [ ] `04_vinyl_edit_create.png` - Bottom sheet vide pour création
- [ ] `05_vinyl_edit_fields.png` - Formulaire avec tous les champs remplis
- [ ] `06_vinyl_edit_cover.png` - Section pochette avec les 4 icônes visibles

### Fonctionnalités spécifiques
- [ ] `07_vinyl_edit_rating.png` - RatingBar avec 3-4 étoiles sélectionnées
- [ ] `08_vinyl_edit_genre.png` - Liste déroulante de genres ouverte
- [ ] `09_vinyl_edit_condition.png` - Liste déroulante d'état ouverte

### Cartes et dialogs
- [ ] `10_vinyl_card_detail.png` - Zoom sur une belle carte vinyle dans la liste
- [ ] `11_delete_confirmation.png` - Dialog de confirmation de suppression
- [ ] `12_cover_preview.png` - Dialog de prévisualisation plein écran d'une pochette
- [ ] `13_cover_crop.png` - Écran de recadrage manuel avec image positionnée

---

## 🎨 Conseils pour de beaux screenshots

### Configuration de l'émulateur
1. **Appareil recommandé** : Pixel 6 ou Pixel 7
   - Résolution moderne : 1080 x 2400 (9:21)
   - Bonne densité d'écran

2. **Créer un émulateur dédié**
   - `Tools > Device Manager > Create Device`
   - Choisir `Pixel 6` ou `Pixel 7`
   - API Level 34 (Android 14) ou 35 (Android 15)

### Données de test
Pour des screenshots attractifs, créer des vinyles réalistes :

```
Vinyle 1:
- Titre: Dark Side of the Moon
- Artiste: Pink Floyd
- Année: 1973
- Genre: Rock
- Label: Harvest Records
- Note: 5 étoiles
- État: Bon
- Notes: Album iconique, excellent état

Vinyle 2:
- Titre: Thriller
- Artiste: Michael Jackson
- Année: 1982
- Genre: Pop
- Label: Epic Records
- Note: 5 étoiles
- État: Bon

Vinyle 3:
- Titre: Abbey Road
- Artiste: The Beatles
- Année: 1969
- Genre: Rock
- Label: Apple Records
- Note: 4 étoiles
- État: Moyen

Vinyle 4:
- Titre: Kind of Blue
- Artiste: Miles Davis
- Année: 1959
- Genre: Jazz
- Label: Columbia Records
- Note: 5 étoiles
- État: Bon

Vinyle 5:
- Titre: Random Access Memories
- Artiste: Daft Punk
- Année: 2013
- Genre: Electronic
- Label: Columbia Records
- Note: 4 étoiles
- État: Bon
```

### Pochettes de test
- Télécharger des pochettes depuis Internet (pour usage personnel uniquement)
- Rechercher "[nom album] album cover high resolution"
- Ou utiliser des pochettes libres de droits

### Thème et apparence
1. **Mode clair** : Screenshots principaux en mode clair (meilleure lisibilité)
2. **Mode sombre** : Optionnel, prendre aussi quelques screenshots en mode sombre
3. **Langue** : Garder le français pour cohérence avec le README

### Cadrage
- Capturer tout l'écran (pas besoin de crop)
- S'assurer que le contenu est bien visible
- Éviter les écrans partiellement chargés

---

## 🔄 Workflow complet

1. **Préparer l'émulateur**
   ```bash
   # Lancer l'app
   ./gradlew :app:installDebug
   ```

2. **Ajouter des données de test**
   - Créer 5-6 vinyles avec des données réalistes
   - Ajouter des pochettes si possible

3. **Prendre les screenshots**
   - Suivre la liste de contrôle ci-dessus
   - Utiliser une des méthodes (Android Studio recommandée)

4. **Organiser les fichiers**
   ```bash
   # Vérifier que tous les screenshots sont présents
   ls -la screenshots/*.png
   
   # Devrait afficher :
   # 01_vinyl_list.png
   # 02_vinyl_list_search.png
   # ... etc
   ```

5. **Optimiser les images (optionnel)**
   ```bash
   # Installer ImageMagick si nécessaire
   brew install imagemagick
   
   # Optimiser la taille des PNG
   cd screenshots
   mogrify -resize 50% *.png
   ```

6. **Vérifier dans le README**
   - Ouvrir `README.md` dans GitHub ou un viewer Markdown
   - Vérifier que tous les screenshots s'affichent correctement

---

## 📦 Alternatives pour des screenshots professionnels

### Mockup tools en ligne
- **Previewed** : https://previewed.app/
- **MockuPhone** : https://mockuphone.com/
- **Smartmockups** : https://smartmockups.com/

Ces outils permettent d'insérer les screenshots dans des frames d'appareils Android réalistes.

### Framing depuis terminal (ImageMagick)
```bash
# Ajouter une ombre et un padding
convert input.png \
  -bordercolor white -border 20 \
  \( +clone -background black -shadow 80x3+5+5 \) \
  +swap -background white -layers merge +repage \
  output.png
```

---

## ✅ Checklist finale

- [ ] Tous les 12 screenshots sont pris
- [ ] Nommés correctement (01-12)
- [ ] Placés dans le dossier `screenshots/`
- [ ] Format PNG
- [ ] Résolution cohérente (1080px de largeur minimum)
- [ ] Le README affiche correctement les images
- [ ] Données de test réalistes et professionnelles
- [ ] Pas d'informations sensibles visibles

---

Bon courage ! 📸✨

