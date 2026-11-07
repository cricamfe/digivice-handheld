# Volcados de RAM

`Snapshots/` guarda 18 volcados de 2 MiB de la RAM de la PS1 (`0x00000000`–`0x001FFFFF`) en distintos momentos del juego. Son datos fijos de prueba: las pruebas de `Android/core` los leen en su sitio (`TestEnvironment.snapshot`) y comparan sus lecturas con los estados de `Android/core/src/test/resources/golden`, y `Tools/RetroArchProbe/fake_retroarch.py` los sirve como si fueran la RAM de RetroArch.

Se capturaron con el escáner de memoria de la app de Windows del proyecto original, que ya no forma parte de este repositorio, así que no se regeneran.

`analyze_snapshots.py` imprime, de cada volcado, la unidad activa del combate y los huecos y bloques de sus combatientes:

```sh
python3 Tools/MemoryScanner/analyze_snapshots.py
```
