# Tablas desde la ROM

Scripts de desarrollo que leen una copia propia de Digimon World 2003 (Europa, `SLES-03936`) para regenerar tablas del código. No incluyen datos del juego y sus salidas de imagen se quedan fuera del repositorio.
Los formatos vienen de `markisha64/ddw3` y `ReGame-Labs/dw3_decomp` (MIT).

Las imágenes y los textos que usa la app ya no salen de aquí: los genera en la consola el extractor Kotlin (`Android/extractor`) desde el archivo del usuario. Los renderizadores de Python que sirvieron de referencia para portarlo (mapas, mundo, enemigos, fondos de combate, cartas y textos) no están en este repositorio.

Hace falta la imagen como datos de usuario de 2048 bytes por sector (`DW3_ISO`), Python, Pillow y numpy.

- `DW3_ISO`: ruta de la imagen.
- `DW3_OUT`: carpeta de salida.
- `DW3_DECOMP`: copia de `dw3_decomp`.
- `DW3_DDW3`: copia de `ddw3`.

`iso.py`, `discfs.py`, `mapx.py`, `sprite.py` y `render/dwr.py` leen el disco y sus formatos; los usan los scripts siguientes y `Tools/FieldData/generate_exits.py`.

`item_icons.py` escribe `Frontend/src/handheld/item-icons.ts` con el icono de cada equipo y objeto de la base de datos. Con `--png DIR` también saca los iconos de 12 px de los menús, fuera del repositorio:

    DW3_ISO=game.iso DW3_DECOMP=dw3_decomp DW3_DDW3=ddw3 python3 Tools/RomExtract/item_icons.py

`field_actors.py` escribe `Frontend/src/handheld/field-actors.ts` con la posición de los sprites de cada personaje del campo respecto al punto del actor, y dónde están sus nombres: la referencia a la línea del texto de conversación del juego que los nombra (`talk04:879`), que la app lee del texto extraído del disco, sin guardar ningún nombre. Después hay que volver a ejecutar `Tools/FieldData/generate_text_keys.py`, que añade esas líneas a las palabras del extractor y falla si alguna no nombra a nadie en algún idioma. `actors.py` y `parse_stages.py` leen `DW3_DECOMP` y `DW3_DDW3`:

    DW3_ISO=game.iso DW3_DECOMP=dw3_decomp DW3_DDW3=ddw3 python3 Tools/RomExtract/field_actors.py

Los textos de las misiones del handheld (`en-US/handheld-quests.json` y el `quests.json` de cada idioma) son frases propias con huecos (`{place}`, `{digimon}`, `{person}`, `{item}`, `{card}`); `Frontend/src/handheld/quest-words.ts` guarda para cada hueco la línea del disco que lo nombra (`stname:40`, `talk06:690`, o el hablante de una escena, `dmg800/9:1`, la tabla 9 del archivo `ESDMG800.BIN`) y `generate_text_keys.py` las publica como palabras del extractor (`APP_WORD_FILES`).

Con las mismas cuatro variables de entorno, `python3 Tools/FieldData/generate_extractor_data.py` regenera `Android/extractor/src/main/resources/extractor/stages.json.gz` desde `render/stagedef.load_stage`. Esta tabla contiene únicamente los índices, colores y objetos numéricos de los 294 escenarios; no contiene nombres, textos ni rutas de la descompilación. El gzip tiene fecha y nombre vacíos para que se pueda regenerar de forma determinista.

Las tablas de la app no guardan nombres de Digimon: cada enemigo, digievolución y compañero lleva `nameId`, la línea de los nombres del juego (`dignam:212`) que la app lee del texto extraído del disco. `Tools/FieldData/generate_digimon_names.py` la saca del disco (la tabla de combate `0x1CF` para los enemigos, la tabla de formas del ejecutable para las digievoluciones), añade el personaje del campo de los jefes (`fieldActor`) y comprueba las tablas. Los nombres de archivo de `Frontend/src/assets/maps` de cada mapa están en `Tools/FieldData/map-pictures.json`; la app busca la imagen de un mapa por su id.

    DW3_ISO=game.iso DW3_DECOMP=dw3_decomp python3 Tools/FieldData/generate_digimon_names.py

Las pruebas de paridad del extractor Kotlin (`DW3_ISO`, `DW3_REFERENCE`) comparan con salidas de referencia locales que no están en el repositorio; sin esas variables se saltan.
