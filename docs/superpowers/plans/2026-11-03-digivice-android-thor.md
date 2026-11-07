# Digivice para AYN Thor: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App Android propia que muestra Digivice con datos vivos en la pantalla inferior de la AYN Thor, mientras Digimon World 2003 corre arriba en RetroArch con SwanStation.

**Architecture:** Un servicio en primer plano ejecuta en Kotlin el núcleo portado de `Backend/`, que lee la RAM por UDP con una caché de páginas. Envía los mismos eventos que SignalR a una WebView en una ventana overlay sin foco, sobre el display inferior. La WebView ejecuta una entrada nueva de Vue que reutiliza stores, presenters y datos de `Frontend/`, con componentes nuevos según el diseño aprobado.

**Tech Stack:** Kotlin, Gradle con Android Gradle Plugin, kotlinx.serialization, JUnit 5, Android WebView, Vue 3, Pinia, Vite, .NET 10 solo para generar referencias de prueba.

**Spec:** `docs/superpowers/specs/2026-11-03-digivice-android-thor-design.md`

## Global Constraints

- Solo lectura: nunca enviar `WRITE_CORE_MEMORY`, `WRITE_CORE_RAM`, `LOAD_CORE`, `SET_INPUT` ni comandos similares.
- Solo `SLES-03936` PAL, con RetroArch nightly y SwanStation, en `127.0.0.1:55355`.
- Ciclo de 500 ms; lecturas de 16 KB como máximo por petición.
- `minSdk` 33. Paquete `dev.cricamfe.digivice`.
- `Backend/Memory/Definitions` es la única fuente de direcciones: Kotlin carga esos JSON, sin copiarlos a mano.
- `Backend/`, `Tauri/` y la versión de Windows siguen funcionando sin cambios de comportamiento.
- Press Start 2P solo a 10, 12 o 16 px, sin mayúsculas acentuadas (el pequeño era de 8 px; lo subió a 10 px el dueño, por decisión propia, al ver la Thor). Iconos a 22 o 44 px, salvo el de `WikiObjeto`, a 88 px.
- Marcos de `Design/ui/kit.css`: margen interior incluido, `stretch`, sin `fill`, fondo liso.
- Retratos de `Design/portraits/rejilla`, cartas de `Design/cards`, interrogante `Design/icons/unknown.svg`.
- Interfaz en español (`es-ES`); nombres y descripciones del juego en inglés.
- No copiar código de EmuLnk: licencia PolyForm Noncommercial.
- El permiso «Mostrar sobre otras apps» lo concede el dueño en la consola, nunca `adb`.
- Antes de instalar en la Thor: no desinstalar nada ni tocar `RetroArch`, salvo lo que indique cada tarea.

## Review Focus

- **Respuesta UDP tardía de una petición anterior:** debe descartarse, nunca decodificarse como la lectura actual. Prueba en la tarea 7.
- **Menú de RetroArch abierto o juego en pausa:** mantiene el último estado, sin marcar error. Prueba en la tarea 7.
- **Volver a la pantalla de título con el juego abierto:** el estado se limpia y aparece la espera discreta, sin mostrar datos de la partida anterior. Prueba en la tarea 7.
- **Mapa o identificador fuera de catálogo, como `0x279`:** la pantalla muestra el interrogante y no falla. Prueba en la tarea 11.
- **Pantalla inferior apagada:** se detiene el sondeo y se recupera al encender. Prueba en la tarea 9.

---

### Task 1: Proyecto Android y prueba de doble pantalla

Esta tarea es una parada obligatoria: si alguna comprobación física falla, se detiene el plan y se replantea con el dueño.

**Files:**
- Create: `Android/settings.gradle.kts`, `Android/build.gradle.kts`, `Android/gradle/libs.versions.toml`, wrapper de Gradle
- Create: `Android/app/build.gradle.kts`, `Android/app/src/main/AndroidManifest.xml`
- Create: `Android/app/src/main/java/dev/cricamfe/digivice/LauncherActivity.kt`
- Create: `Android/app/src/main/java/dev/cricamfe/digivice/overlay/OverlayService.kt`
- Create: `Android/app/src/main/java/dev/cricamfe/digivice/overlay/OverlayWindow.kt`
- Create: `Android/app/src/main/assets/spike/index.html`
- Create: `docs/superpowers/spikes/2026-11-dual-screen.md`

**Interfaces:**
- Produces: `OverlayWindow(context: Context, display: Display)` con `show(url: String)`, `hide()`, `isShowing: Boolean`, `webView: WebView`.
- Produces: `OverlayService`, foreground service `specialUse`, con acciones `ACTION_HIDE` y `ACTION_EXIT` en su notificación.

