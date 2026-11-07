# Digivice para la pantalla inferior de la AYN Thor

Documento de diseño. Fecha: 2026-11-03. Estado: aprobado por el dueño el 2026-11-03.

## Qué es y por qué

Digivice es una «segunda pantalla» para Digimon World 2003: muestra el estado vivo de la partida, el diario, los árboles de digievolución, los enemigos de cada mapa y las ayudas para Seabed y Mobius. Hoy solo funciona en Windows con DuckStation.

Esta app lleva Digivice a la pantalla inferior de la AYN Thor, como **app Android propia**, mientras el juego corre arriba en RetroArch con SwanStation. El dueño del fork la quiere propia, y no como tema de EmuLnk, porque busca un resultado más profesional y con la estética del juego.

Es un proyecto personal para su propia consola. Otras portátiles de doble pantalla son un objetivo secundario.

## Objetivos

1. Con el juego abierto arriba, abajo se ve Digivice con datos vivos, sin tocar nada más que abrir la app.
2. Las pantallas reproducen el diseño aprobado, sin parecer una web.
3. Tocar la pantalla inferior no pausa el juego ni quita el mando al juego.
4. Cubre las mismas funciones que Digivice 4.0 en Windows: equipo, mapa, batalla, diario, misiones, wiki, Seabed, Mobius, evoluciones, técnicas y subastas.

## Fuera de alcance

- DuckStation, otros juegos u otras versiones del juego. Solo la PAL `SLES-03936`.
- Ampliar lo que Digivice cubre del juego. Hoy llega hasta el sector oeste de Asuka Server.
- Publicar la app. Digivice no tiene licencia y la app incluye sprites del juego; publicar exige permiso de upstream y revisar esos recursos.
- Sustituir la versión de Windows.
- Opciones de interfaz en la primera versión. Queda para más adelante un selector de estilo de retrato; el estilo `degradado` ya está generado en `Design/portraits/degradado`.

## Lo que no puede pasar

- Que el juego se ralentice, se pause o pierda el mando por culpa de la app.
- Que la app escriba en la memoria del juego. Es de solo lectura: nunca envía `WRITE_CORE_MEMORY`, `WRITE_CORE_RAM`, `LOAD_CORE`, `SET_INPUT` ni similares.
- Que muestre datos erróneos sin avisar, por ejemplo con otro juego cargado.
- Que parezca una web o algo generado por IA: cajas translúcidas, bordes finos, flechas CSS, emoji o iconos genéricos.
- Que exija root o cambie ajustes de seguridad sin que el usuario lo decida.

## Restricciones comprobadas

Todo esto se midió el 2026-11-03 en la Thor del dueño (Android 13), salvo donde se indica.

### Acceso a la RAM

- RetroArch estable para Android (hasta 1.22.2) **no sirve**: se compila sin `HAVE_COMMAND` y su binario no contiene `READ_CORE_MEMORY`.
- Sirve la nightly con la compilación del 2026-11-03, ya instalada como `com.retroarch.aarch64` (`versionCode` 1793667751). Los comandos llegaron a Android el 2026-03-25, y la lectura de núcleos sin mapa de memoria el 2026-10-25.
- SwanStation no publica mapa de memoria. La nightly expone su RAM en `0` (2 MiB) y la tarjeta de memoria en `0x200000` (128 KB).
- Protocolo: UDP en `127.0.0.1:55355`. La petición es `READ_CORE_MEMORY <hex> <n>`; la respuesta es `READ_CORE_MEMORY <hex minúsculas> AA BB ...` o `... -1 <motivo>`. Máximo 16384 bytes por petición.
- Las direcciones de `Backend/Memory/Definitions` valen tal cual: la partida real se decodificó bien en la Thor.
- Cada petición tarda una trama del juego, unos 20 ms en PAL, **sea cual sea su tamaño**. Medido por Wi-Fi, no dentro de la consola.
- `GET_STATUS` responde `CONTENTLESS` o `PLAYING playstation,<nombre del archivo>`. No hay que fiarse de ese nombre para identificar el juego.
- El serial no sirve de firma: su posición en RAM cambia con el estado de la partida. Sí sirve el código del juego: `0x10105` a `0x3BAE3` coincide en los 18 snapshots y en la RAM de la Thor en pantalla de título.

