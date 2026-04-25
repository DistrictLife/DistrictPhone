# Context Interface — District Phone
> Brief designer · Refonte visuelle en style iOS

---

## Vue d'ensemble du projet

**District Phone** est un mod Minecraft (Forge 1.16.5) qui ajoute un téléphone portable fonctionnel dans le jeu.  
L'interface est rendue en temps réel dans le moteur Minecraft via des textures 2D (pas de HTML/CSS).  
Le designer doit produire des **maquettes .psd** et des **textures exportées** que le développeur intégrera directement dans le jeu.

---

## Contraintes techniques

| Paramètre | Valeur |
|---|---|
| Taille interne du téléphone | 180 × 320 px |
| Zone de contenu (sans bordures) | 160 × 280 px |
| Marges latérales | 10 px de chaque côté |
| Barre de titre (top) | 16 px de hauteur |
| Rendu | Java (Minecraft) — pas de CSS, pas d'ombres natives |
| Couleurs | ARGB hexadécimal (ex : `0xFF1C1C1E`) |
| Polices | Police bitmap Minecraft (non modifiable côté code pour l'instant) |
| Format textures | PNG avec transparence (RGBA) |

> **Important :** Les textures doivent être pixelisées et nettes, pas de flou gaussien réel — simuler les effets visuels iOS (transparence, séparations, fonds) avec des aplats et des dégradés simples.

---

## Style cible — iOS (Dark Mode)

S'inspirer du style **iOS 16/17 en dark mode** :

- **Fonds** : noir profond `#000000` ou gris très sombre `#1C1C1E`, `#2C2C2E`
- **Surfaces / cartes** : `#3A3A3C` avec légère transparence simulée
- **Texte primaire** : blanc `#FFFFFF`
- **Texte secondaire** : gris clair `#8E8E93`
- **Accent / bleu iOS** : `#0A84FF`
- **Vert (appel)** : `#30D158`
- **Rouge (danger/supprimer)** : `#FF453A`
- **Séparateurs** : `#38383A` (1 px)
- **Coins arrondis** : simulés via textures (le moteur ne fait pas de border-radius natif)

---

## Inventaire des écrans à retravailler

### 1. Écran de verrouillage (`ScreenDeverouillage`)
**Capture :** `doc/ScreenDeverouillage.png`

Contenu actuel :
- Heure en grand (12:17)
- Date en dessous ("Mercredi 3 Janvier — An 1")
- Texte "Appuyer pour déverrouiller" en bas

Refonte attendue (iOS Lock Screen) :
- Heure en très grand, fonte fine, centrée verticalement dans le tiers supérieur
- Date en dessous, taille réduite, couleur secondaire
- Séparation visuelle claire
- Fond : texture noire avec très légère vignette
- Texte de déverrouillage stylisé en bas (avec icône cadenas optionnelle)

---

### 2. Écran d'accueil (`ScreenAccueil`)
**Capture :** `doc/ScreenAccueil.png`

Contenu actuel :
- Barre de statut : heure + batterie
- Grille 3×2 d'icônes d'apps (Telephone, SMS, Contacts, Map, News, Paramètres)

Refonte attendue (iOS Home Screen) :
- **Barre de statut** redessinée : heure à gauche, icônes batterie + signal à droite, fond semi-transparent
- **Icônes** : chaque app doit avoir une icône iOS style (carré arrondi avec fond de couleur + glyphe blanc)  
  - Telephone → fond vert + icône combiné
  - SMS → fond vert + bulle de dialogue
  - Contacts → fond gris + silhouette personne
  - Map → fond bleu + épingle
  - News → fond rouge + journal
  - Paramètres → fond gris + engrenage
- **Labels** sous les icônes, fonte fine, couleur blanche

**Textures à fournir :** 6 icônes d'apps (60×60 px recommandé, export PNG)

---

### 3. App Téléphone — Clavier (`ScreenAppPhoneClavier`)
**Capture :** `doc/ScreenAppPhoneClavier.png`

Contenu actuel :
- Barre de navigation avec titre "Telephone" + flèche retour
- Deux onglets : Clavier / Journal
- Pavé numérique + bouton Appeler vert

Refonte attendue (iOS Phone app) :
- **Barre de navigation** : fond `#1C1C1E`, titre centré, bouton `←` à gauche
- **Onglets** style iOS segmented control (pill, fond `#3A3A3C`, sélectionné blanc)
- **Champ d'affichage du numéro** : grande taille, centré, fond transparent
- **Touches du pavé** : cercles/carrés arrondis fond `#3A3A3C`, texte blanc, chiffre grand + lettres petites en dessous (ABC, DEF…)
- **Bouton Appeler** : cercle vert `#30D158` centré, icône téléphone blanc
- **Touche effacement** : icône `⌫` sans fond, à droite du 0

---

### 4. App Téléphone — Journal (`ScreenAppPhoneJournal`)
**Capture :** `doc/ScreenAppPhoneJournal.png`

Contenu actuel :
- Liste d'appels avec numéro, type (Sortant/Entrant/Manqué), flèche de rappel

Refonte attendue :
- Chaque entrée : icône type d'appel colorée (vert sortant, rouge manqué, gris entrant) + numéro/nom + heure
- Flèche de rappel `>>` remplacée par icône téléphone bleu iOS à droite
- Séparateurs fins entre les entrées
- Manqués affichés en rouge

---

### 5. App SMS — Liste des conversations (`ScreenAppSms`)
**Capture :** `doc/ScreenAppSms.png`

Contenu actuel :
- Liste de conversations avec nom + aperçu du dernier message
- Bouton "Nouveau" en bas

Refonte attendue (iOS Messages) :
- Chaque ligne : avatar circulaire à gauche (initiale du contact sur fond coloré), nom en gras, aperçu en gris, heure à droite
- Point bleu pour les messages non lus
- Bouton "Nouveau" remplacé par icône stylo `✏` en haut à droite dans la barre de nav
- Séparateurs entre les conversations

---

### 6. App SMS — Chat (`ScreenAppSmsChating`)
**Capture :** `doc/ScreenAppSmsChating.png`

Contenu actuel :
- Bulles de messages (bleu pour envoyé, gris foncé pour reçu)
- Barre de saisie + bouton envoi `>`

Refonte attendue (iOS iMessage style) :
- **Bulles envoyées** : fond bleu `#0A84FF`, coins arrondis, alignées à droite
- **Bulles reçues** : fond `#3A3A3C`, alignées à gauche
- **Barre de saisie** : fond `#2C2C2E`, bordure arrondie, placeholder "Message…"
- **Bouton envoi** : flèche bleue dans cercle bleu (remplace le `>`)
- Nom du contact en titre de la barre de nav

---

### 7. App SMS — Nouveau message (`ScreenAppSmsNewMessage`)
**Capture :** `doc/ScreenAppSmsNewMessage.png`

Contenu actuel :
- Liste de contacts sélectionnables avec nom + numéro

Refonte attendue :
- Titre "Nouveau message" dans la barre de nav
- Chaque contact : avatar circulaire + nom + numéro, style liste iOS
- Contact sélectionnable = toute la ligne cliquable avec fond hover

---

### 8. App Contacts — Liste (`ScreenAppContacts`)
**Capture :** `doc/ScreenAppContacts.png`

Contenu actuel :
- Barre de recherche `?`
- Lignes avec pseudo + numéro + 3 boutons (✉ SMS bleu, ☎ vert, x rouge)

Refonte attendue (iOS Contacts) :
- **Barre de recherche** redessinée : champ arrondi fond `#3A3A3C`, icône loupe grise, placeholder "Rechercher"
- **Chaque contact** : avatar circulaire avec initiale colorée à gauche, nom en blanc, numéro en gris
- **Boutons d'action** (SMS ✉, Appel ☎, Supprimer ×) : icônes discrètes à droite, style iOS action buttons
- **Bouton Ajouter** : `+` dans la barre de nav en haut à droite (pas en bas)
- Ordre alphabétique avec index latéral optionnel (A B C…)

---

### 9. App Contacts — Ajout (`ScreenAppContactsAdd`)
**Capture :** `doc/ScreenAppContactsAdd.png`

Contenu actuel :
- Champs Nom + Numéro + bouton Confirmer + lien Annuler

Refonte attendue (iOS Add Contact) :
- Titre "Nouveau contact" dans la nav + bouton "Annuler" à gauche + "Enregistrer" à droite (bleu)
- Avatar placeholder circulaire en haut (caméra icon) + "Ajouter une photo"
- Champs dans une carte arrondie style iOS grouped table
- Labels au-dessus des champs en petite gris

---

### 10. App Actualités — Liste (`ScreenAppNews`)
**Capture :** `doc/ScreenAppNews.png`

Contenu actuel :
- Articles avec titre, auteur, date, extrait

Refonte attendue (style Apple News) :
- **Carte** pour chaque article : fond `#2C2C2E`, coins arrondis, padding interne
- Titre en gras blanc, auteur + date en gris, extrait en gris clair sur 2 lignes
- Séparation entre cartes
- Image de une optionnelle à droite (si disponible)

---

### 11. App Actualités — Détail (`ScreenAppNewsDetail`)
**Capture :** `doc/ScreenAppNewsDetail.png`

Contenu actuel :
- Titre + auteur/date + corps de l'article

Refonte attendue :
- Titre en grand gras, auteur + date en gris sous le titre
- Séparateur horizontal
- Corps de l'article avec interligne confortable

---

### 12. App Map (`ScreenAppMap`)
**Capture :** `doc/ScreenAppMap.png`

Contenu actuel :
- Carte Minecraft avec marqueur joueur (rouge), bouton centrer, zoom +/−, échelle

Refonte attendue :
- **Boutons** (centrer, +, −) : style iOS maps — cercles blancs sur fond `#1C1C1E` avec ombre simulée
- **Marqueur joueur** : point bleu pulsant (simulé) avec cercle de précision
- **Barre d'échelle** : style iOS maps, fine, blanche

---

### 13. App Paramètres (`ScreenAppParametres`)
**Capture :** `doc/ScreenAppParametres.png`

Contenu actuel :
- Numéro de téléphone affiché
- Liste des apps avec statut visible/masqué (point vert ou "MASQUÉ")

Refonte attendue (iOS Settings) :
- **Section "Numéro"** : card arrondie fond `#2C2C2E`, numéro centré en grand
- **Section "Applications"** : grouped list style iOS
  - Chaque app : icône colorée à gauche + nom + toggle switch à droite (vert = visible, gris = masqué)
  - Toggle switch style iOS natif (à dessiner en texture)

---

## Livrables attendus

### Fichiers .psd
Un `.psd` par écran, organisé en calques nommés :
```
ScreenDeverouillage.psd
ScreenAccueil.psd
ScreenAppPhone.psd          (Clavier + Journal sur 2 artboards)
ScreenAppSms.psd            (Liste + Chat + Nouveau sur 3 artboards)
ScreenAppContacts.psd       (Liste + Ajout sur 2 artboards)
ScreenAppNews.psd           (Liste + Détail sur 2 artboards)
ScreenAppMap.psd
ScreenAppParametres.psd
```

**Dimensions de l'artboard :** libre — le designer choisit la taille qui lui convient, le développeur adapte à l'intégration  
**Résolution :** libre, mode couleur RVB

### Textures PNG à exporter
Le designer est **libre de choisir la taille de chaque texture** selon ce qu'il juge le mieux pour le rendu.  
Le développeur se chargera de les intégrer et les redimensionner dans le moteur.

| Texture | Usage |
|---|---|
| `icon_telephone.png` | Icône app Téléphone |
| `icon_sms.png` | Icône app SMS |
| `icon_contacts.png` | Icône app Contacts |
| `icon_map.png` | Icône app Map |
| `icon_news.png` | Icône app News |
| `icon_settings.png` | Icône app Paramètres |
| `bg_phone.png` | Fond du téléphone (coque) |
| `bg_statusbar.png` | Barre de statut |
| `bg_navbar.png` | Barre de navigation (titre) |
| `btn_call.png` | Bouton appeler |
| `btn_sms.png` | Bouton SMS |
| `btn_delete.png` | Bouton supprimer |
| `toggle_on.png` | Toggle switch activé |
| `toggle_off.png` | Toggle switch désactivé |
| `avatar_placeholder.png` | Avatar contact par défaut |

---

## Palette de couleurs de référence

```
Fond principal       #000000  /  ARGB: 0xFF000000
Surface 1            #1C1C1E  /  ARGB: 0xFF1C1C1E
Surface 2            #2C2C2E  /  ARGB: 0xFF2C2C2E
Surface 3            #3A3A3C  /  ARGB: 0xFF3A3A3C
Séparateur           #38383A  /  ARGB: 0xFF38383A
Texte principal      #FFFFFF  /  ARGB: 0xFFFFFFFF
Texte secondaire     #8E8E93  /  ARGB: 0xFF8E8E93
Accent bleu          #0A84FF  /  ARGB: 0xFF0A84FF
Vert (appel/ok)      #30D158  /  ARGB: 0xFF30D158
Rouge (danger)       #FF453A  /  ARGB: 0xFFFF453A
Orange (warning)     #FF9F0A  /  ARGB: 0xFFFF9F0A
```

---

## Notes importantes pour le designer

1. **Pas de transparence réelle possible** — simuler le "frosted glass" iOS avec un aplat semi-opaque `#1C1C1E` à environ 90% opacité
2. **Coins arrondis** — les dessiner directement dans la texture PNG, pas via CSS
3. **Taille libre** — travailler à la résolution souhaitée, le développeur se charge du redimensionnement à l'intégration
4. **Livrer en PNG sans compression** avec fond transparent là où c'est possible
5. **Nommer les calques** clairement dans le .psd pour faciliter les exports futurs
6. **Les captures de référence** sont dans `doc/Screen*.png` — s'y référer pour comprendre ce que chaque écran fait fonctionnellement avant de le redessiner
