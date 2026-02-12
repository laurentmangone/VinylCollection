# 🎵 Intégration Discogs - Vue d'ensemble

## Qu'est-ce que Discogs ?

**Discogs** est la plus grande **base de données mondiale de musique et de vinyles**, avec plus de 24 millions de releases cataloguées et curées par une communauté de passionnés.

🌐 **Site web** : https://www.discogs.com

### Chiffres clés
- **24+ millions** de releases (vinyles, CDs, cassettes, digitaux)
- **Couverture mondiale** de tous les genres musicaux
- **Communauté active** : Vendeurs, collectionneurs, DJs
- **Données enrichies** : Artistes, labels, années, genres, styles, formats
- **Images haute résolution** : Pochettes numérisées
- **Prix du marché** : Historique des ventes

---

## 🎯 Fonctionnalité : Recherche Automatique Discogs

### Flux utilisateur

```
1. Utilisateur remplit "Artiste" et "Titre"
   ↓
2. Clique sur "🔍 Chercher sur Discogs"
   ↓
3. Recherche Discogs via l'API REST
   ↓
4. Affichage de 10 meilleurs résultats
   (avec pochette de chaque album affichée via Coil)
   ↓
5. Sélectionne le bon match
   ↓
6. AUTO-REMPLISSAGE automatique :
   - Titre (exact)
   - Année
   - Label
   - Genre
   - Image de couverture (téléchargée et stockée localement)
   ↓
7. Utilisateur vérifie/corrige
   ↓
8. Enregistrement
```

### Exemple concret

**Avant** (sans Discogs) :
- Utilisateur tape : "Pink Floyd" + "Dark Side of the Moon"
- Trouve manuellement : Année, Label, Genre (5-10 minutes)

**Après** (avec Discogs) :
- Utilisateur tape : "Pink Floyd" + "Dark Side"
- Clique "Chercher Discogs"
- Sélectionne le bon résultat
- TOUS les champs complétés automatiquement (30 secondes)

---

## 🔧 Implémentation Technique

### Architecture

```
┌─────────────────────────────────┐
│   VinylEditBottomSheet.kt       │  ← Formulaire édition
│   (Bouton "Chercher Discogs")   │
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│  DiscogsSearchBottomSheet.kt    │  ← Affichage résultats
│  (Bottom Sheet avec RecyclerView)│
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│  DiscogsManager.kt             │  ← API REST Retrofit
│  (Appels Discogs API)          │
└────────────────┬────────────────┘
                 │
┌────────────────▼────────────────┐
│  https://api.discogs.com        │  ← API Discogs
└────────────────────────────────┘
```

### Fichiers créés

1. **`DiscogsManager.kt`** (230 lignes)
   - Gestion de l'API Discogs avec Retrofit
   - Recherche par titre/artiste
   - Téléchargement des images
   - Modèles de données (data classes)

2. **`DiscogsSearchBottomSheet.kt`** (110 lignes)
   - Bottom Sheet pour afficher les résultats
   - RecyclerView avec adaptateu
   - Callback pour sélection du résultat

3. **`DiscogsResultAdapter.kt`** (90 lignes)
   - Adaptateur pour afficher chaque résultat
   - Affichage: Titre, Année, Genre, Label, Format
   - **Chargement des images de pochette via Coil**
   - Utilise `cover_image` (haute résolution) ou `thumb` en fallback

4. **Layouts XML**
   - `item_discogs_release.xml` - Carte pour chaque résultat
   - `bottom_sheet_discogs_search.xml` - Bottom Sheet

5. **Modification `bottom_sheet_vinyl.xml`**
   - Ajout du bouton "🔍 Chercher sur Discogs"

### Dépendances

```kotlin
// Retrofit + Gson pour l'API REST
implementation("com.squareup.retrofit2:retrofit:2.10.0")
implementation("com.squareup.retrofit2:converter-gson:2.10.0")
implementation("com.google.code.gson:gson:2.10.1")

// Coil pour charger les images de pochettes
implementation("io.coil-kt:coil:2.5.0")
```

### Classe DiscogsManager

**API REST utilisée** :
```
GET https://api.discogs.com/database/search?q=artist+title&type=release&per_page=10
```

**Réponse JSON** (exemple) :
```json
{
  "results": [
    {
      "id": 123456,
      "title": "Pink Floyd - Dark Side of the Moon",
      "year": 1973,
      "genre": ["Rock"],
      "style": ["Progressive Rock"],
      "label": ["Harvest"],
      "format": ["Vinyl", "LP"],
      "cover_image": "https://..."
    }
  ]
}
```

---

## 🚀 Utilisation

### Pour l'utilisateur

1. **Remplir Artiste + Titre** dans le formulaire
2. **Cliquer** sur le bouton "🔍 Chercher sur Discogs"
3. **Sélectionner** le bon album de la liste
4. **Les champs se remplissent automatiquement**

### Pour le développeur

**Ajouter une recherche personnalisée** :
```kotlin
val discogsManager = DiscogsManager(context)
val results = discogsManager.searchRelease("Pink Floyd Dark Side")
// results = List<DiscogsRelease>
```

**Télécharger une image** :
```kotlin
val imageFile = discogsManager.downloadCoverImage(
    "https://api.discogs.com/image/...",
    context
)
```

---

## 🖼️ Gestion des images de pochettes

### 1️⃣ Affichage dans les résultats de recherche

