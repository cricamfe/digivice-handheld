# Prueba de doble pantalla en la AYN Thor

Fecha: 2026-11-03. Android 13, RetroArch nightly, SwanStation, Digimon World 2003 PAL en marcha.

| Comprobación | Resultado | Cómo |
|---|---|---|
| Abrir la app con el juego en marcha | Correcto: `GET_STATUS PLAYING` en los 6 sondeos siguientes | `adb` |
| Tocar la pantalla inferior | Correcto: el contador sube, sigue `PLAYING` y el foco sigue en el display 0 | Toques por `adb` y del dueño |
| Usar el mando después de tocar | Correcto | Dueño |
| Apagar y encender la pantalla | Correcto: la ventana vuelve y sigue `PLAYING` | `adb` |
| Ocultar desde la notificación | Correcto: reaparece la pantalla inferior habitual | Dueño |
| Tamaño de la página | 621 × 541 CSS, `devicePixelRatio` 2,306, captura de 1240 × 1080 | Página de prueba |
| Texto de 8 px | Legible | Dueño |

## Conclusiones

- La arquitectura queda confirmada: ventana `TYPE_APPLICATION_OVERLAY` sin foco, sobre el display `PRESENTATION`.
- El diseño de 620 × 540 cabe. El alto útil de 1025 px que indica Android no limita esta ventana.
- Con la consola dormida, RetroArch no responde a los comandos. La regla de ocultar por falta de respuesta debe ignorar ese tiempo.
- El foco de entrada se mantiene en RetroArch aunque el foco de la pantalla inferior figure en el lanzador de `cocoonshell`.

## Datos vivos (tarea 9)

| Medida | Resultado |
|---|---|
| Bits en la ventana frente a la RAM | Coinciden: 9999999 |
| Peticiones por ciclo | 3 lecturas de 16 KB y 1 `GET_STATUS`, cada 500 ms |
| CPU de la app | Unos 5 % de un núcleo, en una sola muestra de `top` |
| Memoria de la app | 197 MB, incluida la WebView |
| Foco tras mostrar la ventana | Sigue en el display 0 |
| Pantalla de título | Bits `-`, sin datos antiguos |
| RetroArch sin responder | La ventana se retira y vuelve al responder |

Pendiente de la sesión larga: que los Bits cambien al gastarlos, el menú de RetroArch abierto y el efecto en el juego.
