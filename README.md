# AMAP Distribution

Application Android pour gérer les distributions de paniers AMAP. Développée pour simplifier le suivi des commandes et des livraisons.

---

## ⚠️ À propos de cette application

> **⚠️ Cette application a été entièrement générée par IA** (Mistral Vibe) à partir d'un prompt utilisateur. Elle est fonctionnelle mais n'a pas bénéficié d'une revue de code complète par des développeurs humains. Utilisez-la à vos risques et périls.

---

## 📱 Fonctionnalités

### Gestion des distributions
- **Liste des membres** : Vue claire de tous les participants à la distribution
- **Suivi en temps réel** : cochez les articles au fur et à mesure qu'ils sont remis
- **Statut visuel** : Les personnes déjà servies sont grisées/barrées ou cachées
- **Filtrage des articles** : Option pour masquer les articles qui ne sont pas 
distribués systématiquement (viande, confiture...)

### Import des données
- **Fichier CSV** : Import depuis un fichier local sur l'appareil
- **Google Sheets** : Téléchargement direct depuis un tableau Google Sheets partagé

### Synchronisation multi-appareils
- **QR Codes** : Générez un QR code avec l'état actuel et scannez-le sur un autre appareil pour synchroniser les données

### Visualisation
- **Vue tableau** : Visualisez le CSV brut dans un tableau
- **Fiche détaillée** : Voir tous les articles d'une personne

---

## 🚀 Installation

### Prérequis
- Appareil Android (version 8.0 Oreo ou supérieure)
- ~5 Mo d'espace de stockage

### Méthode 1 : Installer l'APK fourni

1. **Télécharger l'APK** : Récupérez le fichier `apk/amap-app-debug.apk` depuis ce dépôt
2. **Activer les sources inconnues** : 
   - Allez dans **Paramètres > Sécurité** (varie selon Android)
   - Activez **"Sources inconnues"** ou **"Installation d'applications inconnues"**
3. **Installer l'APK** : Ouvrez le fichier `.apk` avec un gestionnaire de fichiers et suivez les instructions

### Méthode 2 : Build depuis les sources

```bash
# Prérequis
# - Android SDK (dossier configuré dans local.properties)
# - Java JDK 17+

# Builder l'APK
./build.sh

# L'APK est généré dans : app/build/outputs/apk/debug/app-debug.apk
```

---

## 📂 Préparation des données

### Récupérer un fichier CSV existant

Le plus simple : utiliser le bouton "Télécharger le tableau de distribution" 
(pour l'AMAP "Flanc de Coteau" à Seyssins: récupère directement depuis le 
tableur partagé).

Possiblilité de télécharger depuis un tableur partagé (dans google docs : menu Fichier > Télécharger > (.csv)).



### Créer un fichier CSV

Créez un fichier CSV avec le format suivant :

```csv
Nom,Article 1,Article 2,Article 3
Alice,1 panier légumes,1 douzaine d'œufs,1 pain
Bob,1 panier légumes,2 fromages,
Claire,2 paniers légumes,,1 fromage de chèvre
```

**Règles :**
- Première colonne : **Noms** des membres (obligatoire)
- Colonnes suivantes : Articles à distribuer
- Les cellules vides sont ignorées
- Les lignes commençant par `#` ou `//` sont ignorées (commentaires)
- Séparateurs supportés : **virgule (`,`) ou point-virgule (`;`)**


### Exemple de fichier

Un fichier d'exemple (`example.csv`) est inclus dans l'application et utilisé par défaut au premier lancement.

---

## 🎯 Utilisation de l'application

### Premier lancement

1. **Ouvrez l'application**
2. L'application charge automatiquement `example.csv` si aucun fichier n'a été importé
3. Vous verrez la liste des membres avec leurs articles

### Importer un fichier CSV

**Depuis l'écran d'accueil :**
1. Cliquez sur **"Charger un CSV"**
2. Sélectionnez votre fichier dans le gestionnaire de fichiers
3. L'application charge immédiatement les données

### Télécharger depuis Google Sheets

**Depuis l'écran d'accueil :**
1. Cliquez sur **"Télécharger le tableau de distribution"**
2. Attendez la fin du téléchargement (indiqué par un spinner)
3. Les données sont automatiquement chargées

> ⚙️ **Configuration** : L'URL du Google Sheet est préconfigurée. Pour changer, modifiez `googleSheetsUrl` dans `MainViewModel.kt`.

### Gérer une distribution

