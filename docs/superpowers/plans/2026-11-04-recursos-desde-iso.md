# Recursos desde la copia del usuario: plan de implementación

> Al retomar este plan, usar subagentes nativos de Codex: un agente implementa cada tarea y otro la revisa antes de continuar. Las casillas (`- [ ]`) registran trabajo comprobado.

## Estado y orden confirmado · 2026-11-05

- El dueño retomó la implementación el 2026-11-05. Tareas 1 a 4 completadas y revisadas; hito 1 aprobado en la Thor (30 mapas, estimación 4 min 57 s, CPU máxima observada 38,3 °C) en `recursos-iso`. Hito 2 completado y revisado: tareas 5 a 9. Extracción completa y reanudación probadas en el Mac; integración visual y extracción completa en Android pendientes. Las tareas siguientes conservan sus comprobaciones y revisión independiente. Evidencia: `docs/superpowers/journal/extractor.md`.
- Primero se estabilizan y commitean la compilación y la gestión de sesión de Digivice. La continuación del port nativo queda después de esos cambios probados.
- **Antes de publicar cualquier repositorio o APK**, deben completarse los hitos de este plan: extracción desde la copia del usuario, interfaz y asistente, lectura CHD y auditoría del paquete y del historial. El hito 6 conserva la revisión y confirmación final del dueño.
- El destino público sigue siendo un repositorio solo Android, conservando los créditos y el historial pertinente del proyecto original.
- Hay una muestra CHD europea original en `../refs/rom/`, fuera del repositorio. El BIN obtenido con `chdman` coincide con la ISO en los 294.280 sectores.

**Goal:** Que el APK y el repositorio públicos no lleven material del juego, y que la app genere imágenes y textos en la consola desde el archivo de cada usuario.

**Architecture:** Un módulo Kotlin puro, `Android/extractor`, lee el disco y produce píxeles y JSON; lo usan la app (WebP en almacenamiento privado) y una línea de órdenes (PNG para desarrollo). La interfaz deja de empaquetar recursos y los pide por un manifiesto servido en `/game/`. Un asistente en la pantalla superior guía la primera extracción.

**Tech Stack:** Kotlin/JVM 17, Gradle, JUnit 5, Android (SAF, foreground service, WebViewAssetLoader), NDK con `libchdr` solo para CHD, Vue 3 + vitest, Python 3 para generar tablas y referencias.

**Spec:** `docs/superpowers/specs/2026-11-04-recursos-desde-iso-design.md`. Inventario: `docs/superpowers/journal/asset-inventory.md`.

## Global Constraints

- Rama de trabajo `recursos-iso`, creada al retomar desde la revisión Android estabilizada que incorpore los ajustes de compilación y sesión. Se conserva `android-thor` como referencia de lo que el dueño juega hasta el final del hito 4. Commits permitidos, **sin push** hasta la tarea 16 y solo con confirmación del dueño.
- Los commits reflejan su autoría real; se conservan los créditos del trabajo heredado.
- Solo `SLES-03936`. Ninguna escritura sobre el archivo del usuario ni sobre el juego.
- Nunca al repositorio: el disco, salidas de extracción, volcados nuevos de RAM ni `ddw3`. `Frontend/game-dev/` y las salidas de prueba, en `.gitignore`.
- No tocar la pantalla de la consola por adb mientras el dueño juega ni cambiar ajustes del sistema.
- `:extractor` sin dependencias de Android, con `allWarningsAsErrors` como `:core`.
- Interfaz: Press Start 2P solo a 10, 12 o 16 px, iconos a 22, 44 u 88 px, `kit.css`. Textos nuevos en los cinco idiomas.
- Máximo dos hilos de extracción.
- Referencias locales: `DW3_ISO` (imagen de 2.048 bytes por sector), `DW3_REFERENCE` (salidas de Python). Las pruebas que las necesitan llevan `@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")` y se saltan sin ellas.
- Rutas locales del desarrollador: decompilación en `../refs/dw3_decomp` respecto al repositorio; ddw3 se pasa por argumento.

## Review Focus

- Archivo equivocado o incompleto (otro juego, versión de EE. UU., BIN truncado): mensaje claro, ningún recurso escrito. Prueba en la tarea 1.
- Proceso matado a mitad de un mapa: al volver se reanuda y nunca se usa un recurso parcial. Prueba en la tarea 9.
- Sin espacio durante la extracción: mensaje, temporales borrados, reintento posible. Prueba en la tarea 13.
- Archivo movido o permiso perdido al regenerar: vuelve a pedir el archivo sin perder los recursos que funcionan. Prueba en la tarea 13.
- Recurso ausente del manifiesto o manifiesto antiguo tras actualizar: icono de desconocido, sin error de carga, y la app pide regenerar. Prueba en la tarea 10.

