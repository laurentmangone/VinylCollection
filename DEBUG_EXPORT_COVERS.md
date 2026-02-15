# 🔍 Guide de debugging Export/Import

## 🎯 Objectif
Identifier pourquoi les covers ne sont pas exportées dans le JSON.

---

## 📋 Étapes de debugging

### 1️⃣ Installe la nouvelle version avec logs

1. Compile le debug APK : `./gradlew assembleDebug`
2. Installe l'APK sur ton téléphone
3. Ouvre **Android Studio** → **Logcat**

### 2️⃣ Filtre les logs dans Logcat

Dans Logcat, crée des filtres :
- **Tag: `VinylExport`** pour voir l'export
- **Tag: `VinylImport`** pour voir l'import

### 3️⃣ Test d'export

1. Dans l'app, ouvre un vinyl en édition
2. **Vérifie que la cover est bien affichée** dans l'écran d'édition
3. Va dans **Menu → Sauvegarde → Exporter en JSON**
4. Partage le fichier (email, Drive, etc.)

### 4️⃣ Analyse les logs d'export

Dans Logcat, cherche des lignes comme :

#### ✅ Cas normal (vinyl SANS cover)
```
D/VinylExport: Starting export of 2 vinyls
D/VinylExport: [0] 24 Nights - coverUri: NONE
D/VinylExport: [1] The Dark Side Of The Moon - coverUri: NONE
D/VinylExport: Processing: 24 Nights
D/VinylExport:   - coverUri: 'null'
D/VinylExport:   - coverUri isEmpty: null
D/VinylExport:   - coverUri isBlank: null
```

#### ✅ Cas attendu (vinyl AVEC cover)
```
D/VinylExport: Starting export of 2 vinyls
D/VinylExport: [0] 24 Nights - coverUri: /data/user/0/com.example.vinylcollection/files/covers/cover_1771098765432.jpg
D/VinylExport: [1] The Dark Side Of The Moon - coverUri: /data/user/0/.../covers/cover_1771098876543.jpg
D/VinylExport: Processing: 24 Nights
D/VinylExport:   - coverUri: '/data/user/0/.../covers/cover_1771098765432.jpg'
D/VinylExport:   - coverUri isEmpty: false
D/VinylExport:   - coverUri isBlank: false
D/VinylExport: Cover encoded for 24 Nights: 45678 bytes -> 61038 chars
```

#### ❌ Cas problématique (fichier non trouvé)
```
D/VinylExport: Processing: 24 Nights
D/VinylExport:   - coverUri: '/data/user/0/.../covers/cover_123.jpg'
D/VinylExport:   - coverUri isEmpty: false
D/VinylExport:   - coverUri isBlank: false
W/VinylExport: Cover file not found for 24 Nights: /data/user/0/.../covers/cover_123.jpg
```

---

## 🔎 Diagnostic

### Scénario A : coverUri est null ou vide
**Symptôme** : Dans les logs, tu vois `coverUri: NONE` ou `coverUri: 'null'`

**Cause** : Les vinyls n'ont pas de covers enregistrées dans la base de données

**Solution** :
1. Ouvre un vinyl en édition
2. Ajoute une cover (caméra ou galerie)
3. Sauvegarde
4. Refais l'export

**Vérification** : Le JSON devrait maintenant contenir `"coverBase64": null` (grâce à `.serializeNulls()`)

---

### Scénario B : coverUri existe mais fichier introuvable
**Symptôme** : Dans les logs, tu vois `Cover file not found`

**Cause** : Le fichier cover a été supprimé ou déplacé

**Solution** :
1. Réédite le vinyl
2. Remplace la cover
3. Sauvegarde

---

### Scénario C : coverUri existe et fichier trouvé
**Symptôme** : Dans les logs, tu vois `Cover encoded for ... : X bytes -> Y chars`

**Résultat attendu** : Le JSON devrait contenir `"coverBase64": "iVBORw0KGgoAAAA..."`

**Vérification** :
1. Ouvre le fichier JSON exporté
2. Cherche le champ `coverBase64`
3. Il devrait contenir une longue chaîne Base64

---

## 🧪 Test complet

### Test 1 : Vinyl sans cover
1. Crée un vinyl sans cover
2. Exporte
3. **Résultat attendu** : `"coverBase64": null` dans le JSON

### Test 2 : Vinyl avec cover
1. Crée un vinyl
2. Ajoute une cover (caméra ou galerie)
3. Sauvegarde
4. Exporte
5. **Résultat attendu** : `"coverBase64": "iVBORw0KGg..."` dans le JSON

### Test 3 : Import
1. Importe le JSON du Test 2
2. **Résultat attendu** : La cover s'affiche dans la liste et l'édition

---

## 📊 Checklist de vérification

### Avant export
- [ ] Les vinyls ont des covers visibles dans la liste
- [ ] Les vinyls ont des covers visibles dans l'édition
- [ ] Les fichiers covers existent dans `/data/.../covers/`

### Pendant export
- [ ] Logcat affiche `VinylExport: Starting export of X vinyls`
- [ ] Logcat affiche `VinylExport: [N] Title - coverUri: /path/to/cover.jpg`
- [ ] Logcat affiche `VinylExport: Cover encoded for Title: X bytes -> Y chars`

### Après export
- [ ] Le JSON contient `"coverBase64": "..."` pour les vinyls avec covers
- [ ] Le JSON contient `"coverBase64": null` pour les vinyls sans covers
- [ ] Le JSON contient `"rating": null` (preuve que `.serializeNulls()` fonctionne)

### Pendant import
- [ ] Logcat affiche `VinylImport: Cover saved: /path/to/imported_XXX.jpg (Y bytes)`

### Après import
- [ ] Les covers s'affichent dans la liste
- [ ] Les covers s'affichent dans l'édition

---

## 🐛 Problème actuel diagnostiqué

D'après ton JSON exporté :
```json
{
  "id": 21,
  "title": "24 Nights",
  "artist": "Eric Clapton"
}
```

**Diagnostic** :
1. ❌ Pas de champ `rating` (même pas `null`) → Moshi omettait les valeurs `null`
2. ❌ Pas de champ `coverBase64` → Les vinyls n'ont probablement pas de covers OU Moshi omettait les `null`

**Correction apportée** :
- ✅ Ajout de `.serializeNulls()` pour forcer l'inclusion des champs `null`
- ✅ Ajout de logs détaillés pour diagnostiquer les covers

**Prochaine étape** :
1. Installe la nouvelle version
2. Ouvre Logcat
3. Exporte à nouveau
4. Vérifie les logs pour confirmer si les vinyls ont des covers

---

## 📝 Questions à répondre

Après avoir testé avec la nouvelle version :

1. **Est-ce que les vinyls affichent des covers dans l'app ?**
   - Oui → Les covers sont en base de données
   - Non → Il faut ajouter des covers manuellement

2. **Que dit Logcat lors de l'export ?**
   - `coverUri: NONE` → Pas de covers en base
   - `coverUri: /path/...` → Covers présentes, vérifier si fichiers existent
   - `Cover encoded` → Tout est OK !

3. **Le nouveau JSON contient-il `"rating": null` ?**
   - Oui → `.serializeNulls()` fonctionne
   - Non → Problème de compilation

4. **Le nouveau JSON contient-il `"coverBase64": null` ou `"coverBase64": "..."`?**
   - `null` → Pas de covers en base
   - String Base64 → Covers exportées avec succès ! 🎉

---

**Date** : 14 février 2026  
**Version** : Debug avec logs + serializeNulls()  
**Prochaine étape** : Tester et analyser les logs
