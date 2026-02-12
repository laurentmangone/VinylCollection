# Screenshots de l'application

Ce dossier contient les captures d'écran de l'application Vinyl Collection.

## Comment ajouter des screenshots

1. **Prendre des screenshots sur l'émulateur/appareil Android**
   - Utilisez Android Studio Device Manager
   - Ou utilisez `adb shell screencap -p /sdcard/screenshot.png` puis `adb pull /sdcard/screenshot.png`

2. **Nommer les fichiers**
   - `01_vinyl_list.png` - Liste principale des vinyles
   - `02_vinyl_list_search.png` - Recherche dans la liste
   - `03_vinyl_list_empty.png` - État vide de la liste
   - `04_vinyl_edit_create.png` - Création d'un vinyle
   - `05_vinyl_edit_fields.png` - Formulaire d'édition complet
   - `06_vinyl_edit_cover.png` - Gestion de la pochette (icônes)
   - `07_vinyl_edit_rating.png` - Système de notation par étoiles
   - `08_vinyl_edit_genre.png` - Sélection du genre
   - `09_vinyl_edit_condition.png` - Sélection de l'état
   - `10_vinyl_card_detail.png` - Carte vinyle détaillée dans la liste
   - `11_delete_confirmation.png` - Dialog de confirmation de suppression
   - `12_cover_preview.png` - Prévisualisation de la pochette
   - `13_cover_crop.png` - Recadrage manuel de la pochette

3. **Dimensions recommandées**
   - Format portrait: 1080x2400 (ratio 9:21)
   - Format paysage: 2400x1080 (si nécessaire)
   - Ou résolution d'écran native de votre appareil de test

4. **Placer les fichiers**
   - Copier les fichiers PNG dans ce dossier `screenshots/`
   - Le README principal les référencera automatiquement

5. **Pousser dans le repository Git**
   - ✅ Les fichiers PNG de ce répertoire sont **autorisés à être commités** dans Git
   - Configuration : `.gitignore` est configuré pour accepter les `.png`, `.jpg` et `.jpeg` du répertoire `screenshots/`
   - Utiliser : `git add screenshots/*.png` pour ajouter tous les screenshots
   - Puis : `git commit -m "Add/update application screenshots"` et `git push`