---

## Hito 1: pinchazo de mapas en la Thor

### Task 1: Módulo y lectura del disco

**Files:**
- Create: `Android/extractor/build.gradle.kts`, `.../extractor/disc/{ByteSource,Disc,IsoDisc,BinDisc,DiscFiles,GameFileTable,GameCheck}.kt`
- Modify: `Android/settings.gradle.kts`
- Test: `Android/extractor/src/test/kotlin/.../extractor/disc/DiscTests.kt`, con un helper que construye en memoria un ISO9660 sintético

**Interfaces:**
- Produces:
  - `interface ByteSource : Closeable { val size: Long; fun read(position: Long, into: ByteArray, offset: Int, length: Int) }`, `class FileByteSource(file: File)`
  - `interface Disc : Closeable { fun read(lba: Int, bytes: Int): ByteArray }`, `fun openDisc(source: ByteSource): Disc?` (detecta por tamaño y sincronía; null si no es ninguno)
  - `class DiscFiles(disc: Disc) { fun byPath(path: String): DiscFile?; fun atLba(lba: Int): DiscFile?; fun read(file: DiscFile): ByteArray }`, `data class DiscFile(val path: String, val lba: Int, val size: Int)`
  - `class GameFileTable(files: DiscFiles) { fun lbaOf(index: Int): Int }`: `FILE_SECTORS`, 2.382 `int32` en `SLES_039.36` + 0x3576C
  - `sealed interface GameCheck { data object Valid; data object NotADisc; data object WrongGame; data object Truncated }`, `fun checkGame(source: ByteSource): GameCheck`, `const val EXE_SHA256`

- [x] **Step 1: Pruebas que fallan.** `iso sector maps to lba times 2048`; `bin sector maps to lba times 2352 plus 24`; `format is detected by sync bytes, not by the name`; `iso9660 lists files with lba and size`; `a file not on the disc is null`; `another game is WrongGame` (SYSTEM.CNF con otro ejecutable); `a right SYSTEM_CNF with another exe hash is WrongGame`; `a bin cut in half is Truncated`; `a random file is NotADisc`.
- [x] **Step 2:** `cd Android && ./gradlew :extractor:test`. Expected: FAIL, clases sin definir.
- [x] **Step 3: Implementar.** `iso.py` y `discfs.py` son la referencia. El SHA-256 se calcula de la ISO local y se fija como constante.
- [x] **Step 4:** prueba con disco: `the real iso is Valid and the file table points at SnnnPACK files`. Expected: PASS con `DW3_ISO`, sin saltarse.
- [x] **Step 5: Commit** `feat(extractor): leer ISO y BIN y reconocer el juego`.

### Task 2: Referencias de Python y tablas de datos

**Files:**
- Modify: `Tools/RomExtract/render/env.py` (define `S`, hoy `render_all.py` no arranca), `Tools/RomExtract/README.md` (dice que se conservan 75 ilustraciones: ya no)
- Create: `Tools/FieldData/generate_extractor_data.py`, `Android/extractor/src/main/resources/extractor/stages.json.gz`, `.../extractor/data/ExtractorData.kt`
- Test: `.../extractor/data/ExtractorDataTests.kt`

**Interfaces:**
- Produces: `object ExtractorData { val stages: Map<Int, StageData>; fun stage(mode: Int): StageData? }`. `StageData` lleva lo que `render/stagedef.py` saca de cada `wstag*.c`: `mapFile`, color de fondo, `stageObjects`. Sin nombres de escenario ni textos.

- [x] **Step 1:** generar `$DW3_REFERENCE/maps` con `render_all.py` (PNG sin pérdida). Expected: 237 mapas, 0 fallos.
- [x] **Step 2: Prueba que falla** `there are 294 stages and each has a map file or none`.
- [x] **Step 3: Implementar** el generador reutilizando `stagedef.load_stage`. Comprobar con `zcat` que no contiene cadenas de nombres.
- [x] **Step 4:** `./gradlew :extractor:test`. Expected: PASS.
- [x] **Step 5: Commit** `feat(extractor): tabla de escenarios y referencias locales`.

### Task 3: Pintar mapas

