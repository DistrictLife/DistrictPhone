# DistrictLife Phone

Mod Forge 1.16.5 — Téléphone RP pour le serveur DistrictLife.  
Interface smartphone moderne avec appels vocaux, SMS, contacts et applications.

---

## Prérequis

| Outil | Version |
|---|---|
| Minecraft Forge | 1.16.5-36.2.34 |
| [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) | 1.16.5-2.5.36+ |
| Java (pour build local) | JDK 11 + JDK 21 |

---

## Build local

```bash
# Toujours utiliser Java 11 pour lancer Gradle
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-11.0.30.7-hotspot"

./gradlew jar
# Le JAR est dans : build/libs/districtlife_phone-X.X.X.jar
```

---

## Publier une nouvelle version (push + release)

### 1. Faire tes modifications et les commiter

```bash
git add .
git commit -m "feat: description de ce que tu as ajouté"
git push
```

> **Exemples de messages de commit :**
> ```
> feat: ajout de l'application Carte avec position du joueur
> fix: correction du crash au raccrochage d'un appel
> ui: redesign de l'écran d'accueil
> refactor: migration des données vers le NBT de l'ItemStack
> ```

---

### 2. Créer un tag de version avec un message de changelog

Le tag est la **source de vérité unique** pour la version.  
Son message devient le changelog de la release.

```bash
git tag -a v1.2.0 -m "$(cat <<'EOF'
DistrictLife Phone v1.2.0

Nouveautés :
- Ajout de l'application Carte avec position en temps réel
- Nouveau fond d'écran personnalisable depuis les Paramètres

Corrections :
- Crash au raccrochage si le partenaire est déconnecté
- Numéros mal formatés dans la liste de contacts
EOF
)"
```

> **Convention de version `vMAJEUR.MINEUR.PATCH` :**
> - `MAJEUR` — refonte majeure ou changement incompatible
> - `MINEUR` — nouvelle fonctionnalité
> - `PATCH` — correction de bug

---

### 3. Pousser le tag → déclenche la release automatique

```bash
git push origin v1.2.0
```

GitHub Actions va alors :
1. Compiler le JAR avec `./gradlew jar -Pmod_version=1.2.0`
2. Créer la release `DistrictLife Phone v1.2.0` avec le JAR en pièce jointe

La release est visible sur : **[github.com/DistrictLife/DistrictPhone/releases](https://github.com/DistrictLife/DistrictPhone/releases)**

---

### En résumé (commande rapide)

```bash
# 1. Commiter et pousser le code
git add .
git commit -m "feat: ma nouveauté"
git push

# 2. Créer et pousser le tag (déclenche la release)
git tag -a v1.X.X -m "Description du changelog"
git push origin v1.X.X
```

---

## Structure du projet

```
src/
├── main/java/com/districtlife/phone/
│   ├── call/          # Système d'appels (CallManager, états)
│   ├── data/          # Stockage NBT des données du téléphone
│   ├── network/       # Packets réseau client ↔ serveur
│   ├── screen/        # Interface graphique (PhoneScreen, apps)
│   └── svc/           # Intégration Simple Voice Chat
└── main/resources/
    └── assets/        # Textures, icônes, langues
```
