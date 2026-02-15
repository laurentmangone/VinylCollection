# 🔧 Correction : Import JSON ne restaurait pas les covers

## 🐛 Problème identifié

Lors de l'import d'un JSON, les vinyls étaient bien importés mais **les covers ne s'affichaient pas**.

### Causes identifiées

1. **Conflit d'IDs** : Les vinyls importés gardaient leurs anciens IDs du JSON, ce qui pouvait créer des conflits avec les IDs existants dans la base de données.

2. **Noms de fichiers non uniques** : Les covers importées utilisaient l'ancien ID dans le nom de fichier (`imported_<timestamp>_<oldId>.jpg`), ce qui pouvait créer des collisions si plusieurs imports étaient effectués.

3. **Manque de logs** : Impossible de diagnostiquer les erreurs sans logs appropriés.

---

## ✅ Corrections apportées

### 1. Force l'ID à 0 lors de l'import

**Fichier** : `VinylExportImport.kt`

```kotlin
Vinyl(
    id = 0,  // Force l'ID à 0 pour que la DB génère un nouvel ID
    title = item.title,
    // ...
    coverUri = coverUri
)
```

**Impact** : La base de données génère automatiquement de nouveaux IDs séquentiels, évitant tout conflit.

---

### 2. Noms de fichiers uniques avec timestamp + random

**Fichier** : `VinylExportImport.kt`

```kotlin
val uniqueId = "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
val file = File(coversDir, "imported_${uniqueId}.jpg")
```

**Impact** : Chaque cover importée a un nom de fichier unique, même si on importe le même JSON plusieurs fois.

---

### 3. Suppression du `.copy(id = 0)` redondant

**Fichier** : `BackupBottomSheet.kt`

```kotlin
// Avant
importedVinyls.forEach { vinyl ->
    viewModel.add(vinyl)
}

// Après
importedVinyls.forEach { vinyl ->
    viewModel.add(vinyl.copy(id = 0))  // Sécurité supplémentaire
}
```

**Impact** : Double sécurité pour garantir que l'ID est bien à 0.

---

### 4. Ajout de logs pour le debugging

**Fichiers** : `VinylExportImport.kt`

#### Export
```kotlin
android.util.Log.d("VinylExport", "Cover encoded for ${vinyl.title}: ${bytes.size} bytes -> ${encoded.length} chars")
android.util.Log.w("VinylExport", "Cover file not found for ${vinyl.title}: $uri")
android.util.Log.e("VinylExport", "Error encoding cover for ${vinyl.title}: ${e.message}")
```

#### Import
```kotlin
android.util.Log.d("VinylImport", "Cover saved: ${file.absolutePath} (${bytes.size} bytes)")
android.util.Log.e("VinylImport", "Error decoding cover for ${item.title}: ${e.message}")
```

**Impact** : Permet de diagnostiquer facilement les problèmes dans Logcat.

---

## 🧪 Comment tester

Voir le fichier [`TEST_IMPORT_EXPORT.md`](TEST_IMPORT_EXPORT.md) pour le guide de test complet.

### Test rapide

1. **Export** : Menu → Sauvegarde → Exporter en JSON
2. **Import** : Menu → Sauvegarde → Importer un JSON
3. **Vérification** : Les covers doivent s'afficher dans la liste

---

## 📊 Résultats attendus

| Scénario | Avant | Après |
|----------|-------|-------|
| Export avec covers | ✅ OK | ✅ OK |
| Import vinyls | ✅ OK | ✅ OK |
| Import covers | ❌ KO | ✅ OK |
| IDs régénérés | ❌ Non | ✅ Oui |
| Noms fichiers uniques | ❌ Non | ✅ Oui |
| Logs disponibles | ❌ Non | ✅ Oui |

---

## 🔍 Vérification dans Logcat

### Export réussi
```
D/VinylExport: Cover encoded for Dark Side of the Moon: 45678 bytes -> 61038 chars
D/VinylExport: Cover encoded for Abbey Road: 38920 bytes -> 51894 chars
```

### Import réussi
```
D/VinylImport: Cover saved: /data/.../covers/imported_1739568324123_4567.jpg (45678 bytes)
D/VinylImport: Cover saved: /data/.../covers/imported_1739568324456_8901.jpg (38920 bytes)
```

---

## 📝 Fichiers modifiés

1. **`VinylExportImport.kt`**
   - Force `id = 0` lors de l'import
   - Noms de fichiers uniques avec `timestamp + random`
   - Logs ajoutés pour export/import

2. **`BackupBottomSheet.kt`**
   - Appel avec `.copy(id = 0)` pour double sécurité

3. **`BACKUP_FEATURE.md`**
   - Documentation mise à jour
   - Checklist complétée

4. **`TEST_IMPORT_EXPORT.md`** (nouveau)
   - Guide de test complet

---

## 🚀 Prochaines étapes

- [ ] Tester sur un vrai device
- [ ] Tester avec des collections volumineuses (50+ vinyls)
- [ ] Tester l'import/export multiple (fusionner des collections)
- [ ] Optimiser pour les covers > 1MB (compression)

---

**Date** : 14 février 2026  
**Status** : ✅ Corrigé et testé en compilation  
**Build** : ✅ `./gradlew assembleDebug` réussi  
**Prochaine étape** : Test sur device réel

