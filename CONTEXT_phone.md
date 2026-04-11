# CONTEXT.md — DistrictLife Phone Mod

> Fichier de contexte destiné à Claude Code CLI.
> Mod standalone pour le serveur **District Life** (Forge 1.16.5).
> Ce mod gère uniquement le téléphone RP : interface, apps, et logique associée.

---

## IDENTITÉ DU MOD

| Champ | Valeur |
|-------|--------|
| Nom du mod | DistrictLife Phone |
| Mod ID | `districtlife_phone` |
| Package racine | `com.districtlife.phone` |
| Version Minecraft | 1.16.5 |
| Mod Loader | Forge 36.2.34 |
| Java | 8 |
| Dépendances externes | Simple Voice Chat (Forge 2.6.15), Camera Mod by henkelmax |

---

## CONCEPT GÉNÉRAL

Le téléphone est un **item donné par un modérateur** (pas de craft). Le joueur l'ouvre via clic droit. Il affiche un GUI plein écran style **smartphone moderne (iOS/Android like)** entièrement custom.

Chaque joueur possède un **numéro RP unique** attribué à la réception du téléphone, utilisé pour tous les appels et SMS.

---

## FLOW D'UTILISATION

```
Clic droit sur l'item
        │
        ▼
┌─────────────────────┐
│   Écran de          │
│   verrouillage      │  ← Heure RP + date RP
│   [Déverrouiller]   │
└────────┬────────────┘
         │ clic
         ▼
┌─────────────────────┐
│   Écran d'accueil   │  ← Grille d'apps + barre de statut
└────────┬────────────┘
         │ clic sur une app
         ▼
┌─────────────────────┐
│   Application       │  ← Chaque app = écran dédié
└─────────────────────┘
```

---

## ARCHITECTURE DU MOD

```
districtlife-phone/
├── src/main/java/com/districtlife/phone/
│   ├── PhoneMod.java
│   ├── registry/
│   │   ├── ModItems.java
│   │   └── ModCapabilities.java
│   ├── item/
│   │   └── PhoneItem.java
│   ├── capability/
│   │   ├── IPhoneCapability.java
│   │   ├── PhoneCapability.java
│   │   └── PhoneCapabilityProvider.java
│   ├── network/
│   │   ├── PacketHandler.java
│   │   ├── PacketSyncPhone.java
│   │   ├── PacketSendSMS.java
│   │   ├── PacketCallRequest.java
│   │   ├── PacketCallResponse.java
│   │   └── PacketPublishNews.java
│   ├── screen/
│   │   ├── PhoneScreen.java                 # Conteneur principal, gère le stack de navigation
│   │   ├── AbstractPhoneApp.java            # Classe abstraite dont héritent toutes les apps
│   │   ├── screens/
│   │   │   ├── LockScreen.java
│   │   │   ├── HomeScreen.java
│   │   │   ├── AppPhone.java
│   │   │   ├── AppSMS.java
│   │   │   ├── AppContacts.java
│   │   │   ├── AppMap.java
│   │   │   ├── AppNews.java
│   │   │   ├── AppCamera.java
│   │   │   ├── AppGallery.java
│   │   │   └── AppSettings.java
│   │   └── widgets/
│   │       ├── StatusBar.java
│   │       ├── AppIcon.java
│   │       ├── NotificationDot.java
│   │       └── PhoneButton.java             # Bouton générique réutilisable dans les apps
│   └── util/
│       ├── RPTime.java
│       ├── PhoneNumberManager.java          # Attribution + stockage des numéros RP
│       └── PhoneRenderHelper.java
├── src/main/resources/
│   ├── META-INF/mods.toml
│   └── assets/districtlife_phone/
│       ├── lang/fr_fr.json
│       ├── textures/
│       │   ├── item/phone.png
│       │   └── gui/
│       │       ├── phone_frame.png
│       │       ├── wallpaper_default.png
│       │       ├── map_island.png
│       │       └── icons/
│       │           ├── app_phone.png
│       │           ├── app_sms.png
│       │           ├── app_contacts.png
│       │           ├── app_map.png
│       │           ├── app_news.png
│       │           ├── app_camera.png
│       │           ├── app_gallery.png
│       │           └── app_settings.png
│       └── models/item/phone.json
└── CONTEXT.md
```

