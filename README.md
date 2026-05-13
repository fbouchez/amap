# AMAP Distribution

Application Android pour faciliter la distribution des paniers AMAP.

## Fonctionnalités

- Import d'un fichier CSV (1ère colonne = noms, suivantes = articles à prendre)
- Liste des personnes avec statut (pas encore venu / déjà servi)
- Vue détaillée avec checklist des articles à prendre
- Marquage « validé » quand tout est pris
- Affichage des personnes déjà servies (grisées/barrées) avec bouton pour les masquer
- Sauvegarde de l'état (coches, validations) entre les lancements
- Auto-chargement du CSV poussé par `upload-and-run.sh`

## Prérequis

- Android SDK dans `~/Installs/android`
- Gradle dans `~/Installs/gradle/bin`
- Tablette ou téléphone Android branché en USB (debug activé)

## Build

```bash
./build.sh
```

## Upload sur l'appareil

```bash
# Copier son fichier CSV dans le dossier
cp /chemin/vers/distribution.csv current.csv

# Build + install + lancement
./upload-and-run.sh
```

Le script pousse automatiquement `current.csv` sur l'appareil via l'intent de lancement (encodé en base64). L'app le détecte au démarrage et charge les données.

## Format CSV

Séparateur `;` ou `,`. Support des guillemets et des champs multi-lignes.

```csv
Nom;Article 1;Article 2;Article 3
Alice;1 panier légumes;1 douzaine d'œufs;1 pain
Bob;1 panier légumes;2 fromages;
```

Les lignes commençant par `#` ou `//` sont ignorées. La première ligne (en-tête) est ignorée automatiquement.

## Structure du projet

```
amap-app/
├── build.gradle.kts          # Projet Gradle
├── settings.gradle.kts
├── build.sh                  # Build
├── upload-and-run.sh         # Build + install + lancement
├── local.properties          # SDK path
├── current.csv               # Données de distribution (ignoré par git)
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/amap_sample.csv
        ├── res/
        └── java/com/amap/app/
            ├── MainActivity.kt
            ├── model/
            │   ├── Person.kt
            │   └── CsvParser.kt
            ├── viewmodel/
            │   └── MainViewModel.kt
            └── ui/
                ├── MainScreen.kt
                ├── DetailScreen.kt
                └── theme/Theme.kt
```
