# 💾 Export/Import JSON

## ✨ Feature implémentée

L'application supporte maintenant :
- **Export JSON** : Exporter toute ta collection en JSON avec les covers encodées en Base64
- **Import JSON** : Importer une collection depuis un fichier JSON

---

## 🎯 Fonctionnalités

### 1️⃣ Export JSON

**Accès** : Menu (icône sauvegarde) → "Sauvegardes et exports" → "Exporter en JSON"

**Ce qu'il fait** :
- Exporte tous les vinyls en JSON formaté
- Encode les covers en Base64 (images incluses dans le JSON)
- Génère un fichier avec timestamp
- Permet le partage par email/messaging

**Format JSON** :
```
{
  "version": 1,
  "exportDate": "1707763200000",
  "totalVinyls": 5,
  "vinyls": [
    {
      "id": 1,
      "title": "Dark Side of the Moon",
      "artist": "Pink Floyd",
      "year": 1973,
      "genre": "Rock",
      "label": "Harvest",
      "rating": 5,
      "condition": "Bon",
      "notes": "En excellent état",
      "coverBase64": "iVBORw0KGgoAAAANSUhEUgAA..."
    }
  ]
}
```

### 2️⃣ Import JSON

**Accès** : Menu → "Sauvegardes et exports" → "Importer un JSON"

**Ce qu'il fait** :
- Sélectionne un fichier JSON depuis le téléphone
- Décode les covers depuis Base64
- Importe tous les vinyls dans la BD locale
- Les covers sont restaurées automatiquement

**Cas d'usage** :
- Changer de téléphone
- Restaurer une ancienne collection
- Fusionner des collections
- Partager sa collection avec des amis

---

## 📋 Fichiers modifiés/créés

### Nouvelles classes
- `VinylExportImport.kt` - Export/Import JSON avec covers
- `BackupBottomSheet.kt` - UI pour les backups

### Fichiers modifiés
- `app/build.gradle.kts` - Dépendances Moshi + Google Play Services
- `VinylListFragment.kt` - Menu backup
- `AndroidManifest.xml` - Permissions fichiers
- `menu/menu_main.xml` - Action backup
- `strings.xml` - Strings pour backup

---

## 🔧 Dépendances ajoutées

```gradle

// JSON Serialization
implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
implementation("com.squareup.moshi:moshi-adapters:1.15.0")
ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
```

---

## 🚀 Utilisation

### Scénario 1 : Backup régulier

```
1. Chaque semaine, clique Menu → Sauvegarde → "Synchroniser Google Drive"
2. C'est tout ! La collection est sauvegardée
```

### Scénario 2 : Nouveau téléphone

```
1. Export depuis ancien téléphone → Partage par email/Drive
2. Sur nouveau téléphone :
   - Télécharge le JSON
   - Menu → "Importer un JSON"
   - Sélectionne le fichier
   - Tous les vinyls sont restaurés avec les covers !
```

### Scénario 3 : Fusion de collections

```
1. Exporte collection 1 en JSON
2. Exporte collection 2 en JSON
3. Importe les deux JSON successivement
   (Les vinyls en doublon seront remplacés par les plus récents)
```

---

## 💡 Points importants

### Sécurité
- ✅ Les données restent sur le téléphone
- ✅ Google Drive : authentification sécurisée avec OAuth
- ✅ Les covers sont encodées en Base64 (pas de liens externes)

### Limites actuelles
- La sync Google Drive est manuelle (pas d'auto-sync en arrière-plan)
- Les covers > 1MB peuvent ralentir l'export JSON
- En prod, implémenter l'API REST Google Drive v3 pour meilleure perf

### Améliorations futures
- [ ] Auto-sync quotidienne
- [ ] Fusion intelligente de collections
- [ ] Versionning des sauvegardes
- [ ] Export CSV/Excel
- [ ] Support iCloud Drive (iOS)

---

## 📊 Architecture

```
VinylExportImport
├── exportToJson() → Vinyl + Cover Base64 → JSON
├── exportToPrettyJson() → JSON formaté
└── importFromJson() → JSON → Vinyl + Cover décod

GoogleDriveManager
├── authenticate() → OAuth
├── uploadBackup() → Sauvegarde locale + Drive
└── getLastSyncTime() → Affiche sync status

BackupBottomSheet
├── Export button
├── Import button  
└── Google Drive button
```

---

## 🔐 Permissions requises

- `READ_EXTERNAL_STORAGE` - Lire les fichiers JSON
- `WRITE_EXTERNAL_STORAGE` - Écrire les fichiers JSON
- `INTERNET` - Connexion Google Drive
- Google Accounts - OAuth

---

## ✅ Testing checklist

- [x] Export → JSON généré correctement
- [x] Export → Covers encodées en Base64
- [x] Import → Vinyls restaurés
- [x] Import → Covers restaurées
- [x] Partage email → JSON envoyable
- [x] Import depuis email → OK

**Bugs corrigés** :
- ✅ Les covers sont maintenant correctement restaurées lors de l'import
- ✅ Les IDs sont régénérés automatiquement pour éviter les conflits
- ✅ Les noms de fichiers des covers sont uniques (timestamp + random)

---

**Date** : 14 février 2026  
**Status** : ✅ Implémenté et testé - Import/Export JSON uniquement
**Changement** : Fonctionnalité Google Drive retirée  
**Prochaine étape** : Tests utilisateur en condition réelle

