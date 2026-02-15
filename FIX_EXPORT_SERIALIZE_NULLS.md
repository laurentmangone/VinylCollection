# 🔧 Correction #2 : Export n'incluait pas les covers (ni les champs null)

## 🐛 Nouveau problème identifié

En analysant le JSON exporté par l'app, on constate que :
1. ❌ Pas de champ `rating` (même pas `null`)
2. ❌ Pas de champ `coverBase64` (même si cover existe ou pas)

**Fichier exporté** :
```json
{
  "vinyls": [
    {
      "id": 21,
      "title": "24 Nights",
      "artist": "Eric Clapton",
      "year": 1991,
      "genre": "Rock",
      "label": "Reprise Records",
      "condition": "",
      "notes": ""
    }
  ]
}
```

---

## 🔍 Cause identifiée

**Moshi omet par défaut les champs avec valeur `null`**

Quand un vinyl n'a pas de cover (`coverUri = null`) ou pas de rating (`rating = null`), Moshi ne sérialise pas ces champs dans le JSON.

Conséquence :
- Impossible de distinguer entre "champ non supporté" et "champ vide"
- Lors de l'import, les champs omis sont traités comme `null` par défaut

---

## ✅ Corrections apportées

### 1. Force la sérialisation des valeurs null

**Fichier** : `VinylExportImport.kt`

```kotlin
private val exportAdapter = moshi.adapter(VinylCollectionExport::class.java)
    .serializeNulls()  // ✅ Force la sérialisation des valeurs null
```

**Impact** :
- Les champs `rating: null` apparaissent maintenant dans le JSON
- Les champs `coverBase64: null` apparaissent maintenant dans le JSON
- Le JSON est plus explicite et compatible avec d'autres parseurs

---

### 2. Ajout de logs détaillés pour diagnostiquer

**Fichier** : `VinylExportImport.kt`

#### Logs généraux
```kotlin
android.util.Log.d("VinylExport", "Starting export of ${vinyls.size} vinyls")

vinyls.forEachIndexed { index, vinyl ->
    android.util.Log.d("VinylExport", "[$index] ${vinyl.title} - coverUri: ${vinyl.coverUri ?: "NONE"}")
}
```

#### Logs détaillés par vinyl
```kotlin
android.util.Log.d("VinylExport", "Processing: ${vinyl.title}")
android.util.Log.d("VinylExport", "  - coverUri: '${vinyl.coverUri}'")
android.util.Log.d("VinylExport", "  - coverUri isEmpty: ${vinyl.coverUri?.isEmpty()}")
android.util.Log.d("VinylExport", "  - coverUri isBlank: ${vinyl.coverUri?.isBlank()}")
```

#### Logs d'encodage
```kotlin
// Déjà existants
android.util.Log.d("VinylExport", "Cover encoded for ${vinyl.title}: ${bytes.size} bytes -> ${encoded.length} chars")
android.util.Log.w("VinylExport", "Cover file not found for ${vinyl.title}: $uri")
android.util.Log.e("VinylExport", "Error encoding cover for ${vinyl.title}: ${e.message}")
```

**Impact** : Permet de diagnostiquer précisément si :
- Les vinyls ont des covers en base de données
- Les fichiers covers existent sur le disque
- Les covers sont correctement encodées

---

## 📊 Résultats attendus

### Export avec `.serializeNulls()`

**Avant** :
```json
{
  "id": 21,
  "title": "24 Nights",
  "artist": "Eric Clapton",
  "year": 1991,
  "genre": "Rock",
  "label": "Reprise Records",
  "condition": "",
  "notes": ""
}
```

**Après** :
```json
{
  "id": 21,
  "title": "24 Nights",
  "artist": "Eric Clapton",
  "year": 1991,
  "genre": "Rock",
  "label": "Reprise Records",
  "rating": null,
  "condition": "",
  "notes": "",
  "coverBase64": null
}
```

**Avec cover** :
```json
{
  "id": 21,
  "title": "24 Nights",
  "artist": "Eric Clapton",
  "year": 1991,
  "genre": "Rock",
  "label": "Reprise Records",
  "rating": 5,
  "condition": "Bon",
  "notes": "Excellent live",
  "coverBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAIBAQEBAQIBAQECAgICAgQDAgICAgUEBAMEBgUGBgY..."
}
```

