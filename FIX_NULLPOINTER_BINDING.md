# 🔧 Correction : NullPointerException dans les Bottom Sheets

**Date** : 23 février 2026  
**Problème** : Crash de l'application avec `NullPointerException` lors de l'accès au binding dans les coroutines

---

## 🐛 Symptômes

```
java.lang.NullPointerException
	at com.example.vinylcollection.DiscogsSearchBottomSheet.getBinding(DiscogsSearchBottomSheet.kt:23)
	at com.example.vinylcollection.DiscogsSearchBottomSheet.access$getBinding(DiscogsSearchBottomSheet.kt:20)
	at com.example.vinylcollection.DiscogsSearchBottomSheet$performSearch$1.invokeSuspend(DiscogsSearchBottomSheet.kt:123)
```

L'application plantait lors de recherches Discogs, exports/imports JSON, et téléchargements de covers.

---

## 🔍 Cause racine

Les `BottomSheetDialogFragment` utilisent un pattern standard de binding :

```kotlin
private var _binding: BottomSheetBinding? = null
private val binding get() = _binding!!
```

Le binding est mis à `null` dans `onDestroyView()`. Cependant, certaines coroutines lancées avec `lifecycleScope.launch` ou `viewLifecycleOwner.lifecycleScope.launch` peuvent continuer à s'exécuter après que `onDestroyView()` a été appelé.

### Scénario du crash

1. L'utilisateur ouvre un Bottom Sheet (ex: recherche Discogs)
2. Une coroutine est lancée pour une opération asynchrone (recherche réseau, téléchargement image)
3. L'utilisateur ferme rapidement le Bottom Sheet
4. `onDestroyView()` est appelé, mettant `_binding` à `null`
5. La coroutine se termine et essaie d'accéder au `binding`
6. **CRASH** : `NullPointerException` car `binding!!` force un non-null sur une valeur null

---

## ✅ Solution appliquée

### Principe
Au lieu d'utiliser directement `binding` dans les coroutines, on :
1. Stocke une référence locale au binding au début de l'opération
2. Vérifie que `_binding` n'est pas `null` avant chaque accès
3. Utilise `return@launch` pour sortir précocement si le binding est null
4. Utilise `viewLifecycleOwner.lifecycleScope` au lieu de `lifecycleScope` pour un meilleur cycle de vie

### Pattern appliqué

**Avant** (code dangereux) :
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    binding.progressBar.isVisible = true
    val results = discogsManager.searchRelease(query)
    binding.progressBar.isVisible = false
    // CRASH possible ici si le fragment a été détruit
}
```

**Après** (code sécurisé) :
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    // Vérifier que le binding existe avant de commencer
    val currentBinding = _binding ?: return@launch
    
    currentBinding.progressBar.isVisible = true
    val results = discogsManager.searchRelease(query)
    
    // Vérifier à nouveau après l'opération async
    val bindingAfterSearch = _binding ?: return@launch
    bindingAfterSearch.progressBar.isVisible = false
}
```

---

## 📝 Fichiers modifiés

### 1. `DiscogsSearchBottomSheet.kt`

#### Méthode `performSearch()`
- ✅ Vérification du binding avant la recherche
- ✅ Vérification du binding après la recherche réseau
- ✅ Vérification du binding dans le bloc catch

#### Méthode `updateSelectionUI()`
- ✅ Vérification du binding et retour précoce si null

### 2. `BackupBottomSheet.kt`

#### Méthode `exportToJson()`
- ✅ Vérification du binding avant l'export
- ✅ Vérification du binding après l'export JSON
- ✅ Vérification du binding dans le bloc catch

#### Méthode `importFromFile()`
- ✅ Vérification du binding avant l'import
- ✅ Vérification du binding après l'import JSON
- ✅ Vérification du binding dans le bloc catch

### 3. `VinylEditBottomSheet.kt`

#### Méthode `fillFromDiscogsRelease()`
- ✅ Changement de `lifecycleScope` vers `viewLifecycleOwner.lifecycleScope`
- ✅ Vérification du binding après le téléchargement de cover
- ✅ Vérification du binding dans le bloc catch

#### Méthode `updateCoverUi()`
- ✅ Vérification du binding au début de la méthode
- ✅ Retour précoce si binding null

---

## 🧪 Tests effectués

| Scénario | Avant | Après |
|----------|-------|-------|
| Recherche Discogs puis fermeture rapide | ❌ Crash | ✅ OK |
| Export JSON puis fermeture rapide | ❌ Crash | ✅ OK |
| Import JSON puis fermeture rapide | ❌ Crash | ✅ OK |
| Téléchargement cover puis fermeture | ❌ Crash | ✅ OK |
| Import multiple depuis Discogs | ✅ OK | ✅ OK |

---

## 📊 Impact

### Avant
- 🔴 Crash fréquent lors d'utilisation rapide de l'app
- 🔴 Expérience utilisateur frustrante
- 🔴 Perte de données potentielle

### Après
- ✅ Plus de crash lié au binding
- ✅ Gestion gracieuse des fermetures de dialogs
- ✅ Expérience utilisateur fluide

---

## 💡 Bonnes pratiques établies

### Pour tous les Bottom Sheets avec coroutines

1. **Toujours utiliser `viewLifecycleOwner.lifecycleScope`** au lieu de `lifecycleScope`
   ```kotlin
   viewLifecycleOwner.lifecycleScope.launch { ... }
   ```

2. **Capturer le binding au début de la coroutine**
   ```kotlin
   val currentBinding = _binding ?: return@launch
   ```

3. **Revérifier après chaque opération suspendante**
   ```kotlin
   val results = apiCall() // suspend function
   val bindingAfter = _binding ?: return@launch
   ```

4. **Sécuriser les méthodes qui accèdent au binding**
   ```kotlin
   private fun updateUi() {
       val currentBinding = _binding ?: return
       currentBinding.textView.text = "..."
   }
   ```

---

## 🔒 Garanties

Avec ces corrections :
- ✅ Aucun crash si l'utilisateur ferme le Bottom Sheet pendant une opération async
- ✅ Les opérations en cours se terminent proprement sans effet de bord
- ✅ Les ressources sont correctement libérées
- ✅ Le code est thread-safe vis-à-vis du cycle de vie du fragment

---

## 📚 Références

- [Android Lifecycle-aware coroutines](https://developer.android.com/topic/libraries/architecture/coroutines#lifecycle-aware)
- [View Binding in Fragments](https://developer.android.com/topic/libraries/view-binding#fragments)
- [Fragment lifecycle](https://developer.android.com/guide/fragments/lifecycle)

---

**Status** : ✅ Correction complète et testée  
**Build** : ✅ Compilation sans erreur  
**Prochain déploiement** : Inclure dans la prochaine release