**Files:**
- Create: `.../extractor/image/Picture.kt`, `.../extractor/image/{Tim,Rlen,Pack}.kt`, `.../extractor/maps/{MapFile,StageSprites,MapRenderer}.kt`, `.../extractor/image/PngWriter.kt`
- Test: `.../extractor/maps/MapParityTests.kt` (con disco), `.../extractor/image/ImageTests.kt`

**Interfaces:**
- Consumes: `DiscFiles`, `GameFileTable`, `ExtractorData`.
- Produces: `class Picture(val width: Int, val height: Int, val argb: IntArray)`; `class MapRenderer(files: DiscFiles, table: GameFileTable) { val maps: List<Int>; fun render(mode: Int): Picture }`; `fun Picture.writePng(out: OutputStream)`, escrito sobre `java.util.zip.Deflater`.

Referencia: `mapx.py`, `render/dwr.py`, `render/align.py`. Enteros en `IntArray`, sin Canvas ni numpy.

- [x] **Step 1: Pruebas que fallan.** `tim 4 and 8 bit decode through the palette with colours replicated 5 to 8 bits`, con datos propios; `a png round trips through ImageIO`; `S331PACK and S206PACK equal the reference pixel by pixel`.
- [x] **Step 2:** `DW3_ISO=... DW3_REFERENCE=... ./gradlew :extractor:test`. Expected: FAIL.
- [x] **Step 3: Implementar.**
- [x] **Step 4:** añadir `all 237 maps equal the reference`. Expected: PASS, con diferencia 0; las diferencias se guardan como PNG en `build/`.
- [x] **Step 5: Commit** `feat(extractor): pintar los mapas como el juego`.

### Task 4: Medir en la Thor y decidir

**Files:**
- Create: `Android/app/src/debug/java/.../ExtractorProbeReceiver.kt` (como `RamDumpReceiver`), `docs/superpowers/journal/extractor-timing.md`
- Modify: `Android/app/build.gradle.kts`, añadir `debugImplementation(project(":extractor"))` (la integración de producción llega con el asistente), y el manifiesto debug con permiso `DUMP`.

- [x] **Step 1:** receptor debug restringido a `DUMP`, un mapa por broadcast con `goAsync()` y resultado JSON. Abre `getExternalFilesDir(null)/extractor-probe.iso`; el arnés encadena 30 mapas repartidos por tamaño. Escribe WebP al 75 en `cacheDir` mediante temporal y renombrado, registra tiempos y memoria y termina siempre el resultado pendiente.
- [x] **Step 2:** con el dueño fuera del juego y con su permiso, `adb push` de la ISO, instalar debug, lanzar, y borrar la ISO al acabar. Anotar la temperatura máxima de sensores `cpu-*` y `cpuss-*`; `thermal_zone0` en esta Thor es `pa`, no CPU. Pausa de 200 ms entre barridos, con cadencia media observada de 473 ms.
- [x] **Step 3:** estimación total: tiempo por megapíxel × 198 Mpx × 83/48 (proporción medida de todo sobre mapas en el Mac).
- [x] **Step 4: Decidir** con el criterio del diseño: aprobado, estimación 297,344 s, 30/30 sin errores/OOM y CPU máxima observada 38,3 °C. Se mantiene la tarea 7 por adelantado y se vuelve a medir el extractor completo; esta prueba corta no valida su temperatura sostenida. Evidencia y límites en `docs/superpowers/journal/extractor-timing.md`.
- [x] **Step 5: Commit** `docs: tiempos de extracción en la Thor`.

## Hito 2: extractor completo

### Task 5: Sprites, iconos y cartas

Referencia para las cartas: `Tools/RomExtract/probes/cards.py`.

**Files:**
- Create: `.../extractor/sprites/{FieldSprites,ItemIcons}.kt`, `.../extractor/cards/{CardImages,CardFrame}.kt`, tabla `resources/extractor/characters.json`, ampliar `generate_extractor_data.py`
- Test: `.../extractor/sprites/SpriteParityTests.kt`, `.../extractor/cards/CardParityTests.kt`

**Interfaces:**
- Produces: `class FieldSprites(files, table) { fun all(): Sequence<Pair<String, Picture>> }` con claves `actors/<personaje>-<dirección>`, y `npc/<nombre>` por la tabla nombre → personaje; `class ItemIcons(...)` con claves `items/item-XX`; `class CardImages(files) { fun render(cardId: Int): Picture }`, 40×46.

