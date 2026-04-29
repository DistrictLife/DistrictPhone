# DistrictPhone — Notes de développement

## Idées à implémenter plus tard

### Notification dots sur l'accueil
Afficher `notif_point.png` en overlay en haut à droite des icônes concernées (grille + dock).

**Apps concernées :**
- **News** : compteur d'articles non lus (`NewsClientCache`)
- **SMS** : compteur de SMS non lus (client-side cache à créer, incrémenté via `PhoneNetEvent.SmsNotify`)
- **Appel** : pas de compteur — afficher `!` uniquement si appel entrant (`PhoneCallState.getState() == CallState.RINGING`)

**Règles d'affichage :**
- 1–9 : chiffre centré sur le point
- 10+ : afficher `9+`
- 0 : pas de point
- Appel entrant : `!` centré (pas de chiffre)

**Taille fixe** (indépendante du GUI scale) : calculer via `phoneWidth / 424f` comme les autres éléments.

**Fichier texture :** `src/main/resources/assets/districtlife_phone/textures/gui/phone/accueil/notif_point.png`

---

## Intégration DLCitizens (à implémenter plus tard)

### Principe
Le **numéro de téléphone** est l'identité naturelle du système — pas l'UUID du joueur. Un téléphone peut être volé, échangé : les données suivent le téléphone, pas le joueur.

### Données à stocker dans SQLite (DLCitizens)

```sql
-- Journal d'appels
CREATE TABLE phone_calls (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    caller_phone  TEXT NOT NULL,
    callee_phone  TEXT NOT NULL,
    started_at    INTEGER NOT NULL,
    duration_sec  INTEGER NOT NULL DEFAULT 0,
    status        TEXT NOT NULL    -- 'answered','missed','rejected','impossible'
);

-- SMS (avec livraison hors ligne)
CREATE TABLE phone_sms (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_phone   TEXT NOT NULL,
    receiver_phone TEXT NOT NULL,
    message        TEXT NOT NULL,
    sent_at        INTEGER NOT NULL,
    delivered      INTEGER NOT NULL DEFAULT 0  -- 0=en attente, 1=livré
);

-- Lecture des news par téléphone
CREATE TABLE news_read (
    phone_number TEXT NOT NULL,
    article_id   INTEGER NOT NULL,
    read_at      INTEGER NOT NULL,
    PRIMARY KEY (phone_number, article_id)
);
```

### Affichage de l'identité dans les appels et SMS
- **Numéro dans les contacts** → afficher le nom du contact (`PhoneData.getContacts(stack)`)
- **Numéro inconnu** → afficher uniquement le numéro de téléphone
- Aucun packet supplémentaire nécessaire, logique 100% locale côté client
- Fichiers concernés : `PhoneCallHud`, `AppSMS`, `AppPhone`

### Lien réseau DistrictPhone ↔ DLCitizens
- DLCitizens accède au canal de DistrictPhone via réflexion sur `com.districtlife.phone.network.PacketHandler.CHANNEL` (même pattern que dlclient)
- Le nom RP (DLCitizens) n'est PAS utilisé pour l'affichage dans le téléphone — uniquement pour les commandes admin et logs judiciaires

### Flow SMS hors ligne
```
SMS envoyé au numéro X (téléphone hors ligne)
  → DLCitizens INSERT phone_sms (delivered=0)
  → Le téléphone numéro X est ouvert en jeu
  → SELECT WHERE receiver_phone=X AND delivered=0
  → Envoi au client via packet
  → UPDATE delivered=1
```

### Conséquences RP
- Téléphone volé → le voleur voit les SMS, l'historique d'appels, les news lues (cohérent)
- Changement de téléphone → historique lié à l'ancien numéro (logique)
