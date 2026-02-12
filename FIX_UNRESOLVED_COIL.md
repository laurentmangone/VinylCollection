# 🔧 Fix "Unresolved reference 'coil'" dans Android Studio

## ❌ Problème

L'IDE Android Studio affiche des erreurs `Unresolved reference 'coil'` et `Unresolved reference 'okhttp3'` dans VinylApplication.kt, mais le build Gradle réussit.

```
Error:(5, 8) Unresolved reference 'coil'.
Error:(6, 8) Unresolved reference 'coil'.
Error:(7, 8) Unresolved reference 'okhttp3'.
```

---

## ✅ Cause

Les dépendances Coil et OkHttp sont bien présentes dans `build.gradle.kts`, mais **l'IDE n'a pas synchronisé le projet** après l'ajout des dépendances.

---

## 🛠️ Solution : Synchroniser le projet dans Android Studio

### Méthode 1 : Sync Gradle (Recommandé)

1. Dans Android Studio, cliquez sur **File > Sync Project with Gradle Files**
2. Ou cliquez sur l'icône 🐘 **Sync** dans la barre d'outils en haut
3. Attendez que la synchronisation se termine (~30 secondes)
4. Les erreurs "Unresolved reference" devraient disparaître

### Méthode 2 : Invalidate Caches (Si Méthode 1 ne fonctionne pas)

1. **File > Invalidate Caches / Restart...**
2. Sélectionnez **Invalidate and Restart**
3. Android Studio va redémarrer et réindexer le projet
4. Attendez que l'indexation se termine

### Méthode 3 : Rebuild Project

1. **Build > Clean Project**
2. Attendez que le nettoyage se termine
3. **Build > Rebuild Project**
4. Attendez que le rebuild se termine

### Méthode 4 : Via Terminal (déjà fait)

```bash
cd /Users/laurentmangone/Github/VinylCollection
./gradlew clean build --refresh-dependencies
```

✅ **BUILD SUCCESSFUL** - Les dépendances sont bien présentes et le code compile.

---

## ✅ Vérification

Après la synchronisation, vérifiez que :

1. ✅ Les imports `coil.ImageLoader` et `coil.ImageLoaderFactory` ne sont plus en rouge
2. ✅ L'import `okhttp3.OkHttpClient` n'est plus en rouge
3. ✅ Pas d'erreurs dans la classe `VinylApplication`

---

## 📋 Dépendances présentes dans build.gradle.kts

```kotlin
// Image loading
implementation("io.coil-kt:coil:2.5.0")

// OkHttp
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

Ces dépendances sont **bien présentes** et le build réussit.

---

## 🔍 Si le problème persiste

### 1. Vérifiez la connexion Internet
Les dépendances sont téléchargées depuis Maven Central. Assurez-vous d'avoir une connexion Internet active.

### 2. Vérifiez le cache Gradle
```bash
rm -rf ~/.gradle/caches/
./gradlew --refresh-dependencies
```

### 3. Vérifiez que le module app est chargé
Dans Android Studio, vérifiez que le module `app` est bien dans la liste des modules :
- **File > Project Structure > Modules**
- Le module `app` devrait être listé

---

## 🎉 Résultat attendu

Après la synchronisation, le fichier VinylApplication.kt devrait compiler sans erreur :

**Imports résolus** :
```
import coil.ImageLoader              // ✅ Plus d'erreur
import coil.ImageLoaderFactory       // ✅ Plus d'erreur
import okhttp3.OkHttpClient          // ✅ Plus d'erreur
```

**Classe fonctionnelle** :
```kotlin
class VinylApplication : Application(), ImageLoaderFactory {
    // ...existing code...
}
```

---

## 📝 Note importante

**Le build Gradle réussit déjà** (`BUILD SUCCESSFUL`), donc le code est correct et l'application fonctionne. C'est uniquement un problème d'affichage dans l'IDE qui ne voit pas les dépendances.

**Action à faire** : Synchroniser le projet dans Android Studio (File > Sync Project with Gradle Files) 🔄

---

**Date** : 12 février 2026  
**Status** : ✅ Build réussi, synchronisation IDE requise

