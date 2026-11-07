# Disc extraction implementation

Work resumed on November 5, 2026, on `recursos-iso`, from the tested build/session baseline `642728f2`. The playing baseline `android-thor` is unchanged. Nothing has been published.

## Runtime optimization assessment

The current implementation already limits work to the visible interface:

- `GameLoop` polls general state every 500 ms and position/field changes every 100 ms while the map is visible. Actor reads are also map-specific.
- `PagedMemoryAccessor` reuses pages within a polling cycle, then invalidates them for the next cycle. Change detection suppresses redundant events.
- `OverlayService` serializes away from the main thread and skips serialization until its WebView is ready.
- `use-follow.ts` caps map animation at approximately 30 FPS and stops animation frames at rest. Position updates use transforms instead of rebuilding the map.

These are source findings, independently reviewed, not measurements of CPU or battery consumption. The November 3 spike's 197 MB / 5% sample predates some of the current polling work. A fresh baseline should cover idle, walking with NPCs, another tab, and screen-off operation before changing polling or WebView behavior.

Extraction is preparation performed once, outside gameplay. Moving resources out of the APK does not establish an FPS or battery improvement; generated resources still occupy storage and decoded images still occupy memory. The implementation must process maps individually, bound allocations and retain the existing interface optimizations.

## Local disc evidence

All game files and reference output stay outside Git. The existing original cooked image has 294,280 sectors of 2,048 bytes (602,685,440 bytes). A fresh BIN obtained from the owner's original CHD with `chdman extractcd` has the same number of 2,352-byte sectors (692,146,560 bytes), with one MODE2/2352 track and no pregap in the generated CUE.

Comparing the 2,048 user bytes at offset 24 of **every** raw sector against the cooked image found **zero differences**. Both user-data streams have SHA-256:

```text
e5bc8a46269cddafe2b703d1b4e51a4defcb45590e8ab602304d920d6690bb1e
```

The existing Python directory reader lists 2,385 files. `SYSTEM.CNF` is at LBA 23; `SLES_039.36` is at LBA 24, length 923,648 bytes. Its SHA-256 is:

```text
15f37849a81b9f00e7f20d00de7a465b663a9e86b9d08b05d32dd386be95c546
```