### Configuración de RetroArch ya aplicada

- `network_cmd_enable = "true"`, `network_cmd_port = "55355"` y `network_cmd_bind_address = "127.0.0.1"`.
- `pause_nonactive = "true"` sigue activo: si la ventana del juego pierde el foco, se pausa. Por eso importa el objetivo 3.
- La app no puede editar `retroarch.cfg`, porque está en los datos de otra app.

### Pantalla inferior

- Es el display 4 (`Screen-2`), categoría `PRESENTATION`, 1240×1080 en horizontal, con un área de app de 1240×1025 según Android. El diseño usa 620×540 px CSS a densidad 2.
- Ahí corre la app de sistema `rip.moth.cocoonshell` (`ExternalDisplayActivity`), que se debe conservar.
- EmuLnk resuelve el foco con una ventana `TYPE_APPLICATION_OVERLAY` y `FLAG_NOT_FOCUSABLE` sobre ese display. Es una referencia de técnica: su licencia PolyForm Noncommercial impide copiar código sin revisarla. **Esta técnica no se ha probado aún en esta Thor.**

## Cómo se construye

Las decisiones del dueño son: app Android propia, RetroArch con SwanStation, pantalla inferior y el diseño aprobado. Lo demás de esta sección es decisión de quien escribe el documento, y el dueño puede cambiarla.

### Arquitectura

```
RetroArch nightly (arriba)
     ^  UDP 127.0.0.1:55355, solo lectura
     |
App Android, en Kotlin
  Foreground service
    RetroArchClient -> caché de páginas -> núcleo portado de Backend
    (Readers, Loaders, Assemblers, StateComposer, diff de eventos)
     |  mismos DTO JSON que SignalR
     v
  Ventana overlay en el display inferior
    WebView con recursos locales -> Vue 3 + Pinia
    (stores, syncers, presenters, services y database reutilizados)
    (componentes nuevos según el diseño)
```

**Por qué WebView y Vue, y no Compose.** La mitad del frontend (unas 10 000 líneas de 20 000) es lógica y datos ya resueltos: `database`, `services`, `presenters`, `viewmodels` y `stores`. El diseño aprobado también está hecho con CSS: marcos nine-slice en `Design/ui/kit.css`. Reescribir todo en Compose costaría mucho y no aportaría nada visible.

**Por qué portar el backend a Kotlin.** .NET quedó descartado en Android. Kotlin mantiene la lectura fuera del ciclo de vida de la WebView y permite probar el núcleo en la JVM con los snapshots, sin consola. El puente envía los mismos eventos que `Frontend/src/events/events.map.ts`, así que los stores y syncers no cambian.

### Lectura de memoria

El backend lee con `IMemoryAccessor` valor a valor. Por UDP, cada lectura costaría 20 ms, así que:

- `RetroArchClient` implementa esa misma interfaz sobre una **caché de páginas** de 16 KB, renovada una vez por ciclo.
- Las direcciones absolutas de las definiciones caen en tres páginas: `0x40000`, `0x48000` y `0xA4000`. El constructor debe comprobar con los loaders si los offsets relativos añaden alguna. Con tres páginas, cada ciclo cuesta unos 60 ms.
- El ciclo sigue siendo de 500 ms, como `GameLoop:PollingIntervalMs`.
- Las páginas se leen en tramas distintas. Si eso produce estados incoherentes, se acepta hasta el siguiente ciclo, salvo que las pruebas muestren parpadeos visibles.
- UDP puede perder paquetes: una respuesta perdida se reintenta una vez y, si falla, el ciclo se salta conservando el último estado.

### Estados de conexión

Se reutilizan `HealthDTO` y `emulator-connection-error-codes.ts`, con estos casos:

| Situación | Cómo se detecta | Qué ve el usuario |
|---|---|---|
| RetroArch cerrado o comandos desactivados | Sin respuesta a `VERSION` | Pantalla de espera con pasos para activarlos |
| Sin juego | `GET_STATUS CONTENTLESS` | Espera discreta |
| Nightly antigua o estable | `-1 no memory map defined` con juego cargado | Aviso con la versión necesaria |
| Otro juego | Falla la firma de código | Aviso: solo funciona con `SLES-03936` |
| Pantalla de título, sin partida | Firma correcta, equipo vacío | Espera discreta |
| Partida | Firma correcta y datos | Digivice |

La firma es un hash de un tramo de `0x10105-0x3BAE3`. Se comprueba al conectar y al cambiar el nombre del juego en `GET_STATUS`.

Un mapa que no esté en la tabla, como `0x279` en la partida de prueba, se muestra como «mapa desconocido» sin bloquear el resto.

### Ventana e interacción

- `Foreground service` con overlay `TYPE_APPLICATION_OVERLAY` sobre el display `PRESENTATION`, sin foco y con toques activos.
- Requiere el permiso «Mostrar sobre otras apps», pedido con una pantalla inicial de la app.
- Si no hay segunda pantalla, la app se abre como actividad normal a pantalla completa. Sirve para desarrollar, no es una función cuidada.
- **Abrir:** el usuario abre la app una vez desde el lanzador, idealmente antes que RetroArch. La actividad arranca el servicio y se cierra. Si se abre con el juego en marcha, debe hacerlo sin pausarlo, y la prueba del paso 1 lo confirma. No arranca sola con el sistema.
- **Mostrar y ocultar:** el overlay aparece solo cuando RetroArch responde y se retira cuando deja de responder durante unos segundos, para devolver la pantalla a `cocoonshell`. La pantalla de espera solo se ve mientras RetroArch está abierto sin el juego correcto.
- **Cerrar (decisión del dueño):** mantener pulsado un botón de la barra inferior durante un segundo abre un panel en pixel art con «Ocultar» y «Salir». Ocultar devuelve la pantalla a `cocoonshell`; Salir cierra la app. La notificación del servicio conserva esas acciones y permite volver a mostrar Digivice. La pantalla inicial explica el gesto.
- Solo táctil. El mando pertenece al juego. Dianas de al menos 44 px CSS, como en el diseño.
- El teclado virtual no funciona en una ventana sin foco. La búsqueda de la wiki usa el teclado propio en pixel art de `WikiBuscar.dc.html`, que no necesita foco.

### Interfaz

Fuente de verdad: una maqueta privada, «Digivice reimaginado». Copia de las pantallas en `Design/screens`; sus imágenes apuntan a `/_blob/...`, y sus originales están en `Design/ui` y `Design/icons`.

| Pantalla | Archivo |
|---|---|
| Equipo | `Digimon.dc.html` |
| Mapa actual | `MapaActual.dc.html` |
| Batalla | `Batalla.dc.html` |
| Diario y misión | `Diario.dc.html`, `Mision.dc.html` |
| Wiki | `WikiIndice.dc.html`, `WikiEnemigo.dc.html` |
| Seabed | `Seabed.dc.html`, `SeabedRuta.dc.html` |
| Mobius | `Mobius.dc.html` |
| Evoluciones y técnicas | `Evoluciones.dc.html`, `Tecnicas.dc.html` |
| Subastas | `Subastas.dc.html` |
| Batalla de cartas, en el sitio de Mapa | `BatallaCartas.dc.html` |
| Wiki: listado y búsqueda | `WikiLista.dc.html`, `WikiBuscar.dc.html` |
| Wiki: cartas, sobres y tienda | `WikiCarta.dc.html`, `WikiSobre.dc.html`, `WikiTienda.dc.html` |
| Wiki: equipo, objeto y NPC | `WikiEquipo.dc.html`, `WikiObjeto.dc.html`, `WikiNpc.dc.html` |
| Permiso inicial y estados de conexión | `Espera.dc.html` |
| Catálogo de iconos | `Iconos.dc.html` |