---

## CAPABILITY JOUEUR (`PhoneCapability.java`)

Stockée côté serveur, synchronisée au client via `PacketSyncPhone`.

```java
String phoneNumber;               // Numéro RP unique (ex: "06-12-34-56-78")
List<Contact> contacts;           // UUID + pseudo RP + numéro RP
List<Conversation> conversations; // Historique SMS par interlocuteur
List<String> galleryImages;       // Noms de fichiers des photos (Camera Mod)
String ringSoundName;             // Nom de la sonnerie choisie ("Classique", "Bip", "Silence")
boolean hasUnreadSMS;             // Point rouge sur icône SMS
```

Persistance via **NBT** sur `PlayerLoggedInEvent` et `PlayerChangedDimensionEvent`.

---

## NUMÉRO RP (`PhoneNumberManager.java`)

- Format : `06-XX-XX-XX-XX` (aléatoire, unique sur le serveur)
- Attribué automatiquement à la commande `/phone give <joueur>`
- Stocké dans la capability du joueur
- Stocké aussi dans une map serveur `UUID → numéro` pour la résolution inverse
- Persistance via `WorldSavedData`

---

## COMMANDES STAFF

| Commande | Description |
|----------|-------------|
| `/phone give <joueur>` | Donne le téléphone + attribue un numéro RP |
| `/phone reset <joueur>` | Réinitialise la capability complète |
| `/phone number <joueur>` | Affiche le numéro RP d'un joueur (staff only) |
| `/news publish <titre> \| <contenu>` | Publie un article dans l'app News |

---

## PARTIES DU DÉVELOPPEMENT

---

### PARTIE 1 — Interface & navigation

**Objectif** : GUI fonctionnel, navigation entre les écrans, apps en coquilles vides.

#### Écran de verrouillage (`LockScreen.java`)
- Fond : `wallpaper_default.png`
- Centre haut : heure RP en grand format (`HH:mm`)
- Sous l'heure : date RP (`Lundi 14 Avril — An 1`)
- Bas : bouton `« Déverrouiller »`

**Calcul heure RP (`RPTime.java`) :**
- 1 jour Minecraft = 24 000 ticks = 24h RP
- Tick 0 = 6h00 du matin
- `heure = (worldTime % 24000 + 6000) % 24000 / 1000`
- Jour absolu = `worldTime / 24000`
- Noms des jours : Lundi → Dimanche en boucle depuis le jour 0
- Mois RP : 12 mois de 30 jours, 1 an = 360 jours Minecraft

#### Écran d'accueil (`HomeScreen.java`)
- Barre de statut (haut) : heure RP compacte à gauche, icônes signal + batterie (décoratives) à droite
- Grille d'apps 3 colonnes, ordre :
```
[ Téléphone ]  [    SMS    ]  [ Contacts ]
[    Map    ]  [   News    ]  [  Appareil ]
[  Galerie  ]  [ Paramètres]
```
- Chaque app = `AppIcon.java` : icône 32×32 + label dessous + `NotificationDot` si SMS non lu

#### Cadre smartphone (`PhoneScreen.java`)
- GUI centré, fond obscurci derrière
- Cadre via `phone_frame.png`, dimensions internes : **180×320px**
- Stack de navigation : `Deque<AbstractPhoneApp>` — bouton `←` = `pop()`, retour HomeScreen
- Fermeture : touche `Échap`

#### Apps — coquilles vides
Chaque app hérite de `AbstractPhoneApp` et affiche :
- Barre de titre : nom de l'app + bouton `←` retour
- Contenu : placeholder centré `« Bientôt disponible »`

---

### PARTIE 2 — App Contacts

**Objectif** : Gérer sa liste de contacts (ajouter, supprimer, rechercher).

#### Fonctionnalités
- Liste scrollable de tous les contacts du joueur
- Chaque contact affiche le **pseudo RP** + le **numéro RP**
- Bouton `+` en haut à droite : ouvre un formulaire d'ajout
  - Champ texte : saisir un numéro RP (`06-XX-XX-XX-XX`)
  - Le serveur résout le numéro → renvoie le pseudo RP associé
  - Si numéro inconnu : message d'erreur `« Numéro introuvable »`
- Appui long sur un contact : option `Supprimer`
- Barre de recherche en haut : filtre par pseudo ou numéro en temps réel

