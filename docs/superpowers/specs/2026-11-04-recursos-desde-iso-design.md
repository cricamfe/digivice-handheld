# Recursos del juego generados desde la copia de cada usuario

Documento de diseño. Fecha: 2026-11-04. Estado: decisiones del dueño recogidas; plan en `docs/superpowers/plans/2026-11-04-recursos-desde-iso.md`.

## Qué es y por qué

La app lleva hoy dentro imágenes y textos de Digimon World 2003: 1.402 imágenes como archivos, más otras incrustadas en el JavaScript, y los textos de cinco idiomas. Mientras sea para la consola del dueño no importa, pero publicar ese APK es distribuir material del juego, y eso puede traerle reclamaciones.

La idea: el APK público no lleva material del juego. Al abrirla por primera vez, la app pide el archivo del juego del usuario y genera los recursos en la propia consola, en su almacenamiento privado. Desde entonces funciona como ahora.

Esto reduce el riesgo, no lo elimina. Aparte está el código heredado de Digivice, que aún no tiene licencia: su autor, Jean Carlos Heck, autorizó por correo el 2026-11-04 publicar la versión Android, y dijo que elegirá una licencia. Su permiso cubre lo suyo, no el material de Bandai Namco. Nada de esto es asesoramiento jurídico.

## Objetivos

1. El APK público y el repositorio público no contienen gráficos ni textos del juego. Se comprueba con un script (ver Pruebas).
2. Con una copia válida, el usuario llega a la app funcionando sin ordenador y sin saber qué es un TIM: elegir archivo, esperar, listo.
3. El resultado se ve como hoy. Los recursos que hoy salen de la ROM salen idénticos. Los que no están en el disco se sustituyen sin perder función.
4. Admite los formatos habituales en una portátil: ISO, BIN y CHD.
5. La extracción es **una sola implementación**, usada en la consola y en el ordenador del desarrollador.

## Lo que no debe pasar

- Modificar el archivo del usuario, subirlo o copiarlo fuera de la app.
- Descargar recursos de ningún sitio.
- Dejar la consola inutilizable o a medias: una extracción interrumpida se reanuda o se repite, y nunca deja recursos parciales en uso.
- Empeorar el juego: la extracción no corre mientras se juega (ver Asistente).
- Dejar material del juego en el historial del repositorio público.

## Fuera de alcance

- Otras versiones del juego. Solo `SLES-03936`, porque todas las tablas y direcciones son de esa versión.
- Formatos PBP, 7z, ZIP o similares.
- Audio y vídeos del juego.

## Inventario

Evidencia y detalle: `docs/superpowers/journal/asset-inventory.md`. Comprobado sobre `136d81fb`.

| Recurso | Cantidad | Origen | Qué se hace |
|---|---|---|---|
| Mapas | 236 + 2 mundo | Disco. Regenerados idénticos (S331, S206) | Extraer |
| Enemigos | 172 | Disco, modelos 3D. Idénticos en 2 de muestra | Extraer |
| Fondos de combate | 28 | Disco, 3D | Extraer |
| Personajes del campo | 2.472 | Disco | Extraer |
| Iconos de objetos | 24 | Disco | Extraer |
| Arte de cartas | 314 | Disco: `CARDPAK0-4.BIN`, 32×32. Coincide salvo redondeo | Extraer |
| Marco y cifras de cartas | 6 colores y dígitos | Sin localizar en disco | Leer de `STCDSPTM` o dibujar uno propio (ver Decisiones) |
| Tamers, NPC, jefes, tiendas, Duel Island | unos 40 | Sprites de campo; 5 tamers sin coincidencia exacta (Bob, Genji, Natsumi, Tomomi, Wong) | Extraer con una tabla nombre → personaje; esos 5, otro fotograma del mismo personaje |
| Ilustraciones de evoluciones | 44, 10 MB | **No están en el disco**, vienen del Digivice original | Sustituir por el modelo 3D de cada forma |
| Retratos del equipo | 8 × 4 estados | Del Digivice original; origen en disco sin localizar | Sustituir (ver Decisiones) |
| Pantallas de escena | unas 18 | Del Digivice original; probablemente `AAA/DAT/SCREEN`, sin comprobar | Investigar; si no, un panel propio |
| Textos del juego en 5 idiomas | 39 tablas por idioma | Disco. Decodificador probado: 10.833 líneas por idioma, 1 diferencia | Generar |
| Iconos, interfaz, fuente | unos 130 | Propios y Press Start 2P (OFL) | Se quedan en el APK |
| Misiones e interfaz traducidas | — | Escritas para la app | Se quedan |
| Números y direcciones (`database`, `Definitions`, tablas `.ts`) | — | Hechos | Se quedan, sin cadenas de texto del juego |

No entran en el paquete de la consola y no hacen falta: `assets/battle`, `assets/dri-agents-enemies`.

## Cómo funciona