Les images de pochettes s'affichent **automatiquement** dans la liste des résultats Discogs grâce à **Coil** :

- **Format utilisé** : `cover_image` (haute résolution) ou `thumb` (miniature) en fallback
- **Chargement asynchrone** : Les images sont chargées en arrière-plan sans bloquer l'UI
- **Cache automatique** : Coil met en cache les images pour un chargement rapide
- **Placeholder** : Une icône de vinyle s'affiche pendant le chargement
- **Gestion d'erreur** : Si l'image ne peut pas être chargée, l'icône de vinyle reste affichée

**Code dans `DiscogsResultAdapter.kt`** :
```kotlin
val coverUrl = release.cover_image ?: release.thumb
releaseCover.load(coverUrl) {
    crossfade(true)
    placeholder(R.drawable.ic_vinyl)
    error(R.drawable.ic_vinyl)
}
```

### 2️⃣ Téléchargement et stockage local

Lorsque l'utilisateur sélectionne un résultat, l'image est **téléchargée et stockée localement** :

- **Priorité** : `cover_image` (meilleure qualité) puis `thumb` en fallback
- **Stockage** : Fichier local dans `context.filesDir/covers/discogs_timestamp.jpg`
- **FileProvider** : URI sécurisée pour afficher l'image dans l'app
- **Timeout** : 10 secondes pour le téléchargement
- **User-Agent** : Header personnalisé pour identifier l'application

**Flux de téléchargement** :
```
1. Sélection d'un résultat Discogs
   ↓
2. Extraction de l'URL (cover_image ou thumb)
   ↓
3. Téléchargement via HttpURLConnection
   ↓
4. Sauvegarde dans /covers/discogs_*.jpg
   ↓
5. Création d'une URI via FileProvider
   ↓
6. Affichage dans le formulaire d'édition
```

### 3️⃣ Avantages de cette approche

✅ **UX améliorée** : L'utilisateur voit immédiatement les pochettes dans les résultats  
✅ **Performance** : Cache automatique de Coil pour un affichage rapide  
✅ **Offline** : Une fois téléchargée, l'image reste disponible hors ligne  
✅ **Qualité** : Utilisation de `cover_image` pour la meilleure résolution  
✅ **Robustesse** : Fallback sur `thumb` si `cover_image` n'est pas disponible

---

## 💡 Cas d'usage avancés (Futurs)

### 1️⃣ Recherche par code-barre
```kotlin
val release = discogsManager.searchByBarcode("198028762311")
// Parfait pour scanner le code-barre du vinyle
```

### 2️⃣ Détails complets du release
```kotlin
val details = discogsManager.getReleaseDetail(123456)
// Acces à: tracklist, matériel, notes spéciales, etc.
```

### 3️⃣ Sync avec wishlist Discogs
```
- Connecter via OAuth à Discogs
- Importer la wishlist Discogs
- Synchroniser bidirectionnelle
```

### 4️⃣ Prix du marché
```kotlin
// Ajouter le prix moyen Discogs
val price = discogsManager.getMarketPrice(releaseId)
```

---

## ⚙️ Configuration requise

### API Discogs
- ✅ **API Publique** : Pas d'authentification requise
- ✅ **Gratuite** : Aucun coût
- ✅ **Rate limiting** : 60 requêtes/minute (largement suffisant)
- ✅ **HTTPS** : Sécurisé

### Permissions Android
- ✅ `INTERNET` (pour les appels API)
- ✅ Aucune permission caméra/galerie requise

---

## 📊 Statistiques potentielles

Une fois les données Discogs intégrées, on peut afficher :

```
📊 STATISTIQUES DE VOTRE COLLECTION

Année moyenne : 1987
Genre dominant : Rock (45%)
Top 3 labels : Harvest, Virgin, Geffen
Vinyls avant 1980 : 12
Plus recherché : Dark Side of the Moon (VE: $450)
```

---

## 🔗 Ressources

- **API Discogs** : https://www.discogs.com/developers/
- **Documentation** : https://www.discogs.com/developers/resource?resource=database-search
- **Base de données** : https://www.discogs.com/search

---

## ✅ Tests recommandés

### Recherches à tester
- [ ] "Pink Floyd Dark Side" → Doit trouver le classique
- [ ] "The Beatles Help" → Multiple versions (1965, Remaster, etc.)
- [ ] "Unknown Artist Fake Album" → Affiche "Aucun résultat"
- [ ] Recherche vide → Toast d'erreur

### Performance
- [ ] Recherche prend < 2 secondes
- [ ] Scroll dans 100+ résultats est fluide
- [ ] Téléchargement image < 1 seconde

### Edge cases
- [ ] Caractères spéciaux : "Björk" ✓
- [ ] Accents : "Hébergement français" ✓
- [ ] Connexion perdue → Message d'erreur ✓

---

## 🎓 Prochaines étapes proposées

**Court terme** (2-3 semaines)
- [ ] Tester l'intégration Discogs complète
- [ ] Améliorer le parsing des résultats
- [ ] Cache local des recherches

**Moyen terme** (1-2 mois)
- [ ] Scanner code-barre
- [ ] Sync avec wishlist Discogs
- [ ] Afficher prix du marché

**Long terme** (3+ mois)
- [ ] Machine Learning pour suggestions
- [ ] Communauté : partage de collections
- [ ] Export/Import Discogs native

---

**Créé le** : 2026-02-12  
**Dernière mise à jour** : 2026-02-12

