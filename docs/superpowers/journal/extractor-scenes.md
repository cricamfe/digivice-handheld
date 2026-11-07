# Scene replacement ledger

Task 8, November 5, 2026. This ledger separates reconstructed disc resources from the upstream composite screenshots. No screenshots or extracted textures are added to Git. The playing APK is unchanged.

## Reconstructed resources

Both world maps and their unlabelled bases are rendered from the original disc. The 47 marker rows and 23 town rows are read from the European `STSTATUS.PRO` overlay, rather than shipping the decompilation's coordinate arrays. All four PNGs match `world_map.py` pixel for pixel.

The eight partner portraits use the same characters' standing field sprites (direction 1), with transparent backgrounds. Partner IDs 0–7 map to actor IDs 182, 180, 181, 34, 35, 64, 65 and 66. These replace the edited portraits in task 10; status decoration belongs to the app. The owner must see the resulting interface before visual acceptance.

## Composite pictures

The inventory of `AAA/DAT/SCREEN` found 23 files containing sprite sheets, texture archives and compressed resources. The card frame/digit and world-map compositions are already reconstructed. This bounded investigation did not establish a complete renderer for the remaining screenshots, which combine game UI, state, text and sometimes field backgrounds. Recreating those menus solely to produce a companion placeholder is unnecessary; task 10 will use app-drawn panels.

Comparison of the current map asset names against commit `29e6e705` identifies **18** remaining pictures after excluding the two world maps. There are **seven**, rather than the earlier inventory's approximate eight, saving pictures.

| Existing picture | Task 10 replacement | Status |
| --- | --- | --- |
| Card Shop Screen | Card-shop panel | Pending implementation and owner capture review |
| In Battle Screen | Battle panel | Pending implementation and owner capture review |
| In Card Battle Screen | Card-battle panel | Pending implementation and owner capture review |
| In Menu Screen | Game-menu panel | Pending implementation and owner capture review |
| In Store Screen | Store panel | Pending implementation and owner capture review |
| In Training Screen | Training panel | Pending implementation and owner capture review |
| Initial Pack Screen | Starter-pack panel | Pending implementation and owner capture review |
| Kadomatsu Piximon Screen | Piximon panel | Pending implementation and owner capture review |
| Reward Screen | Reward panel | Pending implementation and owner capture review |
| Saving at Asuka Inn 1F | Save panel with current location | Pending implementation and owner capture review |
| Saving at Forest Inn | Save panel with current location | Pending implementation and owner capture review |
| Saving at Mirage Hall | Save panel with current location | Pending implementation and owner capture review |
| Saving at Pelche Oasis | Save panel with current location | Pending implementation and owner capture review |
| Saving at Seiryu Tower | Save panel with current location | Pending implementation and owner capture review |
| Saving at Suzaku Inn | Save panel with current location | Pending implementation and owner capture review |
| Saving at Swamp Inn | Save panel with current location | Pending implementation and owner capture review |
| Cargo Tower | Location panel; no fabricated walkable map | Pending implementation and owner capture review |
| Deeper Crevice | Location panel; no fabricated walkable map | Pending implementation and owner capture review |

Cargo Tower uses runtime mode 528 (`0210`), with its setup in `FIELDSTG` and events in `WSTAG260`. Deeper Crevice uses runtime mode 557 (`022D`), `WSTAG415` (and the corresponding `WSTAG815` variant). These three WSTAG entries have no `mapFile` and are outside the 237 `SnnnPACK` map renderer. Deeper Crevice has its own drawing tasks. Neither field is silently passed off as a normal reconstructed map.

WSTAG numbers and RAM mode IDs are different namespaces. `stages.json.gz`, `MapRenderer.maps` and the extractor's map keys use WSTAG numbers. The frontend map database uses hexadecimal RAM modes. Task 10 must resolve the game's factual mode-to-WSTAG table before looking up a map; hexadecimal conversion alone is insufficient.

No empty `ScenePictures` API or pretend extracted files are introduced. The manifest contains only resources that have actually been generated. This is the explicit fallback allowed by the design; image routing and the panel implementation remain task 10 work.