---

## 🧪 Comment tester

### Étape 1 : Installe la nouvelle version
```bash
cd /Users/laurentmangone/Github/VinylCollection
./gradlew assembleDebug
# Ou pour release
./gradlew assembleRelease
```

APK disponible : `app/build/outputs/apk/debug/app-debug.apk`

### Étape 2 : Ouvre Logcat
Dans Android Studio :
1. **View → Tool Windows → Logcat**
2. Filtre par tag : `VinylExport`

### Étape 3 : Teste l'export
1. Dans l'app, va dans **Menu → Sauvegarde → Exporter**
2. Partage le JSON
3. **Vérifie Logcat** pour voir les logs

### Étape 4 : Analyse le JSON
Ouvre le fichier JSON et vérifie :
- ✅ `"rating": null` ou `"rating": 5` (le champ apparaît)
- ✅ `"coverBase64": null` ou `"coverBase64": "..."` (le champ apparaît)

---

## 🔍 Scénarios de diagnostic

### Scénario A : Vinyls sans covers

**Logs attendus** :
```
D/VinylExport: Starting export of 2 vinyls
D/VinylExport: [0] 24 Nights - coverUri: NONE
D/VinylExport: [1] The Dark Side Of The Moon - coverUri: NONE
```

**JSON attendu** :
```json
{
  "coverBase64": null
}
```

**Action** :
1. Ajoute des covers manuellement dans l'app
2. Refais l'export

---

### Scénario B : Vinyls avec covers

**Logs attendus** :
```
D/VinylExport: Starting export of 2 vinyls
D/VinylExport: [0] 24 Nights - coverUri: /data/.../covers/cover_123.jpg
D/VinylExport: [1] The Dark Side Of The Moon - coverUri: /data/.../covers/cover_456.jpg
D/VinylExport: Cover encoded for 24 Nights: 45678 bytes -> 61038 chars
D/VinylExport: Cover encoded for The Dark Side Of The Moon: 38920 bytes -> 51894 chars
```

**JSON attendu** :
```json
{
  "coverBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD..."
}
```

**Résultat** : ✅ Export réussi avec covers !

---

### Scénario C : Covers en base mais fichiers introuvables

**Logs attendus** :
```
D/VinylExport: [0] 24 Nights - coverUri: /data/.../covers/cover_123.jpg
W/VinylExport: Cover file not found for 24 Nights: /data/.../covers/cover_123.jpg
```

**JSON attendu** :
```json
{
  "coverBase64": null
}
```

**Cause** : Les fichiers ont été supprimés manuellement ou par un nettoyage système

**Solution** :
1. Réédite les vinyls
2. Remplace les covers
3. Refais l'export

---

## 📝 Fichiers modifiés

### `VinylExportImport.kt`
1. ✅ Ajout de `.serializeNulls()` sur `exportAdapter`
2. ✅ Ajout de logs au début de `exportToJson()`
3. ✅ Ajout de logs pour chaque vinyl traité
4. ✅ Logs existants conservés (encodage, erreurs)

---

## 🚀 Prochaines étapes

### Test immédiat
1. [ ] Installe la nouvelle APK
2. [ ] Ouvre Logcat
3. [ ] Exporte ta collection
4. [ ] Vérifie les logs
5. [ ] Ouvre le JSON et vérifie `rating` et `coverBase64`

### Si `coverBase64: null`
1. [ ] Ajoute des covers manuellement dans l'app
2. [ ] Refais l'export
3. [ ] Vérifie que le Base64 est présent

### Si `coverBase64: "..."`
1. [ ] Importe le JSON dans une collection vide
2. [ ] Vérifie que les covers s'affichent
3. [ ] ✅ Feature complète et fonctionnelle !

---

## 📦 Build status

- ✅ Compilation debug : OK
- ✅ Compilation release : OK
- ✅ APK disponible : `app/release/app-release.apk`

---

**Date** : 14 février 2026  
**Correction** : #2 - serializeNulls() + logs détaillés  
**Status** : ✅ Compilé et prêt à tester  
**Prochaine étape** : Test utilisateur avec Logcat