- [x] **Step 1: Pruebas que fallan.** `actors equal the repo's PNGs`; `items equal the repo's PNGs`; `card art equals the repo's at offset 4,2 within rounding` (media ≤ 0,5/255); `card id is 1000 plus disc index`; `ap and hp come from the card header`; `tamers npcs bosses stores and duel island have a sprite` (diferencia media ≤ 4 por canal, salvo los 5 tamers).
- [x] **Step 2:** Expected: FAIL.
- [x] **Step 3: Marco y cifras:** como mucho un día para leer `STCDSPTM` (TIM de 16 bits). Si no se localizan, dibujar seis marcos y las cifras en código, con colores propios. Anotar cuál fue en el ledger.
- [x] **Step 4: Los 5 tamers** (Bob, Genji, Natsumi, Tomomi, Wong): elegir fotograma del mismo personaje y fijarlo en la tabla.
- [x] **Step 5:** Expected: PASS. **Commit** `feat(extractor): sprites, iconos y cartas`.

### Task 6: Textos

**Files:**
- Create: `.../extractor/text/{GameText,TextOutput}.kt`, `resources/extractor/{charset,text-keys}.json`, `Tools/FieldData/generate_text_keys.py`
- Modify: `Tools/FieldData/generate_locales.py` (pasa a generar referencias en lugar de textos, y deja de escribir `*.generated.json`)
- Test: `.../extractor/text/TextParityTests.kt`, `TextTests.kt`

**Interfaces:**
- Produces: `class GameText(files) { fun table(language: GameLanguage, name: String): List<String> }`, `enum class GameLanguage(val folder: String, val prefix: String, val locale: String)` para ENG, SPN, FRA, GER, ITA; `fun buildTexts(text: GameText): Map<String, String>`, de locale a JSON con `{ "messages": {...}, "words": {"<tabla>:<línea>": "..."} }`.

Formato: `u32 count`, `count` offsets `u32`, cadenas terminadas en 0, y tabla de 358 códigos. Referencia: `Tools/RomExtract/probes/decode_text.py`. `text-keys.json`: clave de `en-US` → (tabla, línea), solo con hechos.

Textos sin línea exacta: similitud ≥ 0,88 usa la línea; el resto se queda en `en-US`, con lista explícita en `Tools/FieldData/en-us-own-texts.txt`.

- [x] **Step 1: Pruebas que fallan.** `a table with an accent and a control code decodes`, con datos propios; `a code that is not in the charset does not crash`; `each language's messages equal today's game.generated json`; `words of each language equal today's words.generated json`, ambas con disco y con la copia local de hoy como referencia.
- [x] **Step 2:** Expected: FAIL.
- [x] **Step 3: Implementar.** La β del alemán pasa a ß, y los nombres de hablantes pierden el punto final y empiezan en mayúscula, como en `generate_locales.py`.
- [x] **Step 4:** Expected: PASS salvo la línea conocida de SPN, anotada. **Commit** `feat(extractor): textos del juego en cinco idiomas`.

### Task 7: Modelos 3D

**Files:**
- Create: `.../extractor/models/{Rasterizer,FighterModels,StageModels}.kt`
- Test: `.../extractor/models/ModelParityTests.kt`, `RasterizerTests.kt`

**Interfaces:**
- Produces: `class FighterModels(files, table) { val enemies: List<Int>; val partners: List<Int>; fun render(id: Int, longSide: Int): Picture }`, `class StageModels(files, table) { val stages: List<Int>; fun render(stage: Int): Picture }`.

Referencia: `enemy_models.py` (382 líneas) y `stage_models.py` (250). Compañeros: `FighterInfo` de 0xC4 bytes en `partners + index*0xC4`, como `Tools/RomExtract/probes/partner_render.py`, que se incorpora a `enemy_models.py` para tener referencia.

- [x] **Step 1: Pruebas que fallan.** `a textured triangle fills what it should`; `all 172 enemies equal the reference`; `all 28 stages equal the reference`; `the 44 forms of digievolutions.json render`, sin imagen vacía.
- [x] **Step 2:** Expected: FAIL.
- [x] **Step 3: Implementar.** Si la tolerancia 0 no se alcanza por redondeo de flotantes, declarar una en la prueba y enseñar las diferencias al dueño.
- [x] **Step 4:** Expected: PASS. **Commit** `feat(extractor): enemigos, fondos y formas desde sus modelos`.

### Task 8: Mundo, escenas y retratos

Comprobada y revisada: cuatro mapas y ocho sprites de retrato exactos. Las 18 sustituciones se listan en `journal/extractor-scenes.md`; los paneles y la aceptación visual siguen en la tarea 10. Se omite `ScenePictures` al no generar escenas de disco ficticias.