#### Packets
- `PacketAddContact` — client envoie un numéro, serveur répond avec le pseudo RP ou erreur
- `PacketRemoveContact` — client envoie l'UUID du contact à supprimer
- `PacketSyncPhone` — resync capability complète après modification

---

### PARTIE 3 — App SMS

**Objectif** : Envoyer et recevoir des messages texte entre joueurs via numéro RP.

#### Fonctionnalités
- Liste des conversations triées par date du dernier message
- Chaque conversation : pseudo RP + aperçu du dernier message + horodatage RP
- Point rouge si messages non lus
- Clic sur une conversation : ouvre le fil de messages
  - Bulles style SMS : messages envoyés à droite (couleur primaire), reçus à gauche (gris)
  - Horodatage RP sur chaque message
  - Champ de saisie en bas + bouton `Envoyer`
- Bouton `+` : nouvelle conversation (saisir un numéro RP)
- Portée : **serveur entier**, pas de limite de distance
- Longueur max d'un message : 160 caractères

#### Notification hors GUI
- Si le GUI est fermé à la réception d'un SMS : toast en haut de l'écran avec pseudo + début du message
- Son : `minecraft:entity.experience_orb.pickup`

#### Packets
- `PacketSendSMS` — client → serveur → destinataire : `{expediteurNumero, destinataireNumero, contenu, tickRP}`
- `PacketReceiveSMS` — serveur → client destinataire pour affichage temps réel
- `PacketMarkRead` — marque une conversation comme lue

---

### PARTIE 4 — App Téléphone (appels)

**Objectif** : Passer et recevoir des appels vocaux via Simple Voice Chat.

#### Fonctionnalités
- Écran principal : pavé numérique (0–9, `*`, `#`) + champ affichage du numéro saisi
- Bouton `Appeler` (vert) : envoie une demande d'appel
- Accès rapide : liste des contacts avec bouton `📞` sur chaque ligne
- Historique d'appels : appels passés, reçus, manqués avec horodatage RP

#### Flow d'un appel
```
Appelant appuie sur [Appeler]
        │
        ▼
Serveur envoie PacketCallRequest → destinataire
        │
        ▼
Destinataire : notification appel entrant
(nom/numéro appelant + [Accepter] / [Refuser])
        │
   ┌────┴────┐
[Accepter]  [Refuser]
   │              │
   ▼              ▼
Serveur crée    PacketCallResponse(refusé)
groupe SVC      → appelant voit "Appel refusé"
privé temp.
   │
   ▼
Les deux joueurs rejoignent le groupe SVC
(vocal privé, inaudible des autres)
   │
   ▼
[Raccrocher] → dissolution du groupe SVC
```

#### Intégration Simple Voice Chat
- Création de **groupes privés temporaires** via l'API SVC
- Un appel = un groupe SVC avec les deux UUID joueurs
- Raccrocher (l'un ou l'autre) = dissolution du groupe
- **Dépendance requise** à partir de cette partie

#### Packets
- `PacketCallRequest` — appelant → serveur → destinataire
- `PacketCallResponse` — destinataire → serveur → appelant (`accepted: boolean`)
- `PacketCallEnd` — dissolution groupe SVC + notification à l'autre joueur

---

### PARTIE 5 — App Map

**Objectif** : Afficher la carte statique de l'island avec navigation style Google Maps.

#### Fonctionnalités
- Affichage de `map_island.png` (image haute résolution)
- **Point de position** du joueur : point coloré mis à jour toutes les 20 ticks
- **Zoom** : molette souris + boutons `+` / `-`
  - Zoom min : vue globale de toute l'island
  - Zoom max : vue détaillée d'un quartier
- **Déplacement** : clic maintenu + drag (pan)
- **Recentrage** : bouton `⊙` pour recentrer sur la position du joueur

#### Technique
- Rendu via `RenderSystem` avec matrices de transformation (zoom + pan)
- Coordonnées Minecraft → coordonnées image : mapping proportionnel selon dimensions de la map
- `mapOffsetX`, `mapOffsetY`, `mapScale` stockés localement, reset à chaque ouverture

---

### PARTIE 6 — App News

**Objectif** : Journal RP publié par les modérateurs, consulté par les joueurs.

