# 🖼️ Covers Discogs - Guide Complet

## Vue d'ensemble

L'application **Vinyl Collection** affiche automatiquement les **pochettes d'albums** depuis l'API Discogs lors de la recherche, et les télécharge pour un stockage local permanent.

---

## 🎯 Fonctionnalités

### 1️⃣ Affichage dans les résultats de recherche

Lorsque vous recherchez un album sur Discogs, **chaque résultat affiche sa pochette** :

✅ **Chargement instantané** via Coil (bibliothèque d'images Android)  
✅ **Cache automatique** pour un affichage rapide  
✅ **Placeholder** : Icône vinyle pendant le chargement  
✅ **Gestion d'erreur** : Icône vinyle si l'image ne charge pas

### 2️⃣ Téléchargement et stockage

Lorsque vous sélectionnez un résultat, l'image est **téléchargée et stockée localement** :

✅ **Haute qualité** : Utilise `cover_image` (résolution maximale)  
✅ **Fallback** : Si `cover_image` n'existe pas, utilise `thumb`  
✅ **Stockage permanent** : Fichier local dans `/covers/`  
✅ **Disponible hors ligne** : Plus besoin de connexion après téléchargement

---

## 🔧 Implémentation technique

### Architecture

```
┌──────────────────────────────────────────┐
│  DiscogsSearchBottomSheet                │
│  (Liste des résultats)                   │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  DiscogsResultAdapter                    │
│  (Affiche chaque résultat)               │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  Coil (Bibliothèque d'images)            │
│  - Chargement asynchrone                 │
│  - Cache mémoire + disque                │
│  - Gestion erreurs                       │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│  API Discogs                             │
│  https://i.discogs.com/...               │
└──────────────────────────────────────────┘
```

### Code - Affichage dans les résultats

**Fichier : `DiscogsResultAdapter.kt`**

```kotlin
// Priorité : cover_image (haute résolution) > thumb (miniature)
val coverUrl = release.cover_image ?: release.thumb

if (!coverUrl.isNullOrBlank()) {
    releaseCover.load(coverUrl) {
        crossfade(true)                        // Animation de fondu
        placeholder(R.drawable.ic_vinyl)       // Icône pendant chargement
        error(R.drawable.ic_vinyl)             // Icône si erreur
        listener(
            onSuccess = { _, _ ->
                Log.d("DiscogsAdapter", "Image chargée")
            },
            onError = { _, result ->
                Log.e("DiscogsAdapter", "Erreur: ${result.throwable}")
            }
        )
    }
} else {
    releaseCover.setImageResource(R.drawable.ic_vinyl)
}
```

### Code - Téléchargement pour stockage local

**Fichier : `VinylEditBottomSheet.kt`**

```kotlin
// Sélection de l'URL (cover_image prioritaire)
val coverImageUrl = release.cover_image?.takeIf { it.isNotBlank() }
    ?: release.thumb?.takeIf { it.isNotBlank() }

if (!coverImageUrl.isNullOrBlank()) {
    lifecycleScope.launch {
        val imageFile = discogsManager.downloadCoverImage(
            coverImageUrl,
            requireContext()
        )
        if (imageFile != null) {
            // Créer une URI sécurisée via FileProvider
            coverUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )
            updateCoverUi()
        }
    }
}
```

**Fichier : `DiscogsManager.kt`**

```kotlin
suspend fun downloadCoverImage(
    imageUrl: String,
    context: Context
): File? = withContext(Dispatchers.IO) {
    try {
        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        
        val file = File(coversDir, "discogs_${System.currentTimeMillis()}.jpg")
        
        val url = URL(imageUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "VinylCollection/1.0")
        
        connection.getInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        file
    } catch (e: Exception) {
        Log.e("Discogs", "Erreur téléchargement: ${e.message}")
        null
    }
}
```

---

## 📊 Formats d'images Discogs

L'API Discogs fournit plusieurs formats d'images :

| Champ | Résolution | Utilisation |
|-------|-----------|-------------|
| `cover_image` | **Haute résolution** (variable) | Affichage principal, téléchargement |
| `thumb` | **150x150 px** (miniature) | Fallback si `cover_image` absent |
| `resource_url` | API endpoint | Pour récupérer les images complètes |

**Exemple de réponse API** :
```json
{
  "id": 123456,
  "title": "Pink Floyd - Dark Side of the Moon",
  "thumb": "https://i.discogs.com/thumb/150/image.jpg",
  "cover_image": "https://i.discogs.com/image.jpg",
  "year": 1973
}
```

---

## 🚀 Avantages de cette approche

### Performance
- **Chargement asynchrone** : Ne bloque jamais l'UI
- **Cache automatique** : Coil met en cache les images (mémoire + disque)
- **Optimisation réseau** : Téléchargement unique par image

### Expérience utilisateur
- **Visuel immédiat** : Les pochettes s'affichent pendant la recherche
- **Sélection facilitée** : Plus facile de reconnaître le bon album
- **Disponibilité offline** : Une fois téléchargée, l'image reste accessible

### Robustesse
- **Gestion d'erreur** : Placeholder si l'image ne charge pas
- **Timeout** : 10 secondes max pour éviter de bloquer
- **Fallback** : `thumb` si `cover_image` indisponible

---

## 🔍 Débogage

### Logs disponibles

**Affichage dans les résultats** :
```
D/DiscogsAdapter: Chargement image: https://i.discogs.com/...
D/DiscogsAdapter: Image chargée avec succès
```

**Téléchargement** :
```
D/Discogs: Début téléchargement image: https://...
D/Discogs: Image téléchargée: 245678 octets
D/Discogs: Image sauvegardée: /data/user/0/.../covers/discogs_1234.jpg
```

### Problèmes courants

| Problème | Solution |
|----------|----------|
| Image ne s'affiche pas | Vérifier permission `INTERNET` dans AndroidManifest |
| Erreur "Permission denied" | Vérifier que `VinylApplication` est déclarée dans le manifest |
| Image floue | API Discogs retourne parfois des miniatures, pas de solution |
| Téléchargement lent | Timeout à 10s, peut être augmenté dans `DiscogsManager.kt` |

---

## 📚 Ressources

- **Coil** : https://coil-kt.github.io/coil/
- **API Discogs** : https://www.discogs.com/developers
- **FileProvider** : https://developer.android.com/reference/androidx/core/content/FileProvider

---

## 🔮 Améliorations futures possibles

- [ ] **Pré-chargement** : Charger les 3 premières images en priorité
- [ ] **Compression** : Réduire la taille des images avant stockage
- [ ] **Gestion du cache** : Nettoyer les anciennes images Discogs inutilisées
- [ ] **Mode offline** : Indiquer si l'image vient du cache ou du réseau
- [ ] **Retry automatique** : Retenter le chargement si erreur réseau temporaire

---

**✅ Implémentation complète et fonctionnelle !**