**Files:**
- Create: `.../extractor/maps/WorldMap.kt`, `.../extractor/maps/ScenePictures.kt`, `.../extractor/sprites/Portraits.kt`
- Test: `.../extractor/maps/WorldMapParityTests.kt`

**Interfaces:**
- Produces: `fun renderWorldMaps(files): Map<String, Picture>`, claves `world-asuka`, `world-amaterasu`, `-base`; `fun scenePictures(files): Map<String, Picture>`; `fun partnerPortraits(sprites: FieldSprites): Map<String, Picture>` con claves `portraits/<compañero>`.

- [x] **Step 1:** prueba `world maps equal world_map.py's`. Expected: FAIL.
- [x] **Step 2: Implementar** `WorldMap`.
- [x] **Step 3: Investigar** las 18 pantallas, como mucho un día, en `AAA/DAT/SCREEN`. Las que se localicen, con prueba de parecido. Las demás: lista en el ledger y panel propio en la tarea 10. Enseñar al dueño una captura de cada sustitución.
- [x] **Step 4: Retratos:** sprite de campo de cada compañero, sin fondo; el estado lo dibuja la app. Cuando Cargo Tower no tenga `SnnnPACK` (modo 528), generar con sus tablas o añadirla a la lista.
- [x] **Step 5: Commit** `feat(extractor): mapas del mundo, escenas y retratos`.

### Task 9: Orquestar, reanudar y línea de órdenes

Comprobada y revisada: 3.347 archivos/3.409 claves en 28,77 s (Mac); reanudación 0,43 s sin reescribir recursos. Implementación secuencial, sin parámetro `threads` sin efecto. `AssetSink.pictureExtension` declara el formato real del manifiesto.

**Files:**
- Create: `.../extractor/Extractor.kt`, `.../extractor/AssetSink.kt`, `.../extractor/PngSink.kt`, `.../extractor/Main.kt`
- Modify: `Android/extractor/build.gradle.kts` (`application`), `.gitignore` (`Frontend/game-dev/`)
- Test: `.../extractor/ExtractorTests.kt`

**Interfaces:**
- Produces:
  - `interface AssetSink { fun has(path: String): Boolean; fun picture(path: String, picture: Picture); fun text(path: String, content: String); fun commit(manifest: Manifest) }`
  - `@Serializable data class Manifest(val version: Int, val assets: Map<String, String>)`: clave sin extensión → ruta real; `const val FORMAT_VERSION = 1`
  - `enum class Piece { Text, Cards, Sprites, Icons, World, Maps, Models }`
  - `class Extractor(disc, sink, threads: Int = 2) { fun run(pause: () -> Unit = {}, progress: (Piece, Int, Int) -> Unit); val pieces }`

- [x] **Step 1: Pruebas que fallan.** Con piezas falsas: `a run that fails halfway keeps what it wrote and the next run only does the rest`; `nothing is committed until all pieces finished`; `a picture half written is not taken as done` (el sink escribe a temporal y renombra); `pause is called between items`; `progress reaches the total of each piece`.
- [x] **Step 2:** Expected: FAIL.
- [x] **Step 3: Implementar.**
- [x] **Step 4:** `./gradlew :extractor:run --args="$DW3_ISO ../Frontend/game-dev"`. Expected: manifiesto completo. Anotar tiempo en el Mac.
- [x] **Step 5: Commit** `feat(extractor): extracción completa y reanudable`.

## Hito 3: la interfaz lee del manifiesto

### Task 10: Imágenes

Revisión visual (2026-11-05): el dueño eligió los píxeles originales de los retratos con sombreado interno moderado, oscureciendo sus zonas oscuras y conservando las luces; no se añade sombra exterior. Se ha comprobado el ajuste en los ocho compañeros. Para las evoluciones eligió el giro de tres cuartos al lado contrario (+25°) y el acabado «Cine equilibrado» de la columna central, con luz cálida y borde frío. Ambos acabados están implementados y revisados en Kotlin. Los ocho retratos coinciden exactamente con las muestras; las 44 evoluciones conservan alfa exacto y tienen diferencia máxima de 1 nivel RGB frente a Python. El camino bruto y sus 244 referencias siguen exactos. Ya se han capturado las pantallas de la app en navegador; la prueba en la Thor sigue pendiente. Suite actual del extractor: 92 pruebas pasadas, una de BIN saltada por haberse retirado esa copia regenerable para liberar espacio.

