# Options pour ajouter une icône au README

> **Note** : Ce fichier contient des exemples HTML adaptés pour GitHub Markdown.
> Les exemples utilisent `style="text-align: center;"` pour éviter l'attribut obsolète `align`.

## Problème résolu ✅
L'avertissement `Cannot resolve file 'ic_launcher.png'` a été corrigé en retirant l'image du header.

## Si vous souhaitez ajouter une icône au README

### Option 1 : Utiliser l'emoji 💿 (Actuel - Recommandé)
```markdown
# 💿 Vinyl Collection
```
- ✅ Simple et fonctionne partout
- ✅ Pas besoin d'hébergement d'image
- ✅ Thème de l'application bien représenté

### Option 2 : Exporter et héberger l'icône
1. **Exporter l'icône** :
   - Aller dans `app/src/main/res/mipmap-xxxhdpi/`
   - Copier `ic_launcher.webp`
   - Convertir en PNG si nécessaire :
     ```bash
     # Avec ImageMagick
     convert app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp ic_launcher.png
     ```

2. **Héberger l'image** :
   - Créer un issue GitHub temporaire
   - Glisser-déposer `ic_launcher.png`
   - Copier l'URL générée
   - Mettre à jour le README :
     ```markdown
     <div style="text-align: center;">
       <img src="URL_GITHUB" alt="Vinyl Collection Icon" width="120"/>
     </div>
     ```

### Option 3 : Utiliser un badge/shield
```markdown
<div style="text-align: center;">
  <img src="https://img.shields.io/badge/Android-Vinyl%20Collection-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Vinyl Collection"/>
</div>
```

### Option 4 : Créer un logo SVG personnalisé
Créer un fichier `assets/logo.svg` dans le repo et le référencer :
```markdown
<div style="text-align: center;">
  <img src="assets/logo.svg" alt="Vinyl Collection" width="120"/>
</div>
```

## Recommandation

Pour un README professionnel sur GitHub, **garder juste l'emoji 💿** dans le titre est :
- ✅ Plus simple
- ✅ Toujours fonctionnel
- ✅ Pas de dépendance externe
- ✅ Rapide à charger

Si vous voulez vraiment une icône visuelle, utilisez **Option 2** (héberger sur GitHub Issues).
