# AMAP App

## Règles générales
- Toujours commit les changements après chaque modification (git add + git commit)
- Build avec `./build.sh`
- Le projet est en Kotlin Jetpack Compose

## Structure
- `app/src/main/java/com/amap/app/` — code source Kotlin
- `ui/MainScreen.kt` — écran principal avec la liste des personnes
- `ui/DetailScreen.kt` — détail d'une personne avec ses articles
- `viewmodel/MainViewModel.kt` — state management
- `model/` — modèles Person, Item, CsvParser
- `upload-and-run.sh` — build + install sur device via adb