Integración local: servidor privado Android/Vite, espera al manifiesto y consumidores de imágenes implementados y revisados. Suite handheld 48/48, compilación web/Android y lint correctos. APK provisional 6.203.745 bytes; aún contiene textos del juego y no se ha instalado. Quedan 18 escenas especiales y la elección exacta de mapas por capítulo; se devuelve null en los casos sin correspondencia segura, por lo que la tarea 10 sigue abierta. Evidencia y límites en el diario del extractor.

**Files:**
- Create: `Frontend/src/handheld/game-assets.ts`, `Frontend/src/handheld/__tests__/game-assets.test.ts`
- Modify: `Frontend/src/catalogs/image.catalog.ts`, `Frontend/src/handheld/assets.ts`, `Frontend/src/handheld/main.ts`, `vite.handheld.config.ts`, `Android/app/.../overlay/AppWebViewClient.kt`

**Interfaces:**
- Produces: `async function loadGameAssets(): Promise<AssetState>` con `type AssetState = "ready" | "missing" | "outdated"`; `function gameAssetUrl(kind: string, name: string): string | null`; `AppWebViewClient(context, onRenderProcessGone)`, con el `InternalStoragePathHandler` estándar para `/game/` sobre `filesDir/game/1`.

Quedan en el paquete solo `handheld/assets/{icons,ui,fonts}`. `npcs/Chris.png` y los retratos pasan a `/game/`.

- [x] **Step 1: Pruebas que fallan.** `a known asset gives its url`; `an unknown asset is null`; `a manifest of another version is outdated`; `no manifest is missing`.
- [x] **Step 2:** `npm run test:handheld`. Expected: FAIL. Módulo ausente en el primer intento; unidad del lector terminada y revisada con 9 pruebas verdes. Suite handheld completa: 25/25. Falta conectar el lector a la interfaz y al servidor Android; los pasos siguientes y la aceptación visual siguen abiertos.
- [ ] **Step 3: Implementar** y sustituir los `import.meta.glob`. Vite sirve `Frontend/game-dev` como `/game` en desarrollo. Las pantallas que pasan a modelos 3D o a retratos nuevos: recorrerlas y ajustar tamaños sin cambiar el diseño.
- [x] **Step 4:** comprobar en navegador Equipo, Diario, Mapa, Combate, Evol. y Wiki, con captura para el dueño de evoluciones y retratos nuevos. Consola sin errores. Recorrido real a 620 × 540 y modelos completos sin recorte; no sustituye la prueba posterior en la Thor ni completa la cobertura de mapas.
- [x] **Step 5: Commit** `feat(handheld): cargar las imágenes del juego desde el manifiesto`. Dividido en unidades locales de servidor/arranque, catálogo y consumidores con sus pruebas y revisiones; nada subido.

### Task 11: Textos y tablas sin nombres

**Files:**
- Modify: `Frontend/src/handheld/language.ts`, `Frontend/src/i18n/index.ts`, `Frontend/src/i18n/locales/*/index.ts`, `handheld-locale.ts`, `Tools/RomExtract/field_actors.py`, `Tools/FieldData/generate_{techniques,exits,seabed}.py`, `Tools/ServerCopies/generate_server_copies.py`; regenerar `field-actors.ts`, `game-techniques.ts`, `map-exits.ts`, `seabed-docks.ts`, `server-copies.ts`
- Delete: `locales/*/game.generated.json`, `words.generated.json`, carpeta `pt-BR`
- Test: `Frontend/src/handheld/__tests__/language.test.ts`

**Interfaces:**
- Consumes: JSON de la tarea 6.
- Produces: `gameWord(ref: string): string`, ahora con referencia `"tabla:línea"`. Para las coincidencias internas (`Offers.giver`, `ActorNames`, destino de la Wiki) una clave estable sin texto del juego: el id de personaje.

- [ ] **Step 1: Pruebas que fallan.** `a language brings in the generated part of its messages`; `what a language lacks falls back to English`; `a word with no line gives the reference's own fallback, never crashes`; `the giver of a quest is found by character id`.
- [ ] **Step 2:** Expected: FAIL.
- [ ] **Step 3: Implementar** y separar en `en-US` lo escrito para la app de las líneas del juego, según `en-us-own-texts.txt`. Actualizar `quest-offers.ts` y sus pruebas.
- [ ] **Step 4:** `npm run test:handheld && npx vue-tsc --noEmit -p tsconfig.app.json`. Expected: PASS. Recorrido por idiomas en navegador. **Commit** `feat(handheld): textos del juego generados, sin nombres en el paquete`.

