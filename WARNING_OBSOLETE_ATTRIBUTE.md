# ⚠️ Avertissement "Obsolete attribute" dans README.md

## Contexte

L'IDE (Android Studio/IntelliJ) affiche un avertissement :
```
Warning:(16, 9) Obsolete attribute
```

## Explication

Cet avertissement concerne l'attribut `align="center"` sur les balises `<td>` dans les tableaux HTML du fichier Markdown.

### Pourquoi cet avertissement ?

L'IDE analyse le README.md comme du HTML pur, et en HTML5 strict, l'attribut `align` est effectivement obsolète au profit du CSS.

### Pourquoi c'est OK dans notre cas ?

1. **GitHub Markdown** : Le README.md est destiné à GitHub, qui supporte et **recommande** l'attribut `align` pour les tableaux Markdown
2. **Pas de CSS externe** : GitHub Markdown ne permet pas de styles CSS personnalisés
3. **Syntaxe valide** : C'est la méthode standard pour centrer le contenu des cellules dans les tableaux Markdown sur GitHub

## Solutions

### ✅ Option 1 : Ignorer l'avertissement (Recommandé)

L'avertissement est **sans conséquence** car :
- Le fichier est du Markdown, pas du HTML pur
- L'attribut `align` fonctionne parfaitement sur GitHub
- C'est la méthode recommandée pour les tableaux Markdown

**Action** : Aucune action requise ✅

### ❌ Option 2 : Utiliser du CSS inline (Ne fonctionne pas)

```html
<td style="text-align: center">
```

**Problème** : GitHub Markdown **supprime** les attributs `style` pour des raisons de sécurité.

### ❌ Option 3 : Retirer `align` (Casse la mise en page)

Sans `align="center"`, les screenshots ne seront plus centrés dans les cellules.

## Recommandation

**✅ GARDER `align="center"`** et ignorer l'avertissement de l'IDE.

C'est une fausse alerte due au fait que l'IDE traite le Markdown comme du HTML strict.

## Comment supprimer l'avertissement dans l'IDE ?

Si l'avertissement vous dérange visuellement :

### Dans IntelliJ/Android Studio :

1. **Supprimer pour ce fichier** :
   - Cliquer sur l'ampoule jaune 💡 à côté de l'avertissement
   - Choisir "Suppress for file"

2. **Désactiver globalement l'inspection** :
   - `File > Settings > Editor > Inspections`
   - Chercher "Obsolete HTML attribute"
   - Décocher ou définir comme "Warning" → "Weak Warning"

3. **Exclure README.md de l'inspection HTML** :
   - Clic droit sur README.md
   - `Mark as > Plain Text` (mais perd la coloration syntaxique)

## Conclusion

L'attribut `align="center"` sur les balises `<td>` est :
- ✅ **Correct** pour GitHub Markdown
- ✅ **Nécessaire** pour centrer les screenshots
- ✅ **Standard** dans les projets open-source
- ⚠️ **Obsolète** seulement en HTML5 strict (pas applicable ici)

**Verdict** : Ignorer cet avertissement en toute confiance ! 🎯