This validates the local references; it does not implement CHD access in the app. Direct CHD reading remains milestone 5. The ISO9660 structure reference is [ECMA-119](https://ecma-international.org/wp-content/uploads/ECMA-119_4th_edition_june_2019.pdf); the existing Python readers and real-disc comparisons cover the game's directory layout.

Private evidence is in `../refs/patch-research/extractor-2026-11-05/`, including `disc-parity.json` and the extracted BIN/CUE. No game bytes are included in this record.

## Task 1: ISO/BIN reader

The new pure JVM `:extractor` module detects cooked ISO9660 and MODE2/2352 BIN by contents, reads files by path or LBA, loads the executable's 2,382-entry sector table, and validates the boot target plus executable size/hash. Directory enumeration uses the path table, including directories not linked from a recursive root walk. The caller retains its `ByteSource` after `checkGame`; closing a `Disc` closes its source.

Metadata, path, record-count and allocation bounds reject malformed images. A single read is limited to 64 MiB; audio/video extraction is outside scope. Sources are read-only. No Android application dependency or UI behavior changes in this task.

Validation, using the Android Studio JDK from `Android/`:

```sh
DW3_ISO=/path/to/original.iso DW3_BIN=/path/to/original.bin \
  ./gradlew :extractor:test --rerun-tasks --offline --console=plain
```

**22 tests passed with no failures, errors or skips.** They include synthetic malformed/truncated images, the real ISO and BIN, a wrong executable hash with the correct executable length, the real S331/S206 file-table lookups, and 1,000 equal 4,097-byte reads across sector boundaries. The initial unresolved-class compilation failure was recorded before implementation. The test task tracks both disc environment variables as inputs; without them the two real-disc tests are explicitly skipped.

One independent Gradle invocation failed during startup with `No space left on device` while macOS reported approximately 3.8 GiB free. A retry and the implementer's final rerun succeeded without cleanup or code changes for that error.

An independent review and a separate 22-test rerun approved this task without pending defects.

Rollback boundary: remove `Android/extractor`, its `settings.gradle.kts` inclusion, and this task's documentation. The existing app, frontend, game session and saves are unaffected.

## Task 2: numeric stage data and references

`generate_extractor_data.py` reuses the existing EU stage parser and writes only whitelisted setup values. The gzip resource contains 294 stages and 9,542 objects; values are integers, nulls and containers. No stage names, source paths or notes are carried across. Three stages (260, 415, 815) have no `mapFile`. The Kotlin data classes expose the fields used by the existing renderer.

The compact JSON is 1,603,760 bytes and its deterministic gzip is 76,499 bytes, SHA-256:

```text
5d6ceb4561316ab3eb42e668fdae27d6cf20f047bae82db5ad8bdc49d9c5fe4f
```

The missing `env.S` alias is fixed. `render_all.py` writes into `DW3_OUT/render`; `DW3_REFERENCE` for parity tests points there. A complete Python run produced **237 PNGs, zero failures, 197,738,496 pixels in 58 seconds**. The local references occupy 192,917,078 PNG bytes and 32,494,788 WebP bytes. Direct runs of maps 206 and 331, without the earlier wrapper, produced identical PNGs.

The same real-disc Gradle command now passes **25 tests**, zero failures, errors or skips. A missing-class red run preceded implementation. Independent review repeated the tests, compared every stage against the Python parser, regenerated the gzip byte for byte, and approved the task. Private logs and summaries are under `reference/` in the evidence directory above.

Rollback boundary: remove the generated stage resource, data classes/tests and generator; revert their Gradle serialization dependency and the Python environment/documentation changes. The disc reader and the playing app remain independent.

## Task 3: Kotlin map rendering

The JVM module now decodes TIM, RLEN and offset-table archives, composes map tiles and stage sprites in PS1 ordering-table order, applies 15-bit color modulation and semitransparency, and exports RGBA PNG without Android dependencies. The renderer exposes representative stage modes for each map pack; a mode is not necessarily its pack number.

One map is rendered at a time. The framebuffer is an `IntArray`, converted from 15-bit colors to ARGB in place, and tile draw commands share their texture arrays. Bounds include four million output/aggregate texture pixels, eight MiB per map resource, and primitive/drawing budgets. These limits cover the original game and reject unbounded work from damaged resources.

Independent review identified three silent failure cases, now rejected with explicit errors: TIM blocks shorter than their declared pixel dimensions, incomplete/misaligned map grids, and missing declared map/image/sheet references. Synthetic fixtures cover each case. All 472 unique VRAM TIMs (704 blocks) across the 294 stages have complete payloads; compatibility does not require zero filling.

Verification with `DW3_ISO`, `DW3_BIN` and `DW3_REFERENCE`:

- **39 tests passed**, zero failures, errors or skips, in an independent successful Gradle run.
- **All 237 maps, 197,738,496 pixels, match the Python PNG references exactly.** The independent parity loop took 6,253 ms on this Mac, including reference PNG decoding and comparison; this is not an Android extraction time.
- Without `DW3_*`, **35 tests passed and four real-disc/reference tests were explicitly skipped**.
- Initial missing-class failure, final XML, parity timings and review results are saved outside Git in `maps-*` under the evidence directory.

The intermittent macOS disk-space error recurred while writing HTML reports after tests. The independent run completed successfully. The previous release APK was preserved and hash-verified in the build/session evidence directory, then only the reproducible `Android/app/build` tree was removed to recover space for the debug build.

Rollback boundary: remove the image/map renderer and its tests and the `DW3_REFERENCE` Gradle test input. The disc reader, numeric stage table and playing application remain usable independently. Hardware calibration is the next task.


## Task 5: sprites, item icons and cards

The extractor now reads field standing animations, their mirrored directions, menu icons and card art/headers. It reconstructs the native card frames and AP/HP digits from `STCDSPCS` (file 0x63e) and `STCDSPTM` (0x642), including the compressed 16-bit textures. No fallback frame artwork or extracted digit pixels are embedded.

The factual `characters.json` contains 309 actor references, 39 aliases (covering 40 upstream pictures) and 24 icon frames: 5,454 bytes, SHA-256 `3db8c0c0940a8e8d699a72404e6268d25ccda58ec109e2ae510969b048003113`. Two generator runs produced identical bytes; `stages.json.gz` remained unchanged. Field/model archives with internal empty offsets are handled separately from the existing map archive variant; the shared map parser is unchanged.

**Independent review approved all ten files with zero findings; its separate run passed 11 tests, no failures, errors or skips, `BUILD SUCCESSFUL`.** The initial missing-class red run and final independent XML/log are preserved under `sprites-cards/` in the private evidence directory.

- All **2,472 actor directions and 24 icons match the current PNG pixels exactly**.
- The 39 aliases cover the 40 tamer/NPC/boss/store/Duel Island PNGs. Five edited/different-frame upstream tamers use a standing frame of the same character: Bob/Genji → 57, Natsumi/Tomomi → 48, Wong → 55. These are explicit visual replacements, not claims of exact upstream parity.
- All **314 card artworks** meet mean RGB difference ≤0.5/255 individually; aggregate mean is approximately 0.27335/255, maximum channel difference 1. The mask covers nontransparent TIM texels; transparent art texels belong to the frame. Every AP/HP header matches the database. Card IDs are 1000 + disc index; 1314 and 1315 are unused blanks. The legacy Volcanic Cannon/Gatlin filename mismatch is handled only in the reference test.
- Native frame colors 1/2/3/4/6 differ by at most 1 per visible RGB channel from upstream. The disc's black frame (color 5) is visibly darker: mean approximately 15.4/255 over frame pixels, maximum 33. The tests report frame metrics separately from the artwork assertion and do not claim exact frame parity. One or two corner alpha pixels per card also differ from the edited upstream PNGs.
- The handheld currently uses a separately edited, centered AP/HP strip. Generated cards restore the native digit positions, including spacing for one-digit values. A before/after contact sheet was inspected and is retained privately.

Only the current sprite/card archive is retained in their respective readers. Input sizes, offsets, headers and VRAM bounds are validated; output clipping bounds the work for malformed menu sheets. A test JVM property locates the checkout's reference PNGs without hardcoded developer paths.

The Mac's intermittent ENOSPC error also affected a review shell before Python started. After preserving the probe APK by a hash-verified hard link, only the reproducible app build directory and the generated raw BIN were removed. The original ISO and CHD, BIN/CUE parity results and all test evidence remain; `original.bin` can be regenerated with `chdman extractcd` before another raw-BIN test.

Rollback boundary: remove the sprites/cards classes and their tests, `characters.json` and the generator's `characters()` addition. The disc/map renderer and playing APK are unchanged. The new test-only repository-root property can be removed with these checkout-reference tests.


## Task 6: five-language text decoder

`GameText` decodes the original European language tables using a 358-code charset and checked offsets. It bounds file size (2 MiB), line count (16,384), individual encoded length (65,536 bytes) and repeated/decoded expansion (4 MiB units). Unknown bytes become explicit hexadecimal escapes rather than crashes. `buildTexts` emits per-locale JSON containing messages and stable word references, caching source tables within one locale only.

The generator records 1,365 message paths and 2,066 word references. Most word keys use `table:line`; the two parts of `htlnam:17` use explicit `:part:0` / `:part:1` selectors, and speaker references use a speaker selector. German β→ß, whitespace and speaker punctuation/capitalization follow the previous generator exactly, including the legacy trailing-space case at Spanish `talk00:16`. These selectors contain coordinates, not copied game text.

`en-us-own-texts.txt` records the 91 unmatched routes within the game-related source files. This is narrower than all 517 unmatched legacy strings, which also include the app's own UI. The current UI JSON files remain in place until task 11. `generate_locales.py` now writes developer references to its supplied output directory rather than modifying frontend sources; its eight regenerated JSONs match the saved originals byte for byte.

**Independent review approved all nine files without findings. Eight tests passed independently, no failures, errors or skips, `BUILD SUCCESSFUL`.** Six synthetic tests cover accents/control codes, longest-code matching, unknown bytes and malformed or expanding offsets. Real-disc checks compare 195 tables / 54,165 lines across all five languages, all generated messages and the 2,066 stable words against the four existing translated snapshots using a private legacy-key bridge. English messages are checked against disc-derived coordinates.

One reference discrepancy is asserted explicitly: Spanish `traini:101` in ddw3's TOML retains literal escape sequences where the original disc decodes two digit codes. The remaining 10,832 Spanish lines and all 10,833 lines in each other language match. This exception neither weakens comparison of other lines nor changes the disc's bytes.

Independent in-memory regeneration reproduced charset.json (5,407 bytes) and text-keys.json (294,909 bytes) exactly, together with the 91-route list and four legacy reference sets. Initial red compilation, final/review XML and generation summaries are under `text/` in the private evidence directory. No decoded game strings or legacy-reference bridge were added to Git.

Rollback boundary: remove the new text package/tests, charset/key resources, generator and route list; restore the old output destination in `generate_locales.py`. The existing interface, playing APK, disc reader and graphics extractors remain independent.

## Task 7: fighters and battle backgrounds

The pure Kotlin rasterizer now reads skeletal poses, mesh primitives, texture pages, CLUTs, lighting and battle cameras from the disc. `FighterModels` exposes global fighter IDs: 172 enemy IDs and 44 evolution-form IDs, with no namespace collision. `StageModels` renders the 28 battle backgrounds at 640 × 480. There are no Android or new library dependencies.

The reference tool now supports partner FighterInfo entries as well as enemies. NumPy's paired matrix operations and scalar tail have different FMA behavior; the Kotlin implementation reproduces those operations explicitly. `Math.fma` is available from Android API 33, matching the app's minimum SDK. This establishes parity on the Mac JVM, not yet on Android ART's trigonometric implementation.

A general front-face area threshold of 1e-8 in both implementations removes numerically degenerate faces. Only form 59's reference crop changes, from 324 × 244 to 324 × 191, removing an empty margin caused by invisible faces. Its appearance is preserved. The original PNG and comparison are retained outside Git. All other 243 model outputs retain their pixels. Tests still use **zero pixel tolerance**; there is no special-case fighter ID or relaxed comparison. Visual acceptance of the new evolution artwork remains part of task 10.

Input sizes, textures, draw work and output sizes are bounded. Independent review identified aggregate expansion through aliased animation tracks and meshes. Shared per-model budgets now reject more than 65,536 animation keys or combined vertices/normals **before allocation**. The largest real models use 4,411 keys and 2,193 vectors. Two small synthetic fixtures exercise the aliased-offset failure cases.

The initial missing-class red run, numerical diagnostics and final proof are saved privately. The implementer's final run passed 14 tests (11 synthetic and three full real-disc parity tests). Independent review approved all seven changed files after corrections; its run passed **18 tests: 14 model tests plus four world/portrait tests**, no failures, errors or skips, `BUILD SUCCESSFUL` in four seconds. All **172 enemies, 44 forms and 28 backgrounds match the final Python references exactly**. A brief accidental overlap between the final implementer and reviewer Gradle invocations occurred; both completed successfully, and the reviewer preserved its own 18-test XML/log separately under `models-world-review-test-results/`.

Rollback boundary: remove the models package and its tests, and revert the partner-reference/degenerate-face changes in `enemy_models.py`. The ISO/BIN, map, sprite, card and text extractors remain independent. No installed app or save has changed.

## Task 8: world maps, portrait sources and scene inventory

`renderWorldMaps` reproduces both labelled and base maps directly from the disc's sheets, textures and marker tables. `partnerPortraits` provides the eight transparent standing sprites by numeric partner ID. No edited portrait, screenshot, fallback bitmap or copied town/spot array is embedded. The 8-bit world-map texture uses its original alternate CLUT, distinct from the map renderer's general texture setup.

The independent reviewer approved the four source/test files and the scene ledger after an input-bound correction: every world resource is checked against a 1 MiB maximum before reading, the overlay must include its complete tables, and exactly three sprite sheets are required before constructing any sheet objects. The independent combined run passed **four world/portrait tests**, no failures, errors or skips, within the 18-test run documented above. All four world PNGs and all eight actor-based portraits match their references exactly. A red missing-function run and the intermediate CLUT bounds failure were retained before the final green run.

The detailed [scene ledger](extractor-scenes.md) lists the 18 upstream composite pictures and the chosen app-panel replacements for task 10. Cargo Tower and Deeper Crevice use special drawing rather than an `SnnnPACK` map. No empty scene API or fictitious extracted file is added. New panels, portrait/status composition and evolution artwork must be shown to the owner during task 10; their appearance has not yet been accepted or installed on the Thor.

Rollback boundary: remove `WorldMap.kt`, `Portraits.kt`, their tests and the scene ledger. Existing map/model/sprite/text code and the playing app remain unchanged.

## Task 9: complete, resumable extraction

`Extractor` runs one item at a time through a format-aware `AssetSink`. Images and JSON use extension-free logical keys; the manifest records actual PNG/WebP filenames. Producers are scoped to a single family, keeping caches out of later map/model work. A pause callback runs before each item, and progress includes existing resources. The unused `threads` parameter from the initial plan was omitted: a sequential worker meets the maximum-two-thread constraint without a pretend configuration option.

`PngSink` writes temporary files in the target directory and atomically renames them. It validates relative keys and canonical destinations, rejects traversal and symlinks escaping the root, ignores empty/temporary files, checks every manifest target and publishes the manifest only at successful completion. A failed write preserves the previous manifest. The CLI checks the original EU executable before creating an output directory and refuses an input image inside the output tree, so even an unusually named input such as `manifest.json` cannot be replaced.

Complete resume does not initialize image/text generators. A partially resumed field-sprite stream may decode earlier sprites while advancing to the missing one; it never rewrites completed files. Maps and models render only missing outputs. Resume trusts nonempty atomically promoted files, rather than hashing every file on each start; external modification of generated resources is outside this task's interruption guarantee.

The full ISO run produced **3,347 files, 3,409 manifest keys and 62 aliases**, totalling **186,536,235 bytes** excluding the manifest, in **28.77 seconds** on the Mac. Families: 2,472 actors, 314 cards, 39 NPC aliases, 24 icons, four world maps, eight portraits, 237 maps, 172 enemies, 44 evolution forms, 28 backgrounds and five locale JSONs. Shared WSTAGs and partner sprites use manifest aliases, without duplicate output files. No scene screenshots are fabricated. Output is kept in ignored `Frontend/game-dev/`.

The second full CLI run completed in **0.43 seconds**. SHA-256, byte length and nanosecond modification time of **all 3,347 resources were unchanged**, with identical manifest contents and zero temporary files. Independent review repeated the inventory and compared it against the before-resume snapshot: zero differences. Both timings include disc validation inside the CLI and exclude Gradle startup; they are Mac measurements, not Thor estimates.

The initial missing-class run was red; a subsequent path-validation regression exposed and fixed a `.` key accepted after adding its extension. **Nine synthetic tests passed**, including interruption/resume, final publication, pause/progress, actual sink extensions, aliases, failed promotion, traversal/symlinks and source/output isolation. An independent nine-test run passed with no failures, errors or skips, `BUILD SUCCESSFUL` in three seconds. All seven changed source/config files were approved with no pending findings. Logs, XML, output summaries and resume snapshots are under `orchestrator/` in the private evidence directory.

Reproduce from the repository root with the Android Studio JDK:

```sh
./Android/gradlew -p Android :extractor:run --offline --console=plain \
  --args='/absolute/path/to/original.iso /absolute/path/to/Frontend/game-dev'
```

Use absolute paths because the application task's working directory is the module. A single-track MODE2/2352 BIN uses the same command. Direct CHD reading, Android SAF/storage recovery, UI asset routing and full hardware extraction remain later milestones. The currently installed Thor app and its saves are unchanged.

Rollback boundary: remove the four root extractor classes and their test, the application plugin/main-class configuration and `Frontend/game-dev/` ignore rule. All individual decoder/renderers and the playing app remain independent.

## Integrated verification and visual study

After committing tasks 7–9, a clean rerun of the entire `:extractor:test` task completed in 13 seconds: **85 tests discovered, 84 passed, one explicitly skipped, zero failures or errors**. `DW3_ISO` and `DW3_REFERENCE` were supplied. The raw-BIN comparison was skipped because the regenerable BIN had been removed to recover disk space; its earlier complete sector comparison and 22-test run remain documented above. No disc-reader source changed in tasks 5–9. XML and the aggregate summary are saved in `extractor-all-final-test-results/` outside Git.

The owner reviewed the initial sprite/model comparison and requested visual alternatives before acceptance: filtered field sprites and angled, more cinematic model renders. Task 10 remains pending while those samples are evaluated. Raw extractor output and parity references stay unchanged during this study. Filters and lighting are intended to be baked during preparation, rather than recomputed while playing. No wiki artwork is downloaded or bundled.

Private sprite samples compare nearest-neighbour scaling, Scale2x and Lanczos using the same 32 × 32 disc sprite, with a newly drawn grid frame. The [Scale2x rules](https://www.scale2x.it/algorithm) preserve the source palette; a palette-containment assertion is included in the private prototype. This is a preview, not yet integrated UI or an Android performance measurement. The plan's owner review of portrait/evolution appearance remains open.

## Task 10 preparation: manifest reader and visual references

`loadGameAssets` reads format version 1 from `/game/manifest.json` and reports ready, missing or outdated. It validates the complete manifest before publishing any entries and clears previous entries on missing, invalid or outdated input. Keys and filenames reject traversal, absolute/external paths, percent escapes, backslashes, control characters and invalid UTF-16. `gameAssetUrl` encodes individual filename segments, supports aliases and root world-map keys, and returns null for missing resources. No fallback images, new dependency or UI wiring is introduced by this unit.

The missing-module test was red before implementation. **Independent review approved both files with no findings; its focused run passed 9/9 tests.** `npx vue-tsc --noEmit -p tsconfig.app.json` passed, and the root integration run `npm run test:handheld` passed **25/25 tests across four files**. Existing Vite `glob` deprecation warnings remain in the unchanged image catalogue. A private harness resolved all **3,409 keys** of the actual extracted manifest to their exact encoded URLs. Logs are saved under `game-assets/` in the private evidence directory.

The visual sheets now include the actual app portraits/illustrations in their first column, followed by the disc-derived alternatives. Sprite samples compare raw pixels, Scale2x and Lanczos; model samples compare frontal, three-quarter and softly lit three-quarter views. Independent inspection reproduced Scale2x, checked the camera/normal transforms and confirmed complete silhouettes and transparent margins in the model previews. The frontal previews match the raw extractor outputs. Camera and supersampling both change in the three-quarter sample, so this is a presentation comparison rather than an isolated filter benchmark. All prototypes remain outside Git and the APK. Owner acceptance is pending; task 10 is not complete.

Rollback boundary: remove `game-assets.ts` and its test. No existing caller, asset catalogue, Android server, installed app or save depends on this preparatory unit.

### Visual choice: original pixel grid with internal shadow tones

The owner clarified that sprite shading means deepening the existing dark regions inside the pixel art. An initial exterior-shadow interpretation was corrected; that private sheet is not the selected appearance. The accepted portrait direction is the original pixel grid with **moderate internal shading**, without Scale2x/Lanczos smoothing, an added outline or an exterior shadow.

The private `sprite-prototype-v3/tones.py` applies a pointwise luminance-based adjustment to the 32 × 32 source. The moderate strength is 0.18; a 0.36 alternative was shown for comparison. It preserves alpha, pixel positions, highlights of luminance at least 170 and near-black linework at most 16; all channels share the same multiplier. RGB values in shadow regions deliberately change. Integer nearest-neighbour enlargement is used only to present the samples. This is a tonal treatment of existing colours, not geometry-aware relighting of a 2D sprite.

Independent review reproduced all 12 comparison images from four source sprites and confirmed the exact tone formula, unchanged alpha/highlights, unchanged pixels outside the figure in the panels and preserved source hashes. A separate root check applied the selected moderate tone to all eight partner portraits and confirmed their dimensions, alpha, highlights and source-file hashes. Results and selected PNGs remain in the private evidence directory; raw `game-dev` output is unchanged.

For 3D evolutions the owner selected a real camera rotation of **+25°**, opposite the initial −25°, keeping pitch +6°, the idle pose and 4× supersampling. The second study separates the camera change from stronger internal lighting. Independent review confirmed the rotation and normal transforms, equal alpha across the 12 new renders, transparent margins and unchanged baseline hashes. Its lighting has not been accepted: the owner requested another iteration with **warm key light and a cool edge light**. The final illumination, production implementation, UI captures and Thor measurements remain pending. No new app has been installed.

## Task 12 preparation: read-only release audit

`Tools/audit_release.py` scans a folder, APK/ZIP or reachable Git history (`--history`). It rejects images outside explicit `icons`/`ui` directories or Android launcher resources, inline PNG data, known normalized text samples, ISO/CHD files and BIN files larger than 1,000,000 bytes. Image signatures also catch common formats with renamed extensions. JSON values and JavaScript literals are decoded without evaluation, including Unicode/hex escapes, and normalized with NFKC, casefold and collapsed whitespace before SHA-256 comparison. `audit_samples.txt` contains only **160 unique hashes**, selected deterministically as 32 per language from private generated text values of 32–512 normalized characters; no game text is embedded or echoed to logs.

The scanner reads at most 16 MiB per blob and 512 MiB in total, with explicit entry, metadata, directory-depth and string limits. Exceeded or unscanned input fails rather than reporting a clean result. ZIP processing is bounded before constructing Python's archive index; no members or historical blobs are extracted to disk. Symlinks are reported without following them. History is read without checkout, filters, fetch, ref updates or temporary blobs; Git 2.49 or newer is required. Folder scanning excludes `.git`; history scans all refs but does not include the working tree, index, reflogs or unreachable objects, so a publication review must also audit the release folder.

Independent review found and closed two boundary errors: a ZIP64 locator could bypass the classic ZIP preflight even without sentinel fields, and a shallow clone could report a clean incomplete history. ZIP64 is now rejected before `ZipFile`, including with a maximum-length comment; shallow history is rejected before `git log`, without fetching. Both reproductions were red before their corrections. **The final independent run passed 15/15 synthetic tests in 1.139 seconds, with no pending findings.** Tests also cover traversal, renamed images, escaped/minified strings, Unicode normalization, preservation of inputs, deleted historical content and resource limits.

The original playing APK (72,833,765 bytes) fails as expected: **1,507 IMAGE, 313 INLINE_PNG, 269 TEXT_SAMPLE and 12 LIMIT findings** across 1,895 entries. Its SHA-256 remains `89247919bb9a93011147bd9d04da93a2c6a16bdb272397130a9510613b642e88` before and after the final scan. Private logs and the summary are in `audit/` under the existing evidence directory. No installed APK or save was touched.

Reproduce the tool checks from the repository root:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest Tools.test_audit_release
python3 Tools/audit_release.py /absolute/path/to/release.apk
python3 Tools/audit_release.py /absolute/path/to/release-folder
python3 Tools/audit_release.py --history /absolute/path/to/repository
```

This is a heuristic guard, not proof of provenance or licensing. Dynamic/split strings, unknown encodings or compressed payloads can evade sample matching; allowlisted paths do not establish image ownership. Existing flattened Vite asset paths fail until the planned UI migration preserves the app-owned resource directories. **The new clean APK and real repository history have not been audited; task 12 remains incomplete until tasks 10 and 11 produce that APK.** Android runtime verification is N/A for this standalone developer CLI; its real boundary check is the preserved APK scan above.

Rollback boundary: remove `audit_release.py`, `audit_samples.txt` and `test_audit_release.py`. The extractor, manifest reader and playing app remain independent.


### Selected portrait and evolution presentation in Kotlin

The owner chose **moderate internal portrait shading** and **Cine equilibrado**, the middle column of the warm-key/cool-rim comparison. The more intense column was not selected. `shadePortrait` applies the fixed 0.18 tone curve with ties-to-even rounding to a new pixel buffer; raw `partnerPortraits` and field actors remain unchanged. Two synthetic tests cover representative colours, all grayscale levels, alpha, highlights, near-black linework and source-buffer preservation. All eight shaded portraits match the approved private PNGs exactly.

`renderEvolution` keeps the idle pose, rotates the camera +25° yaw/+6° pitch, applies the selected balanced warm/cool lighting, renders with fourfold supersampling, downsamples in premultiplied colour and adds eight transparent pixels on each side. This fixed presentation is used only for `digievolutions/`; enemies, battle backgrounds and the raw partner renderer retain their existing output. Camera rotation also transforms normals, and the shared RGB ceiling preserves the lighting colours.

The independent Python comparison covers all **44 forms / 4,039,540 pixels**: alpha and dimensions match exactly, and **8,851 pixels (0.219%)** differ by at most **one RGB level out of 255**. The four approved examples have 2, 416, 31 and 87 differing pixels respectively (IDs 5, 12, 144 and 59). Separate/batched floating-point accumulation can put a saturated channel one ULP below its ceiling; the new presentation test explicitly allows one RGB level rather than adding platform-specific arithmetic tricks. RAW parity remains zero tolerance for all 244 model outputs. The accepted PNGs and independent references stay outside Git and are located through `DW3_REFERENCE`, without a dated developer path in the tests.

Independent review found and closed one allocation issue before acceptance: the RAW size limit of 960 was too large for fourfold supersampling. The evolution API now accepts 1–480, and raster dimensions and their Long product are checked before allocating the pixel buffer. The 481 regression failed before correction, then passed alongside an actual render at 480. **The final independent full suite registered 93 tests: 92 passed, one skipped for the absent regenerable BIN, zero errors/failures**, in 16 seconds. Logs and XML are under `evolution-kotlin-validation/` in the private evidence directory.

The production orchestrator now applies the selected treatment to eight portraits and 44 evolutions. A real CLI regeneration of those **52 resources** completed in **1.31 seconds** on the Mac, retaining the other **3,295 files** byte-for-byte with unchanged sizes and modification times. All regenerated portraits/models match their respective exact/one-level reference limits; all eight sprite aliases resolve to the new portraits and the manifest is unchanged. The prior 52 raw files and manifest were copied and verified before replacement, outside Git under `style-integration/raw-before/`. This was a partial regeneration, not a new whole-extractor or Thor timing measurement. The format schema remains version 1; these private development resources were regenerated explicitly because resume otherwise trusts completed files.

Rollback boundary: restore the two production render calls, remove the portrait tone helper/test, `renderEvolution` and its fixed lighting/view code, fourfold downsampler/test and evolution tests. The raw extraction APIs, references, playing branch, installed app and saves remain independent. UI routing and captures are still task 10 work; no new APK was installed.


### Numeric field-map bridge

The UI's RAM field modes are not WSTAG numbers or pack-file indices. `generate_field_image_keys.py` reuses the EU main/extra table readers, checks duplicate WSTAG-handler rows before dictionary conversion, cross-checks both shared readers and validates each map file against the extractor facts. It emits only numeric mode→WSTAG pairs; shared-map WSTAG aliases are already present in the manifest.

The conservative `fieldMapImageKey(mode)` resolves **196 modes**. It returns null for unknown/noninteger inputs, **40 chapter-dependent modes whose map files differ**, and WSTAG entries 557/734 with no map. Cargo Tower mode 528 also returns null because it has a special handler outside the WSTAG table. Of the 159 catalogue modes, 126 resolve, nine are chapter-ambiguous, two have no mapFile and 22 are outside the WSTAG table (including 528). This unit does not claim complete map coverage or choose the wrong chapter as a fallback.

Independent review approved the three files with no findings: **6/6 focused tests**, TypeScript checks and deterministic regeneration passed. Generated SHA-256 is `22a78d934631badaba3cef34747bcf9c25f84edf574312b18ba426b036993ec2`; all 196 keys exist in the extracted manifest. The generator command is `PYTHONDONTWRITEBYTECODE=1 python3 Tools/FieldData/generate_field_image_keys.py --decomp /absolute/path/to/dw3_decomp`. Logs are under `field-image-keys/` outside Git. Runtime hardware check is N/A for this fact-table unit; actual frontend routing remains task 10 work.

Read-only follow-up found that main-quest step 133 already exposes progress >=45, while the game selects the extra chapter only at progress ==45. This is useful for the normal EU terminal progress, but it cannot establish equality for debug/modified values; `mapVariant` is `GAME.placeArg` and is unrelated. This unit keeps the conservative null instead of silently broadening its API. Exact chapter-aware map routing remains an integration decision.

Rollback boundary: remove the generator, generated `field-image-keys.ts` and its test. Existing callers, renderers and installed app do not depend on the table yet.

### Private resource serving and startup ordering

The overlay now awaits the validated manifest before mounting or signalling ready; the launcher remains independent of extraction. Android serves `/game/` from the dedicated private `filesDir/game/1` directory with AndroidX `InternalStoragePathHandler`, before its existing bundle handler. The same-origin navigation and network restrictions remain in place. Both activity/service callers now supply their Context. Missing resources remain a real 404. The extraction wizard that will populate this directory is still task 13; this intermediate APK is not installed on the Thor.

The Vite development middleware serves only PNG, WebP and JSON from the ignored local `Frontend/game-dev` directory. It checks decoded paths and canonical file/symlink targets, accepts GET/HEAD and prevents missing assets from becoming SPA pages. It does not copy development resources into the build. App-owned icons/UI and country flags retain explicit output directories, while other assets keep the ordinary output location; no image is inlined. The handheld page also references its existing party icon as a favicon.

Independent review approved the six server/startup files with no findings: **5/5 focused HTTP/output-naming checks, 48/48 handheld tests, Vue/Node TypeScript checks and ESLint passed**. A real development browser loaded the manifest and shaded portraits through `/game/`; complete screen captures are recorded with the consumer integration below. `:app:assembleRelease :app:lintRelease --offline --console=plain` passed. Lint reports five existing platform/resource advisories (InlinedApi, OldTargetApi, DataExtractionRules, ObsoleteSdkInt, MonochromeLauncherIcon), with no build failure. Logs are under private `frontend-routing/`.

The preliminary image-migrated build has 104 web files / 2,651,398 bytes, with 96 images all under icons/UI and no game directory copied. Its intermediate APK is 6,203,701 bytes, SHA-256 `dffbfaaed94c68235d7bb3b4b03cbfc50017c69530544a7c2ec738d4ad70383b`. The audit still fails as expected: 269 text-sample findings and eight string-scan limits await task 11; 25 compiled Android `res/` images also need provenance-aware handling in task 12. Zero findings are inline PNGs. This is not a clean-release claim or a hardware measurement.

Reproduce the web boundary checks with `npx vitest run --config vite.handheld.config.ts src/handheld/__tests__/game-dev.test.ts`, then `npm run build:handheld` from `Frontend`. Rollback boundary: revert startup waiting, Vite middleware/output naming, favicon and the Android handler plus its two callers and focused test together. The extractor and manifest reader remain independent; later manifest-only consumers depend on this server foundation. No installed app, saves, playing branch or remote refs changed.

### Existing catalogue callers through the manifest

`ImageCatalog` no longer imports game artwork. Existing partner, enemy, evolution and card names are resolved through their current database numeric identities; NPC/tamer/shop callers use the extractor's field-sprite aliases. URLs are read from the manifest on each getter call, after startup loading, rather than cached while modules initialize. Missing/unknown entries return null. Country flags remain app UI. The name bridge is explicitly temporary until task 11 migrates the text tables and their callers.

Map names are conservatively resolved through their existing field modes and the numeric bridge: all candidates must exist and point to the same physical image. The 120 name groups contain 35 unresolved groups, including chapter-dependent maps and non-map scenes; no older bundled image is used as fallback. The 18 special scene panels identified in `extractor-scenes.md` and exact chapter routing remain unfinished task 10 work.

The catalogue regression was red before implementation (four failures), then **5/5 focused tests** passed. Independent review reproduced that run, Vue TypeScript checks and the actual-manifest identity checks: eight partners, 172 enemy names, 44 evolutions and 314 cards resolve without conflicting name identities. Tests cover missing/stale manifests, unknown names, numeric battle stages, encoded NPC aliases and a known map. Browser captures show Central Park `/game/maps/330.png`, Tankmon `/game/enemies/76.png`, evolution models and a card loaded through this route without broken images. Evidence is private under `frontend-routing/`.

Reproduce with `npx vitest run --config vite.handheld.config.ts src/handheld/__tests__/image-catalog.test.ts` from `Frontend`. Rollback boundary: restore only `image.catalog.ts` and remove this focused test; the numeric field facts, extractor, manifest reader and server foundation remain independent. This intermediate rollback brings the old bundled catalogue artwork back, so its APK must not be treated as an audited release.

### Handheld image consumers and selected appearance

The handheld asset helpers now use numeric partner/card/actor IDs from the validated manifest, with no bundled game-art fallback. All callers in card battles, wiki lists/details, equipment, drops, evolution tabs and the waiting screen pass their existing IDs; generic people use actor 47 facing direction 1. App-owned SVG icons take priority over extracted item icons. Missing images retain the existing unknown-image behaviour.

`PortraitSlot` draws its grid, border and condition colours in CSS behind/around the same shaded 32 × 32 image, enlarged exactly 2× with nearest-neighbour pixels. Healthy, injured, debuffed and KO retain distinct frames; KO also uses grayscale. Independent review caught a grid layer initially drawn over the sprite; it was moved behind the image before acceptance. The seven focused helper regressions were red before the helper migration and passed afterwards; independent review approved the helpers and all affected callers, plus Vue TypeScript checks, with no remaining findings.

Browser inspection found the former illustration crop cut off the new full evolution models. Nodes now use 132 px with separate title/art/requirement rows and `contain`; the detail header reserves its title above the complete model. Locked forms retain their previous dimming filter. The 190 px fork artwork is scaled vertically to the 278 px occupied by two nodes plus their gap; changing only its image box would leave the SVG letterboxed. Independent CSS review approved the framing, and its low-priority connector alignment finding was addressed. A real browser measured both fork and branch bounds at 278 px.

Playwright reviewed the running app at **620 × 540** with actual private extraction files and the existing golden-state simulator: Equipo, Diario/Disponibles, Central Park, Tankmon battle, evolution tree/details, wiki card list/detail and Genji's card battle. No broken images or document horizontal overflow were found. Examples include portraits 0/7/6, map 330, battle stage 15, enemy 76, all 44 evolution models, card 1224 and Genji's 18-card folder. Fresh navigations after the favicon correction report **zero console errors/warnings**. Evolution detail stays inside its 244 × 404 panel and scrolls its body; cards with longer requirements have less model space but keep the complete silhouette. Captures and CLI evidence are outside Git under `frontend-routing/`, including `equipo-selected.png`, `evoluciones-selected.png`, `evolucion-dinohumon-selected.png`, `evolucion-greymon-selected.png` and `evoluciones-bifurcacion-selected.png`.

The integrated **48/48 handheld tests**, web build and Android release/lint checks passed. The final CSS-adjusted web bundle is **104 files / 2,651,591 bytes**, and the rebuilt APK is **6,203,745 bytes**, SHA-256 `e3f0b86187da576afbe3b0c6f24ba422c7d7057481e3be6f1249416b76596dfb`. This remains a local intermediate build: task 10 still has special scenes/chapter routing, task 11 retains game text, task 13 has no extraction wizard yet, and the new image consumers have not been tested in the Thor's WebView. KO/debuffed presentation was not exercised by the existing golden snapshots. No new APK was installed and nothing was pushed.

Reproduce helper tests with `npx vitest run --config vite.handheld.config.ts src/handheld/__tests__/assets.test.ts`; run `npm run dev:handheld -- --host 127.0.0.1 --port 5173` with a private `game-dev` extraction for the browser boundary. The captures use `?mock=antes-soldado-opcional-2-centro-administrativo-andar-2&screen=<screen>`, with existing `centralPark`, `battleAttacks` and `cardBattle` cases. Rollback boundary: revert `assets.ts`, its test and the affected handheld image callers/components together. The catalogue bridge, server foundation and Kotlin extraction remain independent, while reverting consumers reintroduces their previous bundled images. The installed playing app and saves remain unchanged.

### Owner refinement: close upper-body framing

The owner preferred larger upper bodies, explicitly accepted clipping, then requested higher framing for MetalGreymon. Both evolution slots now scale the existing image 3x around its top centre and move it up 48 CSS pixels. Original 132/112 px boxes, lighting and source files remain unchanged. Title/requirement grid layers sit above the transformed image and the existing containers clip overflow. Independent review approved the two-file CSS diff with scripts/templates unchanged. Browser samples at 620 x 540 include Greymon, MetalGreymon, Rosemon and Phoenixmon; actual detail sources 12/144 were checked. No broken images, console errors/warnings or document overflow were found. `npm run build:handheld` passed. Private captures are under `evolution-bust/`. This intentionally clips accessories; Android was not rebuilt or installed for the refinement. Rollback: revert only the two CSS changes. No extra tests/dependencies were added for this reversible presentation adjustment.

### Owner preview: pixelated evolution shadows

A hard CSS drop-shadow still followed the smooth render contour, so the owner requested visibly pixelated edges. `EvolutionArt` now keeps the original image over a separate black canvas silhouette. On image load only, the canvas samples the image to a maximum side of 48 pixels, thresholds alpha at 64 into 0/255 and scales with nearest-neighbour CSS rendering. Its 2 x 3 px offset becomes approximately 6 x 9 px after the existing 3x framing transform. Opacity is 0.85. Both tree nodes and the detail panel use the same component; keys by source URL recreate the mask when selecting another model. Existing locked-state filters and cropping remain intact. No extracted files, dependencies or per-frame rendering were added.

Independent review approved the three source files with no findings. TypeScript and `npm run build:handheld` passed. Browser assertions checked all 45 loaded masks for a 48 px maximum dimension, nonempty binary black pixels and pixelated scaling, with zero broken images or document overflow. Switching Rosemon to Greymon produced the correct source and new 35 x 48 mask. Diagnostic canvas readbacks produced Chromium's readback-performance advisory; a fresh navigation without diagnostic pixel reads had zero console errors/warnings. The final 620 x 540 preview is private at `evolution-bust/shadow-pixelated-final.png`; the earlier smooth-shadow trial remains alongside it. Visual acceptance and Thor WebView performance are still pending. Android was not rebuilt or installed. Rollback: restore the two image callers and remove `EvolutionArt.vue`; source renders and extraction remain independent. This is a local preview, with nothing pushed.

The owner found this first mask too coarse and displaced. The follow-up changes only the maximum mask side from 48 to 128 and the local offset from 2 x 3 to 1 x 1 px. With the existing 3x transform, Greymon's detail preview measures approximately 1.8 px per mask pixel and a 3 x 3 px shadow offset at 620 x 540. This is a visual target inspired by small pixel-art edges, not a claim to emulate a specific PS1 rendering mode. All 45 loaded masks have the new maximum dimension; no broken images or document overflow were found, and the locked filters remain intact. Independent review, TypeScript/web build and diff checks passed. The private follow-up capture is `evolution-bust/shadow-fine-pixels.png`. Owner acceptance and Thor performance remain pending; no Android build, install or push was made. Rollback for this refinement is the two constants in `EvolutionArt.vue`.

### Evolution portrait polish

All 44 extracted evolution renders have a manually checked head centre and height, expressed as image percentages and keyed by the existing evolution ID. The shared component uses those proportions in both the tree and detail panel. A first trial at 60% head height made Greymon too small, so the final crop targets 80% of the artwork slot with the head centre at 22%. Long accessories can clip while the face and shoulders stay prominent. Unknown IDs use the previous framing. The background adds a low contrast blue checker pattern fading around the bust. The silhouette is now near-black blue (`#020512`) at 0.68 opacity. It stays offset by 3 px on each axis and samples at roughly 2 CSS pixels per mask pixel after framing, capped by the source dimensions. The mask is still drawn only on image load.

Independent review found no issues and checked that the profile keys match all 44 PNGs. At 620 x 540 the browser rendered 44 distinct assets and 45 masks, with mask pixels measuring 1.97–2.01 CSS px vertically, preserved source aspect ratios, no broken images or document overflow, and correct locked MetalGreymon art. The full card montage and the actual Greymon and MetalGreymon views were visually checked. TypeScript, `npm run build:handheld`, the Impeccable scan and `git diff --check` passed. Captures are private under `evolution-bust/portrait-polish-*.png`. Thor WebView performance and appearance remain pending. Rollback is limited to the shared portrait component, its two callers and the framing table.

### First-run ISO and BIN preparation

The Android launcher opens preparation when the private game pack is absent. Android's document picker grants read-only access to an original PAL ISO or single-track BIN. A separate foreground process validates the disc, fingerprints it for resume and publishes the manifest only after all seven stages finish. Text in the five PAL languages, 314 card records, 443 techniques and 55 forms now come from the disc. Inherited reference catalogs and fallback text still ship in this build.

The setup screen uses the handheld kit's frame SVGs, icons and Press Start 2P font. The disc format and serial use the same thin frame as data rows. Run `python3 Design/build_android_setup.py` with Pillow and `fontTools[woff]` to refresh the native assets. Regeneration reproduced every export byte-for-byte. The violet-to-magenta loading bar follows the [original player registration screen](https://github.com/ReGame-Labs/dw3_decomp/blob/main/src/menus/player_name/confirm.c#L127). It shows the completion of seven equally weighted stages and reaches 100% after publication.

The Gradle checks passed with 764 core, 95 extractor and 53 handheld tests, debug and release builds, and zero lint errors. Extractor tests exercised a real ISO and a BIN obtained from CHD on the Mac. On the Thor, the release build displayed the setup in Spanish, English, French, German and Italian, rejected a bad image and prepared 3,411 resource keys in about 39 seconds. After a forced stop during a separate run, resume reused 3,273 completed files. The connection chooser opened after setup, the overlay attached to the lower screen and a relaunch skipped preparation. Evidence is private under `setup-thor/`. Low-storage behavior still needs a device check. The live CHD session is recorded below.

The APK audit remains open. The scanner flagged 32 images under optimized Android resource paths and reported six incomplete scans of literals in the bundled JavaScript. The repository history still needs its audit.

After testing, the Thor's playing APK was restored and its SHA-256 matched the initial backup. The test disc and redundant packs were removed. One 50 MiB private pack remains for the next preview build.

### Direct CHD preparation

The assistant identifies CHD by its header and opens the selected document's read-only descriptor with libchdr at `47d794151b2b64f6aba60304e6480f4f5a2bc24b`. The JNI reader holds one decoded block and presents the 2,352-byte Mode 2 frames to the existing BIN reader. The extractor retains the same original PAL validation. The physical CHD fingerprint identifies resumable preparation. The app supports standalone v5 images with one raw Mode 2 track and no gaps, and packages the native library and its notices for ARM64.

The [host parity test](../../../Android/app/src/test/native/ChdNative.java) passed 1,000 raw sector comparisons distributed across the original disc, a read crossing a block boundary and rejection of a truncated header. To reproduce, configure the app's CMake project with the host JDK's JNI include directories, compile the Java test, and invoke `dev.cricamfe.digivice.setup.ChdNative <CHD> <BIN>` with the built library on `java.library.path` and `--add-opens=java.base/java.io=ALL-UNNAMED`. The ARM64 library on the Thor independently produced the same SHA-256 for those 1,000 sectors from the existing ROM file. The 95 extractor tests passed with the ISO, BIN and parity references. APK builds and Android lint completed with zero errors.

The release APK rejected a truncated CHD in the Android document picker and completed preparation from the original CHD in an isolated ARM64 emulator. It published 3,411 keys in about 138 seconds and Continue opened the connection chooser. A fresh ISO run on that same emulator produced exactly the same SHA-256 for all 3,350 output files. The first Thor run verified the native read and restored the previous playing APK.

On 2026-11-06 the release APK completed the full setup on the AYN Thor. Android's document picker granted Digivice read access to the existing PAL CHD in `ROMs/psx`. The app prepared 3,411 resource keys in about 45 seconds. Continue opened the connection chooser, which also appeared directly on relaunch. All 3,350 output files matched the Thor's previous ISO run by SHA-256. The original CHD checksum also matched the pretest value. The CHD release APK remains installed with the generated pack active. The previous ISO pack and APK were kept as backups. Logs and checksum lists are private under `chd-thor/`.

A live Thor session used that exact CHD with SwanStation and RetroArch nightly `1793667751`. The lower-screen Party view matched the RAM readings for the three party slots, Agumon's level and HP, and the Bits total. The player marker followed movement inside Amaterasu Inn 1F. On entering Amaterasu City, the RAM map ID changed from `0x0279` to `0x0270` and the lower screen loaded the city map. The build with the Party level layout fix reattached to the same running RetroArch session and displayed the complete level row on all three portraits, including after switching selection. Screenshots and RAM responses are private under `chd-thor/live-thor/`.

The APK audit still reports 32 optimized Android image paths and six incomplete JavaScript literal scans. It reports no disc image in the APK. Rollback is limited to the CHD commit. The Kotlin ISO/BIN reader and the resource pack format are unchanged.

A synthetic check compiled the production map validator with Clang. It accepted a chain of 64 self-references and rejected 65, a two-block cycle and a self-loop.