**Écran principal :**
- **Cochez les articles** : Cliquez sur une case pour cocher/décocher un article
- **Marquer comme servi** : Cochez tous les articles puis cliquez sur **"Valider"** (ou appui long sur la personne)
- **Masquer les servis** : Activez le bouton **"Montrer terminés"** pour afficher/masquer les personnes déjà servies

**Écran détaillé d'une personne :**
- Cliquez sur un nom dans la liste
- Vue complète de tous les articles de cette personne
- Bouton de retour pour revenir à la liste

### Synchronisation avec QR Code

**Pour synchroniser entre deux appareils :**

1. **Sur l'appareil source** (celui qui a les données à jour) :
   - Dans l'écran principal, cliquez sur l'icône **QR Code** (en haut à droite)
   - Sélectionnez **"Générer QR Code"**
   - Montrez le QR code à l'autre appareil

2. **Sur l'appareil cible** :
   - Cliquez sur l'icône **QR Code**
   - Sélectionnez **"Scanner QR Code"**
   - Scannez le code de l'autre appareil
   - Les données sont fusionnées automatiquement

> ⚠️ **Note** : La synchronisation fusionne les données. Si un article est coché sur l'appareil source, il sera coché sur le cible. Le hash du CSV doit correspondre.

### Visualiser le tableau

1. Depuis l'écran principal, cliquez sur l'icône **Tableau** (en haut à droite)
2. Vous voyez le CSV brut sous forme de tableau
3. Utilisez le bouton **Retour** pour revenir

---

## 🔧 Configuration technique

### Scripts utilitaires

#### `upload-and-run.sh`

Script pratique pour développer/tester l'application :

```bash
# Utilisation de base (avec l'appareil par défaut)
./upload-and-run.sh

# Avec un appareil spécifique (configuré dans ~/.adb-devices)
./upload-and-run.sh mon-telephone

# Le script :
# 1. Build l'APK
# 2. Installe sur l'appareil
# 3. Pousse example.csv vers l'appareil via intent
# 4. Lance l'application
```

**Fichier de configuration des appareils** (`~/.adb-devices`) :
```
mon-telephone  192.168.1.xxx:5555
autre-appareil AB12CD34
```

---

## 📁 Structure du projet

```
amap-app/
├── README.md                    # Ce fichier
├── .gitignore
├── local.properties            # Configuration SDK (généré)
├── build.gradle.kts            # Configuration Gradle du projet
├── settings.gradle.kts
├── build.sh                    # Script de build
├── upload-and-run.sh           # Script de déploiement
├── example.csv                 # Fichier CSV d'exemple
├── apk/                        # APKs générés
│   └── amap-app-debug.apk      # Dernière version compilée
└── app/
    ├── build.gradle.kts
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   ├── assets/
    │   │   └── example.csv      # CSV embarqué dans l'APK
    │   ├── res/                # Ressources
    │   └── java/com/amap/app/
    │       ├── MainActivity.kt
    │       ├── model/
    │       │   ├── CsvParser.kt
    │       │   ├── Item.kt
    │       │   └── Person.kt
    │       ├── viewmodel/
    │       │   └── MainViewModel.kt
    │       └── ui/
    │           ├── CsvTableScreen.kt
    │           ├── DetailScreen.kt
    │           ├── HomeScreen.kt
    │           ├── MainScreen.kt
    │           ├── QrSyncDialog.kt
    │           └── theme/
    └── build/                  # Fichiers générés (ignoré par git)
```

---

## 🤝 Contribuer

1. **Fork** le dépôt sur GitHub
2. **Clone** votre fork
3. **Créez une branche** pour votre fonctionnalité : `git checkout -b ma-nouvelle-fonctionnalité`
4. **Commit** vos changements : `git commit -m "Ajout de ma fonctionnalité"`
5. **Push** vers votre fork : `git push origin ma-nouvelle-fonctionnalité`
6. **Ouvrez une Pull Request** depuis GitHub

---

## 🐛 Signaler un problème

Ouvrez une **issue** sur GitHub avec :
- Description du problème
- Étapes pour reproduire
- Capture d'écran si nécessaire
- Version d'Android
- Modèle de l'appareil

---

## 📄 Licence

Cette application est fournie "en l'état" sans garantie d'aucune sorte. Vous êtes libre de l'utiliser, la modifier et la redistribuer.

**Auteur initial** : Généré par Mistral Vibe (IA)
**Mainteneur** : [À compléter]

---

## 📞 Contact

Pour toute question concernant l'AMAP, contactez votre référent AMAP habituel.

Pour les problèmes techniques liés à l'application, ouvrez une issue sur le dépôt GitHub.