### Task 12: Auditoría

Preparación independiente terminada y revisada (2026-11-05): 15 pruebas sintéticas, APK antiguo rechazado y preservado. Esta tarea sigue abierta hasta generar y comprobar el APK limpio después de las tareas 10 y 11. No se ha auditado todavía el historial del repositorio real.

**Files:**
- Create: `Tools/audit_release.py`, `Tools/audit_samples.txt` (hashes SHA-256 de cadenas de muestra, no las cadenas), `Tools/test_audit_release.py`

- [x] **Step 1:** el script recibe un APK, una carpeta o un repo (`--history`) y falla con: imágenes fuera de `icons`, `ui` e icono de la app; `data:image/png` en JS; hashes de cadenas normalizadas que coincidan con muestras; archivos `.bin` de más de 1 MB, `.iso`, `.chd`. Límites de lectura, sin extracción ni fetch. 15/15 pruebas pasadas por el revisor independiente.
- [x] **Step 2:** probarlo con el APK de `android-thor`. Expected: FALLA con cientos de avisos. Resultado: 1.507 imágenes, 313 PNG incrustados, 269 coincidencias de muestras y 12 límites; SHA-256 idéntico antes y después.
- [ ] **Step 3:** con el APK de esta rama. Expected: PASA, y tamaño anotado. Corregir lo que aparezca.
- [ ] **Step 4: Commit** `feat(tools): auditar que el APK no lleva material del juego`.

## Hito 4: asistente

### Task 13: Extracción en la consola

**Files:**
- Create: `Android/app/.../setup/{GameAssetsStore,WebpSink,ExtractionService,ExtractionState}.kt`, `Android/core/.../SetupFlow.kt`, `Frontend/src/handheld/screens/PrepararScreen.vue`
- Modify: `LauncherActivity.kt`, `LauncherBridge.kt`, `LauncherApp.vue`, `core/LauncherFlow.kt`, `OverlayService.kt`, `AppPanel.vue`, `AndroidManifest.xml`, `i18n` en cinco idiomas
- Test: `Android/core/src/test/.../SetupFlowTest.kt`

**Interfaces:**
- Produces:
  - `enum class AssetState { Ready, Missing, Outdated }` y `class SetupFlow { fun next(assets: AssetState, fileAvailable: Boolean, error: String?, extracting: Boolean, gameAnswering: Boolean): SetupStep }`, con `enum class SetupStep { StartOverlay, AskFile, Extract, PauseExtraction, ShowError }`: lógica pura de qué toca.
  - `class GameAssetsStore(context) { val current: File?; val state: AssetState; fun fresh(): File; fun promote(dir: File); fun clearOld() }`
  - `ExtractionService`: foreground, con `ACTION_START(uri)` y `ACTION_CANCEL`; difunde `ExtractionState(piece, done, total, error)`.
  - `DigiviceNative`: `pickGame()`, `startExtraction()`, `state()`.
  - Códigos de error: `wrong_game`, `not_a_disc`, `truncated`, `no_space`, `file_gone`, `interrupted`.

- [ ] **Step 1: Pruebas que fallan.** `without assets it asks for the file before starting the overlay`; `an outdated manifest regenerates with the saved file`; `a lost file asks again and keeps the assets in use`; `no space is an error that can be retried and clears temporary files`; `extraction pauses while RetroArch answers and resumes when it stops`; `with assets ready nothing is asked`.
- [ ] **Step 2:** `./gradlew :core:test`. Expected: FAIL.
- [ ] **Step 3: Implementar.** SAF con `ACTION_OPEN_DOCUMENT` y `takePersistableUriPermission`, sin ruta de archivo. `WebpSink`: `Bitmap.CompressFormat.WEBP_LOSSY` al 75 para mapas, enemigos y fondos; `WEBP_LOSSLESS` para sprites, cartas e iconos, que son píxel exacto. Espacio libre: al menos 200 MB antes de empezar. Pausa: consultar `RetroArchClient.portState` y `status`.
- [ ] **Step 4:** sin recursos, `OverlayService` no muestra overlay y lo dice en la notificación. En el panel, «Regenerar recursos».
- [ ] **Step 5: Commit** `feat(android): asistente que genera los recursos en la consola`.

### Task 14: Probarlo en la Thor

**Files:**
- Modify: `docs/superpowers/journal/extractor-timing.md`, `Android/README.md`