Notas sobre estas pantallas:

- `Lugares` de la wiki abre la pantalla de mapa, que ya muestra lugar y encuentros.
- `Subastas` muestra el caso más completo: subasta activa sin Bits suficientes. Sin subasta activa, el panel superior usa el texto de `Diario`.
- Una carta de acción muestra su nota en la ventana de datos de `WikiCarta`, en lugar de AP y HP.
- Una batalla de NPC bloqueada muestra qué falta para desbloquearla, no su contenido.
- Las pestañas inferiores de `Espera` solo sirven para revisar los estados: no van en la app.
- Sin botón de Ko-fi ni selector de idioma. El crédito a jeanheck va en `Espera`.

`Main` y `Mapa` son la primera ronda descartada. Reglas visuales que no se pueden perder:

- Press Start 2P solo a 10, 12 o 16 px, sin mayúsculas acentuadas. El tamaño pequeño era de 8 px: lo subió a 10 px el dueño, por decisión propia, al ver el texto en la Thor. Con 10 px lo que no cabe se reorganiza, se envuelve o se da más espacio; el texto no baja de 10 px.
- Iconos a 22 o 44 px, con `image-rendering: pixelated`. Única excepción: el icono protagonista de `WikiObjeto`, a 88 px (cuatro veces su tamaño original).
- Marcos nine-slice con `stretch`, sin `fill`, sobre fondo liso.
- Texto e iconos nunca pegados al marco: el margen interior va en `Design/ui/kit.css`, y una excepción solo se justifica en contenido que llena el marco, como la rejilla de Mobius.
- Sprites reales del juego en lugar de retratos dibujados.
- Retratos del equipo a 64 px, con los de `Design/portraits/rejilla`: sprite de `Design/sprites` sobre una rejilla del Mundo Digital. Los generan `Design/build_sprites.py` y `Design/build_portraits.py`. Sin base ni sombra en el suelo.
- El color de la rejilla indica el estado, igual que el fondo original de Digivice: bien, herido, alterado o KO.
- Donde el sprite va a 32 px, como las pestañas de Evoluciones y la DVEXP de `WikiEnemigo`, se usa solo, porque la rejilla no se lee a ese tamaño.
- Las cartas con puntos usan `Design/cards`, generadas por `Design/build_cards.py`: muestran `4/4` en lugar de `4/ 4`.
- Cuando falta un dato, se muestra el icono de interrogante (`Design/icons/unknown.svg`) con texto, nunca un hueco.

Los datos de ejemplo del diseño se sustituyen por los reales. Si una pantalla necesita un dato que Digivice no da, se avisa antes de inventarlo.

**Idioma.** La interfaz va en español, con un locale `es-ES` nuevo. Digivice solo trae `en-US` y `pt-BR`. Nombres y descripciones del juego quedan en inglés, como en el diseño.

### Repositorio

- `Android/`: proyecto Gradle nuevo.
- `Frontend/`: entrada nueva `handheld` (por ejemplo `Frontend/src/handheld/`) con los componentes del diseño. Reutiliza el resto. El cliente SignalR queda detrás de un transporte intercambiable.
- `Backend/`, `Tauri/` y la versión de Windows siguen funcionando sin cambios, para poder traer mejoras de upstream.
- `Backend/Memory/Definitions` sigue siendo la única fuente de direcciones. Kotlin las carga desde esos JSON, sin copiarlas.
- `EmuLnk/` es una prueba anterior descartada. No se usa ni se borra en este proyecto.

### Pruebas

- Núcleo en JVM: decodificar los 18 snapshots de `Tools/MemoryScanner/Snapshots`, y portar los casos relevantes de `Tests/`, sobre todo loaders y diff de eventos.
- Equivalencia con C#: comparar los DTO de Kotlin con los del backend original para los mismos snapshots. Este Mac no tiene `dotnet`, así que hay que instalarlo o generar los archivos de referencia en otra máquina.
- Cliente UDP: contra `Tools/RetroArchProbe/fake_retroarch.py`, incluidos los fallos.
- Frontend: Digivice no tiene pruebas de frontend por norma del proyecto. Las pantallas se revisan renderizadas, en la Thor.

