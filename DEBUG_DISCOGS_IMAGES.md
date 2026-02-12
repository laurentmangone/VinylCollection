# 🔍 Guide de débogage des images Discogs dans Android Studio

## 📋 Étapes pour vérifier que les images s'affichent

### 1. Ouvrir Logcat

Dans Android Studio :
1. Cliquez sur `View` → `Tool Windows` → `Logcat`
2. Ou utilisez le raccourci : `⌘ 6` (Mac) ou `Alt + 6` (Windows/Linux)

### 2. Configurer le filtre Logcat

Dans la barre de recherche de Logcat, entrez :
```
DiscogsAdapter | Discogs | Coil
```

Ou créez un filtre personnalisé :
- Cliquez sur le menu déroulant à côté de la barre de recherche
- Sélectionnez `Edit Filter Configuration`
- Nom : `Discogs Images`
- Log Tag : `DiscogsAdapter|Discogs|Coil` (cochez "Regex")
- Cliquez sur `OK`

### 3. Lancer l'application

1. Lancer l'application sur l'émulateur : `▶️ Run`
2. Créer ou éditer un vinyle
3. Remplir les champs :
   - **Artiste** : `Pink Floyd`
   - **Titre** : `Dark Side`
4. Cliquer sur le bouton `🔍 Chercher sur Discogs`

### 4. Observer les logs

#### ✅ **Si tout fonctionne correctement, vous devriez voir** :

```
D/Discogs: Recherche trouvée: 10 résultats
D/Discogs: [0] Pink Floyd - The Dark Side Of The Moon
D/Discogs:   - cover_image: https://i.discogs.com/abc123.jpg
D/Discogs:   - thumb: https://i.discogs.com/thumb/abc123.jpg
D/DiscogsAdapter: Chargement image pour 'Pink Floyd - The Dark Side Of The Moon': https://i.discogs.com/abc123.jpg
D/Coil: Image request: https://i.discogs.com/abc123.jpg
D/Coil: Response code: 200
D/DiscogsAdapter: ✅ Image chargée avec succès: https://i.discogs.com/abc123.jpg
```

**Résultat visuel** : Les pochettes d'albums s'affichent dans la liste des résultats.

---

#### ❌ **Erreur : Image non chargée**

```
E/DiscogsAdapter: ❌ Erreur chargement image pour 'Pink Floyd - The Dark Side Of The Moon'
E/DiscogsAdapter: URL: https://i.discogs.com/abc123.jpg
E/DiscogsAdapter: Erreur: Unable to create a fetcher that supports: https://i.discogs.com/abc123.jpg
```

**Causes possibles** :
- Émulateur sans connexion Internet
- URL d'image invalide
- Problème de certificat SSL

**Solution** :
1. Vérifiez la connexion Internet de l'émulateur
2. Ouvrez un navigateur dans l'émulateur et testez l'accès à `https://i.discogs.com`
3. Redémarrez l'émulateur

---

#### ❌ **Erreur : HTTP 403 (Accès refusé)**

```
D/Coil: Response code: 403
E/DiscogsAdapter: ❌ Erreur chargement image
```

**Causes possibles** :
- Discogs bloque les requêtes sans User-Agent valide
- Rate limiting de l'API

**Solution** :
- Le User-Agent est déjà configuré dans `VinylApplication.kt`
- Attendez quelques minutes avant de réessayer (rate limiting)

---

#### ⚠️ **Avertissement : Pas d'image disponible**

```
D/DiscogsAdapter: ⚠️ Pas d'URL d'image disponible pour: Pink Floyd - The Dark Side Of The Moon
```

**Cause** :
- Discogs n'a pas retourné d'URL d'image pour ce résultat

**Résultat visuel** :
- L'icône de vinyle par défaut s'affiche

---

#### ❌ **Erreur : Permission INTERNET refusée**

```
E/Discogs: Erreur recherche: canceled due to java.lang.SecurityException: Permission denied (missing INTERNET permission?)
```

**Cause** :
- La permission INTERNET n'est pas correctement déclarée dans le Manifest
- L'application n'a pas été recompilée après l'ajout de la permission

**Solution** :
```bash
./gradlew clean
./gradlew :app:installDebug
```

---

## 🧪 Test complet

### Scénario de test

1. **Ouvrir l'app** → Liste vide ou avec des vinyles
2. **Créer un vinyle** → Bouton `+` en bas à droite
3. **Remplir les champs** :
   - Artiste : `The Beatles`
   - Titre : `Abbey Road`
4. **Chercher sur Discogs** → Bouton `🔍 Chercher sur Discogs`
5. **Vérifier** :
   - ✅ Les pochettes s'affichent dans la liste
   - ✅ 10 résultats maximum
   - ✅ Les informations sont correctes (titre, année, genre, label)
6. **Sélectionner un résultat** → Cliquer sur une carte
7. **Vérifier** :
   - ✅ Les champs sont auto-remplis
   - ✅ La pochette est téléchargée et affichée

---

## 🔧 Dépannage avancé

### Nettoyer le cache de Coil

Si les images ne se chargent toujours pas, essayez de nettoyer le cache :

```kotlin
// Dans VinylApplication.kt (temporaire pour debug)
override fun onCreate() {
    super.onCreate()
    // Nettoyer le cache Coil au démarrage
    val imageLoader = ImageLoader.Builder(this).build()
    imageLoader.diskCache?.clear()
    imageLoader.memoryCache?.clear()
    
    Log.d("VinylApp", "Cache Coil nettoyé")
}
```

### Tester avec une URL d'image directe

Pour tester si Coil fonctionne, modifiez temporairement `DiscogsResultAdapter.kt` :

```kotlin
// Test avec une image publique
val testUrl = "https://picsum.photos/200"
releaseCover.load(testUrl) {
    crossfade(true)
    placeholder(R.drawable.ic_vinyl)
}
```

Si l'image de test s'affiche, alors le problème vient des URLs Discogs.

---

## 📊 Checklist de vérification

- [ ] Permission INTERNET dans AndroidManifest.xml
- [ ] usesCleartextTraffic="true" dans AndroidManifest.xml
- [ ] VinylApplication implémente ImageLoaderFactory
- [ ] Coil configuré avec OkHttpClient et User-Agent
- [ ] DiscogsResultAdapter utilise .load() avec les bons paramètres
- [ ] L'APK a été recompilé après les modifications
- [ ] L'émulateur a accès à Internet
- [ ] Les logs Logcat sont visibles et filtrés

---

## 🎯 Résultat attendu

Après avoir suivi ce guide, vous devriez voir :

```
┌─────────────────────────────────────┐
│  [Pochette]  Pink Floyd - The...    │
│              1973   Vinyl, LP       │
│              Rock                   │
│              Harvest                │
└─────────────────────────────────────┘
│  [Pochette]  Pink Floyd - The...    │
│              2016   Vinyl, LP       │
│              Rock                   │
│              Pink Floyd Records     │
└─────────────────────────────────────┘
```

Avec de **vraies pochettes d'albums** au lieu de l'icône de vinyle par défaut.

---

**Date** : 12 février 2026  
**Fichiers modifiés** :
- AndroidManifest.xml
- VinylApplication.kt
- DiscogsResultAdapter.kt
- item_discogs_release.xml

