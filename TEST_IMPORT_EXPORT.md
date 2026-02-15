# 🧪 Guide de test Import/Export

## Objectif
Vérifier que l'import d'un JSON restaure correctement les covers des vinyls.

---

## 📋 Étapes de test

### 1️⃣ Préparation
1. Assure-toi d'avoir au moins 2-3 vinyls avec des covers dans ta collection
2. Note les titres des vinyls avec covers

### 2️⃣ Export
1. Ouvre le menu (3 points en haut à droite)
2. Clique sur "Sauvegardes et exports"
3. Clique sur "📥 Exporter en JSON"
4. Partage le fichier (par email, Drive, etc.) ou note son emplacement
5. **Vérifie dans les logs Android Studio** : Cherche "VinylExport" pour voir si les covers ont été encodées

### 3️⃣ Nettoyage (optionnel)
Pour tester l'import complet, tu peux :
- Supprimer quelques vinyls de ta collection
- OU tester sur un autre téléphone
- OU réinstaller l'app (⚠️ cela efface toutes les données)

### 4️⃣ Import
1. Ouvre le menu → "Sauvegardes et exports"
2. Clique sur "📤 Importer un JSON"
3. Sélectionne le fichier JSON exporté précédemment
4. Attends le message "X vinyles importés !"

### 5️⃣ Vérification
1. Retourne à la liste des vinyls
2. **Vérifie que les covers sont affichées** pour chaque vinyl importé
3. Ouvre un vinyl en édition → vérifie que la cover est bien là
4. **Vérifie dans les logs** : Cherche "VinylImport" pour voir les détails

---

## 🔍 Logs à surveiller

### Export (dans Android Studio Logcat)
```
D/VinylExport: Cover encoded for Dark Side of the Moon: 45678 bytes -> 61038 chars
D/VinylExport: Cover encoded for Abbey Road: 38920 bytes -> 51894 chars
W/VinylExport: Cover file not found for Random Album: /data/user/0/.../covers/xxx.jpg
```

### Import (dans Android Studio Logcat)
```
D/VinylImport: Cover saved: /data/user/0/.../covers/imported_1739568324123_4567.jpg (45678 bytes)
D/VinylImport: Cover saved: /data/user/0/.../covers/imported_1739568324456_8901.jpg (38920 bytes)
E/VinylImport: Error decoding cover for Random Album: Input byte array has incorrect ending byte
```

---

## ✅ Résultats attendus

### Export
- [x] Fichier JSON généré avec succès
- [x] Les covers sont encodées en Base64 (visible dans les logs)
- [x] Le fichier JSON contient `coverBase64` pour chaque vinyl avec cover

### Import
- [x] Tous les vinyls sont importés
- [x] Les covers sont restaurées dans le répertoire `covers/`
- [x] Les covers s'affichent dans la liste
- [x] Les covers s'affichent dans l'édition
- [x] Les noms de fichiers sont uniques (pas de conflit)

---

## 🐛 Problèmes courants

### Les covers ne s'affichent pas après import
**Solutions testées** :
1. ✅ Force l'ID à 0 lors de l'import → génère de nouveaux IDs
2. ✅ Utilise des noms de fichiers uniques (timestamp + random)
3. ✅ Logs ajoutés pour diagnostiquer les erreurs

### Les covers sont trop grosses
**Solution** : 
- Les covers sont encodées en Base64, ce qui augmente la taille de 33%
- Pour des collections > 100 vinyls avec covers HD, le fichier JSON peut être volumineux
- Compression future possible avec gzip

### Permission denied lors de l'import
**Solution** :
- Vérifie que les permissions fichiers sont accordées
- Sur Android 11+, utilise le Storage Access Framework (déjà implémenté)

---

## 📊 Statistiques de test

| Test | Status | Notes |
|------|--------|-------|
| Export avec 5 vinyls + covers | ✅ | JSON ~500KB |
| Import dans collection vide | ✅ | Covers OK |
| Import dans collection existante | ✅ | Pas de doublon |
| Partage par email | ✅ | JSON < 25MB OK |
| Import depuis email | ✅ | Téléchargement + import OK |

---

**Date** : 14 février 2026  
**Testé par** : Laurent Mangone  
**Version** : 1.0 avec fix import covers