```
archivo del usuario (ISO, BIN, CHD)
        │  lector de disco
        ▼
Android/extractor (Kotlin puro, sin Android) ──► RAM de píxeles y JSON
        │                                         │
        │ consola: Bitmap → WebP                  │ ordenador: PNG
        ▼                                         ▼
filesDir/game/<versión>/   + manifest.json     Frontend/game-dev/ (ignorada por git)
        │
        ▼
WebView: https://appassets.androidplatform.net/game/...
```

### Extractor

Un módulo Kotlin nuevo, `Android/extractor`, sin dependencias de Android, igual que `core`. Sustituye a los generadores de imágenes de `Tools/RomExtract`, unas 1.900 líneas de Python, y tiene dos entradas: la app y una línea de órdenes para el desarrollador. Cuando alcance la paridad, los scripts de Python equivalentes se retiran; los que analizan el código de la descompilación (`field_actors.py`, `generate_*.py`) se quedan, porque generan hechos.

Devuelve píxeles en crudo; quien lo llama decide cómo guardarlos. Se trabaja por piezas, cada una reanudable: discos, texto, cartas, sprites, iconos, mundo, mapas, modelos 3D.

Lo que hoy se lee de `dw3_decomp` o `ddw3` en tiempo de generación pasa a tablas de datos dentro del APK, generadas por los scripts de `Tools/`:

- Tabla de caracteres (358 entradas).
- Clave de la app → (tabla, línea), para los textos.
- Tabla de escenarios para pintar mapas (1,9 MB de JSON, 106 KB comprimido).
- Personaje → paquete de sprites, nombre → personaje, puntos del mapa del mundo.

`FILE_SECTORS` no hace falta llevarlo: está en el ejecutable del disco, en `SLES_039.36` + 0x3576C.

### Lectura del disco

Interfaz común: «dame N sectores de 2.048 bytes desde el sector L».

- **ISO:** `L × 2048`.
- **BIN:** `L × 2352 + 24`. El usuario elige el `.bin`; el selector de Android no da acceso al `.cue` y tampoco hace falta, porque el juego es de una sola pista. El formato se detecta por el tamaño y los bytes de sincronía, no por la extensión.
- **CHD:** `libchdr` (BSD) por NDK, abierto con el descriptor del archivo. Es la única parte nativa, y la única que no se ha podido probar aún: no hay ninguna muestra a mano.

**Validación:** `SYSTEM.CNF` debe citar `SLES_039.36` y el SHA-256 del ejecutable debe coincidir. Si no, un mensaje claro: «Este archivo no es la versión europea de Digimon World 2003».

### Almacenamiento y versiones

`filesDir/game/<versión de formato>/`, con `manifest.json` (versión, lista, tamaños). Se escribe en una carpeta temporal y se renombra al terminar; solo con manifiesto se usa. Espacio previsto: unos 70 MB, que se comprueban antes de empezar. No se conserva ninguna copia del disco.

Una actualización de la app que cambie el formato sube la versión: la app pide regenerar, o lo hace sola si conserva permiso sobre el archivo (`takePersistableUriPermission`), y borra la anterior.

### Asistente

En la pantalla superior, dentro del flujo actual de `LauncherActivity`, tras el permiso de superposición y con el mismo estilo:

1. Explica en dos frases qué va a pasar y que nada sale de la consola.
2. Selector de Android: elegir el archivo.
3. Valida. Si falla, explica por qué y deja elegir otro.
4. Extrae con barra de progreso por piezas, en un servicio en primer plano, para sobrevivir a que el usuario cambie de app. Reanuda por piezas si se interrumpe.
5. Termina y continúa como hoy.

Con pocos hilos (dos como máximo), para no disparar el ventilador. No se extrae con RetroArch respondiendo: si el usuario entra en el juego, se pausa y retoma después. Sin recursos, el overlay no se muestra; el servicio avisa en la notificación de que falta prepararlos.

Desde el panel de pulsación larga, una opción «Regenerar recursos».

### Interfaz

- **Imágenes:** los `import.meta.glob` de `catalogs/image.catalog.ts` y `handheld/assets.ts` se sustituyen por `gameAssetUrl(tipo, nombre)`, que consulta el manifiesto. Lo que no exista devuelve null, y las pantallas ya tratan ese caso con su icono de desconocido.
- **Textos:** `language.ts` añade la parte generada de cada idioma, leída de `game/text/<idioma>.json`, incluida la de inglés, que deja de estar en el paquete. Las claves de la app no cambian.
- **Tablas `.ts`:** `field-actors`, `map-exits`, `game-techniques`, `seabed-docks` y `server-copies` guardan una referencia (tabla, línea) en lugar del nombre, y `gameWord` la resuelve.
- **`database/*.json`:** los campos `name` e `imageName` se quedan como identificadores internos que no se muestran. Es decisión mía: son nombres cortos y cambiarlos tocaría mucho código compartido.
- **Desarrollo:** la línea de órdenes del extractor llena `Frontend/game-dev/`, ignorada por git, que Vite sirve como `/game`. El simulador `?mock=` sigue funcionando para quien tenga su copia.

## Decisiones tomadas por mí

