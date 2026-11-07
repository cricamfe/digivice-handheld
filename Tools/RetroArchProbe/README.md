# Prueba de RAM en RetroArch

Comprueba que RetroArch entrega la RAM de Digimon World 2003 por sus comandos de red. La app de Digivice depende de ello.

## Requisito: RetroArch nightly

| RetroArch para Android | `READ_CORE_MEMORY` con SwanStation |
| --- | --- |
| Estable hasta 1.22.2 | No: compilado sin `HAVE_COMMAND` |
| Nightly desde el 25-03-2026 | Comandos activos, pero rechaza núcleos sin mapa de memoria |
| Nightly desde el 25-10-2026 | Sí: lee la RAM del núcleo desde la dirección 0 |

Descarga: <https://buildbot.libretro.com/nightly/android/RetroArch_aarch64.apk> (194 MB).

Copia antes `/storage/emulated/0/RetroArch` a otro sitio, con partidas, estados y ajustes. La nightly usa el paquete `com.retroarch.aarch64`. Si Android no deja instalarla encima, tendrías que desinstalar la versión actual, y eso puede borrar los datos internos de la app.

## En la Thor

1. Abre RetroArch y actualiza o descarga SwanStation desde el actualizador en línea.
2. Activa `Ajustes > Red > Comandos de red` y deja el puerto en `55355`. Si no aparece, pon en `retroarch.cfg` las líneas `network_cmd_enable = "true"` y `network_cmd_port = "55355"`.
3. Cierra RetroArch del todo y vuelve a abrirlo.
4. Carga el juego PAL (`SLES-03936`) y una partida, y déjalo en marcha con un Digimon en el equipo.
5. Anota la IP de la Thor: `Ajustes de Android > Wi-Fi > tu red`.

## En el Mac

El Mac y la Thor deben estar en la misma red:

```bash
python3 -B Tools/RetroArchProbe/probe_ram.py IP_DE_LA_THOR
```

Compara Bits, nivel, HP y MP con la pantalla del juego. Cambia algo en el juego, por ejemplo gastando Bits, y repite.

| Salida | Significado |
| --- | --- |
| Código 0 y valores iguales | Funciona |
| Código 2 | Sin respuesta: comandos desactivados, versión estable o red |
| Código 3 | RetroArch responde, pero no entrega la RAM: juego sin cargar o nightly antigua |
| Código 4 | Los datos no parecen de una partida: otra versión del juego o partida sin cargar |

Sin Mac, ejecútalo en la Thor desde Termux (`pkg install python`) y sin IP. Mide además la velocidad que tendrá la app, porque también irá dentro de la consola. Por Wi-Fi, las lecturas grandes pueden perderse o tardar más.

## Al terminar

Mientras el puerto esté abierto, cualquiera en tu red puede enviar comandos como `WRITE_CORE_MEMORY` o `LOAD_CORE`. Tras la prueba, desactiva los comandos de red, o limítalos a la propia consola con esta línea en `retroarch.cfg`:

```text
network_cmd_bind_address = "127.0.0.1"
```

## Comprobar el script sin consola

```bash
python3 -B Tools/RetroArchProbe/test_probe.py
```

Usa `fake_retroarch.py` para simular las respuestas con los 18 snapshots de `Tools/MemoryScanner/Snapshots`. No sustituye a la prueba en la Thor.