## Orden de construcción

1. **Prueba de doble pantalla, que puede cambiar el diseño.** App mínima con overlay en el display 4, con WebView y una pantalla real. Hay que confirmar en la Thor:
   - Tocar abajo no pausa el juego ni le quita el mando.
   - El mando sigue funcionando en SwanStation.
   - Abrir la app con el juego en marcha no lo pausa.
   - El overlay tapa bien `cocoonshell`, se retira al cerrar RetroArch y se recupera al apagar y encender la pantalla.
   - El texto pequeño se lee bien (en esta prueba era de 8 px; el dueño lo subió a 10 px tras verlo en la Thor).
   - El área útil de 1025 px de alto, o los 1080 reales, es compatible con los 540 px CSS del diseño.
2. Núcleo Kotlin con pruebas en JVM.
3. Cliente UDP y estados de conexión, en la consola: medir el coste por ciclo y comprobar que el juego no se resiente.
4. Puente y datos vivos en una pantalla: Equipo.
5. Resto de pantallas.
6. Pantalla inicial y permisos, y prueba larga de juego real.

Si el paso 1 falla, se para y se replantea con el dueño, antes de seguir. Si pasa, la arquitectura de este documento queda confirmada y se sigue sin otra parada.

### Acceso a la consola

- La Thor se conecta por USB y el constructor usa `adb` (`~/Library/Android/sdk/platform-tools/adb`), con depuración USB ya autorizada en este Mac. Hay que pedir al dueño que la conecte cuando no aparezca en `adb devices`.
- El constructor puede instalar APK y lanzar la app por `adb`. El permiso «Mostrar sobre otras apps» lo concede el dueño en la consola.
- Lo físico lo comprueba el dueño: el mando, la lectura del texto de 10 px, la sesión de 30 minutos y encender y apagar la pantalla.
- Las pantallas se revisan por lotes: Equipo primero y luego grupos de pantallas relacionadas.
- Los 18 snapshots no parecen cubrir subastas ni batalla de cartas. Si el dueño llega a esos casos, se capturan snapshots nuevos de RAM. Lo que no se capture queda sin prueba de equivalencia, y se dice en el informe de esa fase.

## Cómo sabremos que funciona

- Una sesión de al menos 30 minutos en la Thor sin pausas, sin pérdida de mando y sin bajada perceptible de velocidad del juego.
- Los valores de Equipo coinciden con el menú del juego al ganar Bits, subir de nivel y cambiar de mapa.
- Cerrar o cambiar de juego muestra el estado correcto, y volver reconecta solo.
- El dueño aprueba las pantallas renderizadas en la propia consola.

## Riesgos conocidos

- **Nightly móvil.** Cualquier nightly puede romper los comandos. La app debe fallar con un aviso claro. Se recomienda no actualizar RetroArch sin probar. Obtainium instaló la versión anterior y podría ofrecer volver a la estable, lo que rompería la app. La copia del APK actual debe conservarse para poder reinstalarlo.
- **`retroarch.cfg`.** Al editarlo con `adb`, su dueño pasó a ser `shell`. RetroArch sigue con permiso por grupo, pero no se ha visto una escritura real. Conviene comprobarlo en la primera sesión.
- **Tamaño.** Los recursos pesan 74 MB, sobre todo mapas y enemigos. Es aceptable para uso personal.
- **Batería y calor.** Una WebView y un sondeo constante añaden consumo, sin medir aún. Si la pantalla inferior está apagada, el sondeo debe pararse.

## Queda a criterio del constructor

- Librerías Android, versión mínima de SDK (como mínimo 33, por el dispositivo) y estructura interna de módulos.
- Formato exacto del puente WebView y cómo inyectar los eventos.
- Cómo portar loaders y assemblers, manteniendo el comportamiento.
- Mecanismo de migración desde `/_blob/...` a recursos locales.
