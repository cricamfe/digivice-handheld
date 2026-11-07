# Digivice para portátiles Android con dos pantallas

App Android que muestra Digivice en la pantalla inferior mientras Digimon World 2003 corre en RetroArch en la superior. La AYN Thor es el único dispositivo probado. Solo lee la memoria del juego: no escribe en ella ni le envía entradas.

```
RetroArch nightly + SwanStation --(UDP 127.0.0.1:55355, READ_CORE_MEMORY)--> core (Kotlin) --> WebView (Vue) en la pantalla inferior
```

## Requisitos

- **RetroArch nightly** `com.retroarch.aarch64`, compilación del 2026-11-03 (`versionCode` 1793667751), con el núcleo SwanStation.
- **Juego:** Digimon World 2003, versión europea (`SLES-03936`), original o con Flawe Mod 2.0 Combined.
- **`retroarch.cfg`:**
  ```
  network_cmd_enable = "true"
  network_cmd_port = "55355"
  network_cmd_bind_address = "127.0.0.1"
  ```
  La app no puede editarlo, porque está en los datos de otra app.
- **Permiso «Mostrar sobre otras apps»:** lo concede quien usa la consola, desde la primera pantalla de la app.

### No actualices RetroArch sin probar

Los comandos UDP solo funcionan en Android desde 2026-03-25, y la lectura de núcleos sin mapa de memoria, como SwanStation, depende de la nightly. Una nightly antigua o la versión estable responden `-1 no memory map defined` con el juego cargado, y cualquier nightly nueva puede romper los comandos. Si Obtainium ofrece una actualización, pruébala antes de quedarte con ella.

## Compilar e instalar

Necesita Android Studio (por su JDK), NDK 28.2.13676358, CMake 3.22.1, Node y `adb`, con la consola conectada. El APK se compila para `arm64-v8a`.

```bash
git submodule update --init
(cd Frontend && npm ci)
```

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./Android/gradlew -p Android :core:test :app:check :app:assembleRelease
```

```bash
adb install -r Android/app/build/outputs/apk/release/app-release.apk
```

Sin `Android/keystore.properties`, la versión `release` se firma con la clave de depuración: sirve para tu consola, no para publicarla. Con ese archivo, que git ignora, se firma con tu clave; cómo crearla está en el [README](../README.md#firma). `:app:assembleDebug` es más lenta en la consola, pero deja inspeccionar el WebView desde Chrome.

Instala las dependencias con `npm ci` al preparar el proyecto o cambiar `package-lock.json`. Cada compilación del APK comprueba los tipos y regenera la interfaz antes de copiarla; `:app:check` ejecuta también sus pruebas. Node y npm deben estar en el `PATH` de Gradle, incluido al compilar desde Android Studio. Las pruebas de `:core` se pueden ejecutar solas sin Node.

## Uso

- En el primer inicio, el asistente de Digivice abre el selector de Android para elegir una ISO, un BIN de una sola pista o un CHD v5 de **Digimon World 2003 europeo original** (`SLES-03936`). Puedes elegir el mismo CHD que usas en RetroArch si es del juego original. Digivice lo abre en modo de lectura y valida el disco. Si la extracción se interrumpe, puedes reanudarla desde el asistente.
- **Original y mod:** abre el archivo del juego que quieras en RetroArch; Digivice reconoce ambas versiones. Para continuar una partida en el mod, copia la tarjeta de memoria con el nombre del archivo nuevo y carga la partida dentro del juego. No cargues estados rápidos del original en el mod: restauran también su código en RAM.
- Ábrela antes o después de RetroArch: aparece en la pantalla inferior cuando RetroArch responde, y se retira a los 5 segundos si deja de hacerlo, por ejemplo con RetroArch en segundo plano.
- Al cerrar RetroArch, la app se cierra sola. Para la siguiente partida hay que abrirla otra vez.
- **Pulsación larga** en cualquier pantalla: idioma, ocultar, salir y licencias (versión, GPL, dónde está el código y los textos de las licencias que incluye el APK). Ocultar la mantiene fuera hasta que abras la app otra vez.
- **Idiomas:** español, inglés, francés, alemán e italiano. El asistente sigue el idioma de Digivice configurado en Android. El panel de juego se puede cambiar desde el menú de pulsación larga.

## Pruebas

```bash
cd Android && ./gradlew :core:test
```

```bash
cd Frontend && npm run test:handheld && npx vue-tsc --noEmit -p tsconfig.app.json
```

Probar la interfaz sin consola, con una partida guardada de las referencias:

```bash
cd Frontend && npm run dev:handheld
```

Abre `http://localhost:5173/handheld.html?mock=<snapshot>&screen=mapa`; usa el nombre, sin `.json`, de un estado en `Android/core/src/test/resources/golden/state`.

## Datos fijos de prueba

- `Backend/Memory/Definitions`: las direcciones de RAM del proyecto original, en JSON. El APK las copia como `definitions/` y las pruebas de `:core` las leen en su sitio. Del backend en C# solo quedan estos datos.
- `Tools/MemoryScanner/Snapshots` y `Tools/RetroArchProbe/thor-title.bin`: volcados de RAM del juego. No están en el repositorio y git los ignora; las pruebas que comparan memoria real se saltan con un aviso si no los pones ahí. Las del bucle y el cliente usan RAM sintética y no los necesitan.
- `Android/core/src/test/resources/golden`: los estados y eventos esperados de esos volcados. Los generó la herramienta en C# del proyecto original y ahora se mantienen a mano junto con el core.

Los comentarios `Port of Backend/…` y `Port of Tests/…` del core nombran el archivo del [proyecto original](https://github.com/jeanheck/digivice) del que viene cada clase.

## Regenerar lo que sale del juego

| Qué | Cómo |
|---|---|
| Iconos de objetos | `Tools/RomExtract/item_icons.py` |
| Personajes del campo y su papel | `Tools/RomExtract/field_actors.py` |
| Técnicas y formas de combate | El extractor Kotlin lee las tablas del ejecutable PAL y sus textos del disco. |
| Rutas de Seabed | `Tools/FieldData/generate_seabed.py <dw3_decomp> <esitmnam.toml>` |
| Nombres por idioma | `Tools/FieldData/generate_locales.py <lang_file/dw2003> <salida>` |
| Condiciones de cada misión | `Tools/FieldData/generate_quest_conditions.py <dw3_decomp>` |

Las fuentes son [ReGame-Labs/dw3_decomp](https://github.com/ReGame-Labs/dw3_decomp) y [markisha64/ddw3](https://github.com/markisha64/ddw3), con tu copia del juego. Cuándo se puede empezar cada misión sale de los scripts del juego que lee `generate_quest_conditions.py`.

## Traducciones

Cada idioma está en `Frontend/src/i18n/locales/<idioma>/`:

- Los textos del disco se generan en `texts/<idioma>.json` en el directorio privado de la app. Los archivos de prueba equivalentes se mantienen fuera del repositorio.
- `ui.json`: interfaz. `quests.json`: misiones.

Si un texto no existe en un idioma, sale en inglés.