- [ ] **Step 1:** con el dueño, y sin partida en curso: instalar limpio, elegir el ISO y luego un BIN. Tiempo, temperatura y uso de disco.
- [ ] **Step 2:** interrumpir: forzar cierre a mitad, sin espacio simulado con un archivo grande, archivo equivocado, juego abierto a mitad.
- [ ] **Step 3:** jugar con los recursos generados y comparar capturas con `android-thor` en las mismas pantallas.
- [ ] **Step 4:** anotar y corregir. **Commit** `docs: extracción probada en la consola`.

## Hito 5: CHD

### Task 15: CHD

**Files:**
- Create: `Android/app/src/main/cpp/{CMakeLists.txt,chd_jni.cpp}`, `Android/app/.../setup/ChdSource.kt`, submódulo fijado de `libchdr` (BSD-3) y avisos de licencia
- Modify: `DocumentSource.kt`, `ExtractionService.kt`, `Android/app/build.gradle.kts` y las cadenas del asistente
- Test: selección y extracción completas en consola, y prueba de paridad del JNI con el BIN en el Mac

**Interfaces:**
- El lector nativo recibe el descriptor y expone los primeros 2.352 bytes de cada unidad CHD de 2.448 como `ByteSource`. `openDisc` y `checkGame` reutilizan el mismo lector de sectores y la misma validación de BIN. El módulo `:extractor` sigue sin NDK.

- [x] **Step 1:** muestra CHD original de la Thor verificada por SHA-256 y guardada fuera del repositorio.
- [x] **Step 2: Prueba:** 1.000 sectores crudos repartidos y una lectura entre dos bloques coinciden con el BIN de referencia. Una cabecera truncada se rechaza.
- [x] **Step 3: Implementar.** Solo `arm64-v8a`.
- [x] **Step 4:** el asistente terminó en la Thor con el CHD original de la carpeta de RetroArch. Generó 3.411 recursos en unos 45 segundos. Los 3.350 archivos coinciden por SHA-256 con la extracción anterior desde ISO en la misma consola. Al volver a abrir Digivice apareció el selector de conexión. `audit_release.py` conserva las incidencias de publicación anteriores. Véase el [diario del extractor](../journal/extractor.md#direct-chd-preparation).

## Hito 6: publicación

### Task 16: Repositorio público

Jean Carlos Heck autorizó por correo el 2026-11-04 publicar la versión Android con lo que haga falta de Digivice, usar ese nombre si se quiere y añadir un Ko-fi propio. Pidió crédito para Digivice y para las fuentes de «Reference Data Credits» de su README (GameFAQs y demás). Aún no ha elegido licencia: **sin ella no se pone un `LICENSE` sobre el código heredado**; se pregunta al dueño. Su permiso no cubre el material de Bandai Namco, así que el resto del plan no cambia. Ningún push sin confirmación del dueño.

**Files:**
- Create en la copia limpia: `README.md`, `NOTICE`, `.github/FUNDING.yml`, y `LICENSE` cuando Jean elija la suya
- Delete en la copia limpia: `Backend/` salvo `Memory/Definitions`, `Tauri/`, `Tests/`, `Digivice.slnx`, `build_app.ps1`, vistas de Windows en `Frontend/src`, imágenes, snapshots, `Design/{cards,sprites,portraits,screens}`, scripts Python que el extractor sustituye

- [ ] **Step 1:** copia en carpeta aparte con `git filter-repo`: quitar esas rutas de **todo** el historial y cambiar el correo con `--mailmap` a la dirección `noreply` del dueño. No tocar el repositorio original.
- [ ] **Step 2:** hacer que `TestEnvironment.snapshot` y las pruebas que lo usan se salten con aviso sin `Tools/MemoryScanner/Snapshots`, sacando de ahí los volcados. Comprobar que la app compila y las pruebas pasan en la copia. Revisar los golden JSON con la auditoría.
- [ ] **Step 3:** `python3 Tools/audit_release.py --history .`. Expected: PASA.
- [ ] **Step 4:** README (qué es, basado en Digivice de Jean Carlos Heck, requisitos, asistente, limitaciones, compilación, avisos legales), `NOTICE` (Jean y su proyecto con enlace y Ko-fi, las fuentes de sus «Reference Data Credits», dw3_decomp con su MIT, ddw3, libchdr, Press Start 2P), firma propia guardada fuera del repositorio.
- [ ] **Step 5:** enseñar al dueño el resultado, y con su confirmación crear el repositorio y subir. Release con el APK auditado.