- [ ] **Step 1: Crear rama y guardar lo ya generado**

```bash
git switch -c android-thor
git add docs Design Tools/RetroArchProbe
git commit -m "docs: add Thor design, assets and RAM probe"
```

- [ ] **Step 2: Crear el proyecto Gradle**

Usa versiones estables compatibles con el JDK de Android Studio (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`, Java 25) y el SDK instalado (`android-37.0`, build-tools 36). `compileSdk` 37, `targetSdk` 36, `minSdk` 33.

Run: `cd Android && JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Implementar lanzador, servicio y ventana**

`LauncherActivity` pide el permiso si falta, arranca el servicio y termina. `OverlayWindow` crea una ventana `TYPE_APPLICATION_OVERLAY` sobre el primer display de `DisplayManager.DISPLAY_CATEGORY_PRESENTATION`, con `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_NO_LIMITS` y tamaño de pantalla completa. La WebView usa `setInitialScale(200)`, que da 620 px CSS sobre 1240 físicos.

La página de prueba es una copia de `Digimon.dc.html` con imágenes locales. Muestra `innerWidth`, `innerHeight`, `devicePixelRatio` y un contador de toques.

- [ ] **Step 4: Instalar en la Thor y conceder el permiso**

Pide al dueño que conecte la consola, y comprueba `~/Library/Android/sdk/platform-tools/adb devices`. Instala con `adb install -r app/build/outputs/apk/debug/app-debug.apk`. El dueño concede el permiso.

- [ ] **Step 5: Comprobar con el dueño y anotar en `docs/superpowers/spikes/2026-11-dual-screen.md`**

Con Digimon World 2003 en marcha en SwanStation:

| Comprobación | Resultado esperado |
|---|---|
| Abrir la app con el juego en marcha | El juego no se pausa |
| Tocar la pantalla inferior varias veces | El contador sube, el juego no se pausa |
| Usar el mando después de tocar | Sigue controlando el juego |
| Apagar y encender la pantalla | El overlay vuelve |
| Ocultar desde la notificación | Reaparece `cocoonshell` |
| `innerWidth` e `innerHeight` | 620 × 540, o anotar el valor real |
| Texto pequeño (10 px; era de 8 px y el dueño lo subió tras verlo en la Thor) | Legible para el dueño |

Si el alto útil no es 540, anota el valor y ajústalo con el dueño antes de seguir, porque afecta a todas las pantallas.

- [ ] **Step 6: Parar si algo falla y confirmar con el dueño**

Con todo correcto y el dueño de acuerdo, la arquitectura queda confirmada.

- [ ] **Step 7: Commit**

```bash
git add Android docs/superpowers/spikes
git commit -m "feat(android): add overlay spike on the Thor lower display"
```

### Task 2: Referencias de prueba generadas con el backend original

**Files:**
- Create: `Tools/GoldenState/GoldenState.csproj`, `Tools/GoldenState/Program.cs`, `Tools/GoldenState/SnapshotMemoryAccessor.cs`
- Create: `Android/core/src/test/resources/golden/state/<snapshot>.json`
- Create: `Android/core/src/test/resources/golden/events/<antes>__<despues>.json`

**Interfaces:**
- Consumes: `IMemoryAccessor`, `IDuckstationSession`, `AddBackendServices`, `StateComposer`, `StateConverter.ToDTO`, `StateEventFactory.Create` de `Backend/`.
- Produces: JSON con las mismas opciones que SignalR, ordenado de forma estable, que usarán las tareas 3 a 6.

- [ ] **Step 1: Instalar .NET 10 con permiso del dueño**

Run: `brew install --cask dotnet-sdk`, solo tras confirmar con el dueño.
Expected: `dotnet --version` empieza por `10.`

- [ ] **Step 2: Implementar `SnapshotMemoryAccessor(byte[] ram) : IMemoryAccessor` y el programa**

Sustituye en el contenedor `IDuckstationSession` por una sesión con ese acceso. No modifiques `Backend/`. Genera un JSON de estado por cada uno de los 18 snapshots de `Tools/MemoryScanner/Snapshots`, y otro de eventos por cada par de snapshots consecutivos por nombre. Usa `JsonSerializerDefaults.Web`, como SignalR, y `DtoRuntimeTypeJsonConverter`.

- [ ] **Step 3: Generar y comprobar**

Run: `dotnet run --project Tools/GoldenState -- Tools/MemoryScanner/Snapshots Android/core/src/test/resources/golden`
Expected: 18 archivos en `state`. En `antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1.json`: Bits `45846`, mapa `0x21A` y Kotemon a nivel 25 con HP 1862/2079.

- [ ] **Step 4: Comprobar que Windows sigue igual y hacer commit**

Run: `dotnet test Tests/Tests.csproj`
Expected: mismos resultados que en `main`. Anota los que fallen por depender de Windows.

```bash
git add Tools/GoldenState Android/core/src/test/resources/golden
git commit -m "test: generate golden state from the original backend"
```

### Task 3: Módulo de núcleo: direcciones, acceso a memoria y lectores

**Files:**
- Create: `Android/core/build.gradle.kts`
- Create en `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/memory/`: `MemoryAccessor.kt`, `SnapshotAccessor.kt`, `MemoryReader.kt`, `MemoryBlockReader.kt`, `addresses/`, `resources/`, `readers/`, `converters/`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/memory/`

**Interfaces:**
- Produces: `interface MemoryAccessor { fun readInt32(address: Long): Int; fun readInt16(address: Long): Short; fun readBytes(address: Long, count: Int): ByteArray }`.
- Produces: `class SnapshotAccessor(ram: ByteArray) : MemoryAccessor`.
- Produces: `class AddressesRepository(readText: (relativePath: String) -> String)`, con los mismos métodos que `IAddressesRepository`.
- Produces: un lector Kotlin por cada lector de `Backend/Memory/Readers`, con el mismo nombre y método `read`.

- [ ] **Step 1: Portar las pruebas de `Tests/Unit/Memory` y `Tests/Integration/Memory` como pruebas que fallan**

Añade además:

```kotlin
@Test fun `player values match the known snapshot`() {
    val player = PlayerReader(MemoryReader(snapshot("antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1"))).read(addresses.player())
    assertEquals(45846, player.bits)
    assertEquals(0x21A, player.mapId.toInt())
}
```

- [ ] **Step 2: Ejecutar para ver que fallan**

Run: `./gradlew :core:test`
Expected: FAIL por símbolos sin definir.

- [ ] **Step 3: Portar direcciones, recursos, conversores y lectores**

Traducción directa, conservando nombres en camelCase. Mantén el comportamiento de `MemoryBlockReader` fuera de límites: devuelve 0 y registra un aviso. Little-endian, como `BitConverter`.

- [ ] **Step 4: Ejecutar y hacer commit**

Run: `./gradlew :core:test`
Expected: PASS

```bash
git add Android/core
git commit -m "feat(core): port memory addresses and readers to Kotlin"
```

### Task 4: Núcleo: dominio, loaders y composición del estado

**Files:**
- Create en `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/`: `domain/models/`, `domain/assemblers/`, `application/loaders/`, `application/providers/`, `application/StateComposer.kt`, `CoreModule.kt`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/{domain,application}/`

**Interfaces:**
- Consumes: lectores y `AddressesRepository` de la tarea 3.
- Produces: `class StateComposer { fun compose(): State }`.
- Produces: `class CoreModule(accessor: MemoryAccessor, readDefinition: (String) -> String)`, con `stateComposer`, y wiring manual sin framework de inyección.

- [ ] **Step 1: Portar como pruebas que fallan** `Tests/Unit/Domain`, `Tests/Unit/Application` y `Tests/Integration/Application/Loaders`.

- [ ] **Step 2: Ejecutar para ver que fallan**

Run: `./gradlew :core:test`
Expected: FAIL

- [ ] **Step 3: Portar modelos, ensambladores, loaders y proveedores, con el mismo cableado que `Backend/Infrastructure/DependencyInjection.cs`**

- [ ] **Step 4: Ejecutar y hacer commit**

Run: `./gradlew :core:test`
Expected: PASS

```bash
git add Android/core
git commit -m "feat(core): port domain, loaders and state composition"
```

### Task 5: Núcleo: DTO y equivalencia con el backend original

**Files:**
- Create en `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/events/`: `dto/`, `converters/`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/events/StateGoldenTest.kt`

**Interfaces:**
- Produces: `object StateConverter { fun toDto(state: State): StateDto }`.
- Produces: `val coreJson: Json`, con `@Serializable` y nombres en camelCase idénticos a `Frontend/src/events/dto/`.

- [ ] **Step 1: Escribir la prueba de equivalencia, parametrizada por los 18 snapshots**

```kotlin
@ParameterizedTest @MethodSource("snapshots")
fun `state json equals the original backend`(name: String) {
    val state = CoreModule(snapshot(name), ::definition).stateComposer.compose()
    assertJsonEquals(golden("state/$name.json"), coreJson.encodeToString(StateConverter.toDto(state)))
}
```

`assertJsonEquals` compara árboles JSON e indica la ruta de la primera diferencia. Porta también `Tests/Unit/Events/Converters`.

- [ ] **Step 2: Ejecutar para ver que falla**

Run: `./gradlew :core:test --tests '*StateGoldenTest*'`
Expected: FAIL

- [ ] **Step 3: Portar DTO y conversores**

- [ ] **Step 4: Ejecutar y hacer commit**

Run: `./gradlew :core:test`
Expected: PASS en los 18 snapshots.

```bash
git add Android/core
git commit -m "feat(core): port DTOs with golden equivalence"
```

### Task 6: Núcleo: diferencias y eventos

**Files:**
- Create en `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/events/`: `diffing/`, `factory/`, `GameStateStore.kt`, `Event.kt`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/events/EventsGoldenTest.kt`

**Interfaces:**
- Produces: `data class Event(val type: EventType, val payload: Dto)`, con `EventType` igual al enum del backend.
- Produces: `object StateEventFactory { fun create(previous: State?, current: State): List<Event> }`.
- Produces: `object HealthEventFactory { fun success(store: GameStateStore): List<Event>; fun error(store: GameStateStore, code: String, detail: String? = null): List<Event> }`.
- Produces: `class GameStateStore { var currentState: State?; var status: HealthStatus; var lastErrorCode: String?; var lastErrorDetail: String?; fun clearState() }`.

- [ ] **Step 1: Portar como pruebas que fallan** `Tests/Unit/Events/Diffing`, `Factory` y `States`, y añadir la comparación con `golden/events` por cada par.

- [ ] **Step 2: Ejecutar para ver que fallan**

Run: `./gradlew :core:test`
Expected: FAIL

- [ ] **Step 3: Portar diferencias, fábricas y almacén**

- [ ] **Step 4: Ejecutar y hacer commit**

Run: `./gradlew :core:test`
Expected: PASS

```bash
git add Android/core
git commit -m "feat(core): port event diffing and factories"
```

### Task 7: Cliente de RetroArch, caché de páginas y bucle

**Files:**
- Create en `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/retroarch/`: `RetroArchClient.kt`, `PagedMemoryAccessor.kt`, `GameSignature.kt`, `RetroArchConnector.kt`, `ConnectionCodes.kt`
- Create: `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/GameLoop.kt`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/retroarch/FakeRetroArch.kt` y sus pruebas

**Interfaces:**
- Consumes: `MemoryAccessor`, `CoreModule`, `GameStateStore`, fábricas de eventos.
- Produces: `class RetroArchClient(host: String = "127.0.0.1", port: Int = 55355, timeoutMs: Int = 500)`, con `version(): String?`, `status(): RetroArchStatus` y `read(address: Long, size: Int): ByteArray`.
- Produces: `sealed interface RetroArchStatus` con `Unreachable`, `Contentless`, `Paused(title)` y `Playing(title)`.
- Produces: `class PagedMemoryAccessor(client: RetroArchClient, pageSize: Int = 16384) : MemoryAccessor`, con `beginCycle()` y `requestsInCycle: Int`.
- Produces: `object ConnectionCodes`, con `RETROARCH_UNREACHABLE = "retroarch_unreachable"`, `NO_CONTENT = "no_content"`, `MEMORY_UNAVAILABLE = "memory_unavailable"`, `WRONG_GAME = "wrong_game"`, `WAITING_FOR_SAVE = "waiting_for_save"`, `MEMORY_READ_FAILED = "memory_read_failed"`, `STATE_COMPOSE_FAILED = "state_compose_failed"`.
- Produces: `class GameLoop(connector: RetroArchConnector, module: CoreModule, store: GameStateStore, dispatch: (Event) -> Unit)`, con `tick()`, `start(scope)` y `stop()`.

- [ ] **Step 1: Escribir `FakeRetroArch`, servidor UDP de prueba con los formatos de `Tools/RetroArchProbe/fake_retroarch.py`, y estas pruebas que fallan**

| Prueba | Comprobación |
|---|---|
| `parses memory reply` | `READ_CORE_MEMORY 48da0 16 B3 00 00` da esos bytes |
| `refused read throws with reason` | `-1 no memory map defined` lanza `MemoryReadRefused` con el motivo |
| `truncated read is refused` | Menos bytes de los pedidos lanzan `MemoryReadRefused` |
| `late reply is discarded` | Una respuesta de otra dirección no se decodifica |
| `lost reply retries once` | Dos pérdidas seguidas lanzan `MemoryReadLost`; una se recupera |
| `pages are read once per cycle` | Varias lecturas en una página, una sola petición |
| `reads across pages join correctly` | Una lectura en el límite de 16 KB coincide con el snapshot |
| `never sends write commands` | Tras varios ciclos, ningún comando fuera de `VERSION`, `GET_STATUS` y `READ_CORE_MEMORY` |
| `signature accepts the game` | Coincide en los 18 snapshots y en `Tools/RetroArchProbe/thor-title.bin` |
| `signature rejects other RAM` | Una RAM a ceros no coincide |
| `cycle cost is bounded` | En los 18 snapshots, `requestsInCycle <= 10`; imprime el máximo real |

Y estas, con `GameLoop`:

| Situación del simulador | Resultado |
|---|---|
| Sin respuesta a `VERSION` | Error `retroarch_unreachable` |
| `GET_STATUS CONTENTLESS` | Error `no_content` |
| `-1 no memory map defined` con juego | Error `memory_unavailable` |
| Firma incorrecta | Error `wrong_game` |
| Firma correcta, equipo vacío | Error `waiting_for_save` y estado limpio |
| `GET_STATUS PAUSED` | Mantiene el último estado, sin error ni eventos nuevos |
| Partida, y después título | Se limpia el estado y pasa a `waiting_for_save` |
| Respuesta perdida en un ciclo | Se salta el ciclo y conserva el estado |
| Cambio de nombre en `GET_STATUS` | Vuelve a comprobar la firma |

- [ ] **Step 2: Ejecutar para ver que fallan**

Run: `./gradlew :core:test --tests '*retroarch*'`
Expected: FAIL

- [ ] **Step 3: Implementar**

Firma: CRC32 de `0x10105` a `0x3BAE3` inclusive. Calcula la constante con los snapshots en una prueba y fíjala en `GameSignature`. Cada petición lleva su dirección en la respuesta, y debe coincidir antes de aceptarla. El equipo está vacío si los tres huecos de `PartyAddresses` valen `0xFF`.

- [ ] **Step 4: Ejecutar y hacer commit**

Run: `./gradlew :core:test`
Expected: PASS

```bash
git add Android/core
git commit -m "feat(core): add RetroArch client, page cache and game loop"
```

### Task 8: Entrada `handheld` y puente de eventos

**Files:**
- Create: `Frontend/handheld.html`, `Frontend/vite.handheld.config.ts`, `Frontend/src/handheld/main.ts`, `Frontend/src/handheld/App.vue`
- Create: `Frontend/src/events/event-source.ts`, `Frontend/src/handheld/bridge.service.ts`, `Frontend/src/handheld/mock.service.ts`
- Modify: `Frontend/src/events/signalr.handlers.ts`, `Frontend/src/events/signalr.service.ts`, `Frontend/package.json`

**Interfaces:**
- Produces: `interface GameEventSource { on<K extends keyof EventsMap>(eventName: K, handler: (data: EventsMap[K]) => void): void }`.
- Produces: `initializeSignalRHandlers(source: GameEventSource = signalRService): void`.
- Produces: `window.__digivice.receive(eventType: string, payloadJson: string): void`.
- Produces: `npm run build:handheld`, con salida en `Frontend/dist-handheld`.
- Produces: `handheld.html?mock=<snapshot>`, que carga el estado de prueba para revisar pantallas en el Mac.

- [ ] **Step 1: Introducir `GameEventSource` sin cambiar el comportamiento de Windows**

Run: `cd Frontend && npm run type-check && npm run build-only`
Expected: sin errores.

- [ ] **Step 2: Implementar puente, simulador y entrada**

`bridge.service.ts` emite `HubConnectionStatusChanged` conectado al arrancar. `App.vue` muestra por ahora solo los Bits del store. `mock.service.ts` lee los JSON de `Android/core/src/test/resources/golden/state`.

- [ ] **Step 3: Comprobar el aislamiento del bundle**

Run: `npm run build:handheld && ! grep -rlE 'HubConnectionBuilder|__TAURI' dist-handheld`
Expected: compila y `grep` no encuentra nada.

- [ ] **Step 4: Comprobar en el navegador**

Abre `handheld.html?mock=antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1`.
Expected: muestra `45846`.

- [ ] **Step 5: Commit**

```bash
git add Frontend
git commit -m "feat(frontend): add handheld entry with event bridge"
```

### Task 9: Integrar servicio, puente y reglas del overlay

**Files:**
- Create: `Android/core/src/main/kotlin/dev/cricamfe/digivice/core/OverlayPolicy.kt`
- Create: `Android/app/src/main/java/dev/cricamfe/digivice/overlay/WebBridge.kt`
- Modify: `OverlayService.kt`, `Android/app/build.gradle.kts`, `Android/settings.gradle.kts`
- Test: `Android/core/src/test/kotlin/dev/cricamfe/digivice/core/OverlayPolicyTest.kt`

**Interfaces:**
- Consumes: `GameLoop`, `coreJson`, `OverlayWindow`, `window.__digivice.receive`.
- Produces: `class OverlayPolicy(hideAfterMs: Long = 5000) { fun onReachable(nowMs: Long); fun onUnreachable(nowMs: Long); fun onDisplayChanged(isOn: Boolean); val shouldShow: Boolean; val shouldPoll: Boolean }`.
- Produces: `window.DigiviceNative.hide()` y `window.DigiviceNative.exit()`, expuestos a la WebView, que hacen lo mismo que las acciones de la notificación.

- [ ] **Step 1: Escribir pruebas de `OverlayPolicy` que fallan**

| Prueba | Comprobación |
|---|---|
| `hidden until RetroArch answers` | `shouldShow` falso al inicio y cierto tras `onReachable` |
| `stays visible through short gaps` | Sigue visible tras 4 s sin respuesta |
| `hides after five seconds unreachable` | Oculto a los 5 s |
| `display off stops polling` | `shouldPoll` falso con pantalla apagada |
| `display on resumes polling` | `shouldPoll` cierto al encender, y el overlay vuelve |

- [ ] **Step 2: Ejecutar para ver que fallan**

Run: `./gradlew :core:test --tests '*OverlayPolicy*'`
Expected: FAIL

- [ ] **Step 3: Implementar e integrar**

Gradle copia `Frontend/dist-handheld` y `Backend/Memory/Definitions` a los assets de la app en cada compilación. El servicio ejecuta `GameLoop` y envía cada evento con `evaluateJavascript`, en el hilo principal. El estado de pantalla sale de `DisplayManager.DisplayListener`. Al conectar la WebView, envía `InitialState` y el estado de salud.

Los códigos nuevos de `ConnectionCodes` se añaden a `Frontend/src/events/emulator-connection-error-codes.ts`.

- [ ] **Step 4: Ejecutar pruebas**

Run: `./gradlew :core:test :app:assembleDebug`
Expected: PASS y `BUILD SUCCESSFUL`.

- [ ] **Step 5: Comprobar en la Thor con el dueño y medir**

Instala y carga una partida. Expected: los Bits coinciden con el menú y cambian al gastar. Anota en `docs/superpowers/spikes/2026-11-dual-screen.md` el coste real por ciclo y si el juego se resiente.

- [ ] **Step 6: Commit**

```bash
git add Android Frontend/src/events docs/superpowers/spikes
git commit -m "feat(android): run the core in the overlay service"
```

### Task 10: Kit visual, navegación y Equipo

**Files:**
- Create: `Frontend/src/handheld/kit.css`, `Frontend/src/handheld/router.ts`, `Frontend/src/handheld/components/{NavBar,Header,PartyColumn,PortraitSlot,StatusBar}.vue`, `Frontend/src/handheld/screens/EquipoScreen.vue`
- Create: `Frontend/src/handheld/assets/` con copias de `Design/ui`, `Design/icons`, `Design/portraits/rejilla`, `Design/sprites`, `Design/cards`
- Create: `Frontend/src/i18n/locales/es-ES/`
- Modify: `Frontend/src/handheld/App.vue`, `Frontend/src/i18n/index.ts`

**Interfaces:**
- Consumes: `useGameStore`, presenters y viewmodels de `Frontend/src`.
- Produces: `useScreen()`, con `current` y `go(name: ScreenName)`; `ScreenName` incluye todas las pantallas de las tareas 10 a 14.
- Produces: `<PortraitSlot :digimon="id" :status="status" />`, con imagen `portraits/rejilla/<Nombre>-<status>.png`.
- Produces: `es-ES` con la misma estructura que `en-US`. Nombres y descripciones del juego caen a `en-US` por `fallbackLocale`.
- Produces: en `NavBar`, mantener pulsado un botón durante 1 segundo abre `AppPanel.vue` con «Ocultar», «Salir» y «Cancelar», sin navegar. Llama a `window.DigiviceNative`; en el navegador, sin ese objeto, los botones solo cierran el panel.

Regla para las tareas 10 a 14: reproduce `Design/screens/<Pantalla>.dc.html` a 620 × 540, con datos del store y los presenters existentes. Las imágenes `/_blob/<id>` se identifican comparando su `sha256` en el listado de recursos del artifact con los archivos locales. Una pantalla que necesite un dato inexistente se avisa, sin inventarlo.

- [ ] **Step 1: Implementar kit y estado con el simulador**

`kit.css` parte de `Design/ui/kit.css`, con rutas locales. El estado del retrato sale de `ProfilePresenter.getStatus`.

- [ ] **Step 2: Implementar Equipo según `Digimon.dc.html`**

- [ ] **Step 3: Comprobar tipos y compilación**

Run: `npm run type-check && npm run build:handheld`
Expected: sin errores.

- [ ] **Step 4: Comparar en el navegador**

Abre `handheld.html?mock=antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1` a 620 × 540.
Expected: mismo aspecto que `Digimon.dc.html`, con Kotemon Nv 25, HP 1862/2079 y Bits 45846. Sin texto ni iconos pegados al marco.

- [ ] **Step 5: Revisar en la Thor con el dueño y hacer commit**

```bash
git add Frontend
git commit -m "feat(handheld): add visual kit and party screen"
```

### Task 11: Mapa, batalla, Seabed y Mobius

**Files:**
- Create en `Frontend/src/handheld/screens/`: `MapaScreen.vue`, `BatallaScreen.vue`, `BatallaCartasScreen.vue`, `SeabedScreen.vue`, `SeabedRutaScreen.vue`, `MobiusScreen.vue`
- Test: `Frontend/src/handheld/mock-cases.ts`

**Interfaces:**
- Consumes: `useScreen`, `PartyColumn` y los presenters de `presenters/map`, `seabed-modal` y `mobius-desert-modal`.
- Produces: la pestaña Mapa elige sola entre mapa, batalla, batalla de cartas, Seabed y Mobius, según el store.
- Produces: `mock-cases.ts`, con variantes para revisar: `unknownMap` (MapId `0x279`), `digimonBattle`, `cardBattle`.

- [ ] **Step 1: Implementar las seis pantallas según sus `.dc.html`**

Batalla de cartas usa `NpcBattleOpponentHelper` y `database/tcg/folder.json`, y muestra el mazo rival. Con un mapa fuera de catálogo, muestra `Design/icons/unknown.svg` y «Mapa desconocido» sin fallar.

- [ ] **Step 2: Comprobar compilación y variantes**

Run: `npm run type-check && npm run build:handheld`
Expected: sin errores. En el navegador, `?mock=...&case=unknownMap` muestra el interrogante; `digimonBattle` y `cardBattle` cambian la pestaña Mapa.

- [ ] **Step 3: Revisar en la Thor con el dueño y hacer commit**

```bash
git add Frontend
git commit -m "feat(handheld): add map, battle, seabed and desert screens"
```

### Task 12: Diario, misión, subastas, evoluciones y técnicas

**Files:**
- Create en `Frontend/src/handheld/screens/`: `DiarioScreen.vue`, `MisionScreen.vue`, `SubastasScreen.vue`, `EvolucionesScreen.vue`, `TecnicasScreen.vue`

**Interfaces:**
- Consumes: `useScreen`, presenters de `journal`, `auction`, `digievolution`, `tree`.

- [ ] **Step 1: Implementar las cinco pantallas según sus `.dc.html`**

`Subastas` cubre: activa, activa sin Bits suficientes, sin subasta, participada, perdida y futura. Sin subasta activa, el panel superior usa el texto de `Diario`.

- [ ] **Step 2: Comprobar y revisar**

Run: `npm run type-check && npm run build:handheld`
Expected: sin errores. Revisa con el simulador los estados de subasta y las pestañas del diario.

- [ ] **Step 3: Revisar en la Thor con el dueño y hacer commit**

```bash
git add Frontend
git commit -m "feat(handheld): add journal, auctions and evolution screens"
```

### Task 13: Wiki

**Files:**
- Create en `Frontend/src/handheld/screens/wiki/`: `WikiIndice.vue`, `WikiLista.vue`, `WikiBuscar.vue`, `WikiEnemigo.vue`, `WikiCarta.vue`, `WikiSobre.vue`, `WikiTienda.vue`, `WikiEquipo.vue`, `WikiObjeto.vue`, `WikiNpc.vue`
- Create: `Frontend/src/handheld/components/PixelKeyboard.vue`

**Interfaces:**
- Consumes: `useScreen`, `services/` y `database/` existentes, y la búsqueda de `viewmodels/search`.
- Produces: `<PixelKeyboard v-model="query" />`, sin usar el teclado del sistema.

- [ ] **Step 1: Implementar las diez pantallas según sus `.dc.html`**

Una carta de acción muestra su nota en lugar de AP y HP. Una batalla de NPC bloqueada muestra qué falta. Un dato ausente usa el interrogante con texto, nunca un hueco. Los objetos que no son chips usan el cofre.

- [ ] **Step 2: Comprobar y revisar**

Run: `npm run type-check && npm run build:handheld`
Expected: sin errores. En el navegador se recorre índice → listado → ficha en cada categoría, y la búsqueda encuentra `Kotemon`, `Armor Chip` y `Genji`.

- [ ] **Step 3: Revisar en la Thor con el dueño y hacer commit**

```bash
git add Frontend
git commit -m "feat(handheld): add wiki screens and pixel keyboard"
```

### Task 14: Estados de conexión y pantalla inicial

**Files:**
- Create: `Frontend/src/handheld/screens/EsperaScreen.vue`
- Modify: `Frontend/src/handheld/App.vue`, `Frontend/src/i18n/locales/es-ES/`, `Android/app/src/main/java/dev/cricamfe/digivice/LauncherActivity.kt`

**Interfaces:**
- Consumes: `store.healthStatus`, `store.lastErrorCode`, `ConnectionCodes`.
- Produces: un estado por código de `ConnectionCodes`; las pestañas de revisión de `Espera.dc.html` no se incluyen.

- [ ] **Step 1: Implementar según `Espera.dc.html`**

`no_content` y `waiting_for_save` son esperas discretas. El permiso inicial lo muestra `LauncherActivity` en la pantalla superior, con una WebView a la misma escala y el botón «Abrir ajustes». También explica que mantener pulsada la barra inferior abre las opciones para ocultar o salir. Conserva los créditos «Digivice, de jeanheck» y «Port de cricamfe».

- [ ] **Step 2: Comprobar en la Thor con el dueño**

Run: `./gradlew :core:test :app:assembleDebug`
Expected: PASS.

| Acción en la Thor | Pantalla esperada |
|---|---|
| RetroArch abierto sin juego | Espera discreta |
| Otro juego cargado | «No es Digimon World 2003» |
| Pantalla de título | «Esperando tu partida» |
| Cerrar RetroArch | El overlay se retira a los 5 s |
| Volver a abrir RetroArch y el juego | Reconecta solo |

- [ ] **Step 3: Commit**

```bash
git add Frontend Android
git commit -m "feat: add connection states and first-run screen"
```

### Task 15: Sesión larga y cierre

**Files:**
- Modify: `docs/superpowers/spikes/2026-11-dual-screen.md`
- Create: `Android/README.md`

- [ ] **Step 1: Comprobar que la app solo lee**

Run: `grep -rnE 'WRITE_CORE|LOAD_CORE|SET_INPUT' Android/core/src/main Android/app/src/main`
Expected: sin resultados.

- [ ] **Step 2: Sesión real con el dueño, de 30 minutos como mínimo**

| Criterio | Resultado esperado |
|---|---|
| Juego | Sin pausas, sin perder el mando, sin bajada perceptible |
| Equipo | Coincide con el menú al ganar Bits, subir de nivel y cambiar de mapa |
| Pantallas | El dueño las aprueba en la consola |
| Batería y calor | Anotar consumo; sin sondeo con la pantalla inferior apagada |

Anota los resultados, y lo que quede sin probar, como subastas o batalla de cartas si no se llega a ellas.

- [ ] **Step 3: Escribir `Android/README.md`**

Debe incluir: versión exacta de la nightly probada, ajustes de `retroarch.cfg`, compilación e instalación, regeneración de referencias y por qué no actualizar RetroArch sin probar.

- [ ] **Step 4: Ejecutar todas las pruebas y hacer commit**

Run: `./gradlew :core:test :app:assembleDebug && dotnet test ../Tests/Tests.csproj && (cd ../Frontend && npm run type-check && npm run build-only && npm run build:handheld)`
Expected: todo correcto, con los resultados de `dotnet test` iguales a los de la tarea 2.

```bash
git add Android docs
git commit -m "docs(android): record long session and setup guide"
```