#### Fonctionnalités joueur
- Liste des articles triés du plus récent au plus ancien
- Chaque article dans la liste : titre + auteur (pseudo RP modo) + date RP + aperçu 1 ligne
- Clic sur un article : lecture complète avec scroll
- Point rouge sur l'icône News si article non lu

#### Publication par les modos
- Commande : `/news publish <titre> | <contenu>`
- Sauvegarde côté serveur dans `config/districtlife_phone/news.json`
- Structure : `{id, titre, auteur, contenu, tickRP, dayRP}`
- Maximum 50 articles (les plus anciens supprimés automatiquement)
- Limite : 500 caractères titre, 2000 caractères contenu
- À la publication : `PacketReceiveNews` envoyé à tous les connectés

#### Packets
- `PacketReceiveNews` — serveur → tous les clients à la publication
- `PacketSyncNews` — serveur → client à la connexion (envoi de tous les articles)

---

### PARTIE 7 — App Appareil Photo

**Objectif** : Intégrer Camera Mod by henkelmax pour prendre des photos sauvegardées dans la galerie.

#### Fonctionnalités
- Bouton `📷 Prendre une photo` : déclenche la prise de vue via Camera Mod
- La photo est sauvegardée dans `.minecraft/screenshots/districtlife/` avec nom horodaté
- Le chemin est ajouté à `galleryImages` dans la capability via `PacketSyncPhone`
- Message de confirmation : `« Photo enregistrée dans votre galerie »`
- Affichage du nombre de photos actuellement en galerie

#### Intégration Camera Mod
- Écoute de l'événement de fin de capture Camera Mod
- Copie du fichier dans le dossier dédié DistrictLife
- **Dépendance requise** à partir de cette partie

---

### PARTIE 8 — App Galerie

**Objectif** : Afficher et gérer les photos prises avec l'appareil photo.

#### Fonctionnalités
- Grille de miniatures 3 colonnes, chargées depuis `.minecraft/screenshots/districtlife/`
- Scroll vertical si la grille dépasse la hauteur du cadre
- Clic sur une miniature : affichage plein écran dans le cadre
  - Bouton `←` : retour grille
  - Bouton `🗑` : suppression (retire de la capability + supprime le fichier local)
- Si aucune photo : message centré `« Aucune photo »`

#### Technique
- Chargement dynamique des textures via `TextureManager` / `NativeImage`
- Miniatures mises en cache localement au premier chargement
- Pas de transit réseau — fichiers 100% locaux au client

---

### PARTIE 9 — App Paramètres

**Objectif** : Permettre au joueur de choisir sa sonnerie.

#### Fonctionnalités
- Section **Sonnerie** : liste de 3 choix radio style iOS (cercle coché)
  - `Classique` → son `minecraft:block.note_block.pling`
  - `Bip` → son `minecraft:entity.experience_orb.pickup`
  - `Silence` → aucun son
- Bouton `Sauvegarder` : enregistre le choix dans la capability
- La sonnerie est jouée à la réception d'un appel (Partie 4) et d'un SMS (Partie 3)

#### Packets
- `PacketUpdateSettings` — client → serveur : `{ringSoundName}`
- Serveur met à jour la capability et renvoie un `PacketSyncPhone`

---

## CONVENTIONS DE CODE

- Langue du code : **anglais**
- Langue des textes joueur / logs : **français**
- Pas de lambdas complexes (compatibilité Java 8)
- GUI : système vanilla Forge (`Screen` + `AbstractWidget`), pas de lib externe
- Toutes les textures GUI en PNG 32-bit, fond transparent si nécessaire
- `fr_fr.json` pour tous les textes affichés au joueur
- Simple Voice Chat : optionnel en Parties 1–3, **requis** à partir de la Partie 4
- Camera Mod : optionnel en Parties 1–6, **requis** à partir de la Partie 7

---

## NOTES IMPORTANTES

- Le téléphone n'est **pas craftable** — uniquement via `/phone give`
- Chaque joueur a un **numéro RP unique et permanent** (ne change pas sauf reset)
- Toutes les communications passent par le **numéro RP**, jamais par le pseudo Minecraft directement
- La galerie est **locale au client** — les fichiers photos ne transitent pas par le serveur
- Le GUI doit rester **lisible à toutes les résolutions** Minecraft (scalable)