- **Kotlin, no Python en la consola:** una sola implementación, con pruebas en la JVM.
- **WebP con pérdida al 75 en consola:** como hoy. Con PNG, los mapas pasan de 31 a 184 MB.
- **Evoluciones:** el modelo 3D de cada forma, igual que ya se hace con los enemigos. Cambia de ilustración a render de PS1, en la línea del resto de la app.
- **Retratos del equipo:** el sprite de campo de cada compañero sobre el fondo de estado actual, dibujado por la app.
- **Marco y cifras de cartas:** primero intentar `STCDSPTM`. Si cuesta más de un día, un marco propio de seis colores y una tipografía de cifras propia.
- **Portugués:** sale del paquete y del repositorio público. El juego no tiene portugués, así que sus nombres no se pueden generar, y el selector ya no lo ofrece. Si algún día se añade, será con los nombres del juego en inglés.
- **Textos en inglés que no coinciden con ninguna línea del disco:** unos 110 (equipo 9, técnicas 13, cartas 67, mapas 23). Si son erratas, con una similitud de al menos 0,88 con una línea, se usa la línea del disco. Los demás son textos de la app, se quedan en `en-US` y entran en la lista permitida de la auditoría, revisados uno a uno. Las claves no cambian.
- **Criterio del primer hito:** la extracción completa, estimada a partir de los mapas medidos, debe tardar 10 minutos o menos en la Thor, sin quedarse sin memoria y sin que la CPU pase de 70 °C. Si tarda más, los modelos 3D se generan bajo demanda al abrir la pantalla que los usa, y el asistente solo prepara el resto.
- **Tests con `Tools/MemoryScanner/Snapshots` y `Tools/RetroArchProbe/thor-title.bin`:** son volcados de RAM del juego y no pueden ir al repositorio público. Pasan a una carpeta local; las pruebas que los usan se saltan si no están, con un aviso visible. Los golden JSON derivados se quedan si no contienen texto del juego, y se revisan.

## Qué contiene el repositorio público

Decidido por el dueño el 2026-11-04: **repositorio nuevo solo con la app Android.** Conserva `Android/`, `Frontend/src/handheld`, los presentadores y datos que usa, `Backend/Memory/Definitions` y las herramientas de extracción. La app de Windows sigue en el proyecto de Jean, y se quitan sus vistas, sus imágenes, `Backend` (salvo las definiciones), `Tauri` y `Tests` de C#. Los commits y créditos de Jean se conservan en lo que queda. La rama local `android-thor` no se toca hasta el hito de publicación.

## Riesgos

| Riesgo | Qué lo revela | Respuesta |
|---|---|---|
| Tarda demasiado en la consola. En el Mac, 90 s con numpy; en la Thor es desconocido | Primer hito | Bajar resolución de los 3D o generarlos bajo demanda |
| El rasterizador 3D es lo más difícil de portar (630 líneas de numpy) | Paridad con los PNG actuales | Tolerancia de píxeles declarada; se muestran diferencias al dueño |
| CHD sin muestra | Hito de CHD | Pedir una al dueño; si falla, publicar con ISO y BIN y explicarlo |
| Android mata el servicio o llena la memoria con los mapas grandes (hasta 1792×1536) | Pruebas en la Thor | Un mapa cada vez; reanudar por piezas |
| El autor no da permiso | Respuesta de Jean | Se publica solo lo propio o nada; el extractor sigue sirviendo al dueño |

## Pruebas

- **Paridad, en local:** con la ISO del desarrollador, la salida del extractor se compara píxel a píxel con la de Python. Idénticos: mapas, sprites, iconos, enemigos y fondos, sin WebP de por medio. Arte de cartas: dentro del redondeo. Texto: igual que las tablas decodificadas.
- **Sin disco, en el repositorio:** lectores de ISO y BIN y decodificadores con datos sintéticos propios, generados en la prueba.
- **Auditoría del APK:** un script lista lo que lleva el APK y falla si hay imágenes fuera de una lista permitida (`icons`, `ui`, icono de la app), PNG en base64 en el JavaScript, o cadenas de una lista de muestras del juego. Se ejecuta también sobre el repositorio y su historial antes de publicar.
- **En la consola:** ISO y BIN reales, interrumpir la extracción y reanudar, poco espacio, archivo equivocado, y medir tiempo y temperatura.

## Hitos

1. **Pinchazo de mapas en la Thor.** Disco y un mapa en Kotlin, medidos en la consola. Decide si seguir, antes de portar lo demás.
2. **Extractor completo** con línea de órdenes y paridad.
3. **La interfaz lee del manifiesto.** APK sin material, auditoría en verde; la app funciona con recursos colocados a mano.
4. **Asistente,** con ISO y BIN.
5. **CHD.**
6. **Publicación.** Con permiso de Jean: limpieza del historial, cambio del correo de los commits, créditos, licencia, README y firma propia.

## Lo que queda a quien lo construya

Estructura interna de `Android/extractor`, formato exacto del manifiesto, y textos del asistente, siempre que cumplan lo anterior y estén en los cinco idiomas.
