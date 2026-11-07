# Thor extraction benchmark

Measured on November 5, 2026, after independent review of the Kotlin renderer and debug probe. The first-milestone gate passed; this is not yet an end-to-end extraction measurement.

## Scope and method

This measures preparation of game resources outside gameplay. It is separate from the runtime CPU/battery baseline of the Digivice interface.

The Python reference report contains 237 maps and 197,738,496 pixels. Sort maps by `(pixel count, pack number)` and select 30 evenly spaced entries, including the smallest and largest. The sample contains 25,542,656 pixels, from 98,304 to 2,752,512 pixels per map. Representative stage modes:

```text
680 256 280 226 233 326 586 656 625 431
310 202 845 610 716 606 685 570 895 355
616 201 641 711 756 491 485 406 330 401
```

The debug-only `ExtractorProbeReceiver` renders **one map per broadcast** and encodes it as lossy WebP at quality 75. Thirty sequential broadcasts avoid keeping one receiver active for an entire batch. Each call uses `goAsync()` and finishes its pending result, returning timing/memory JSON to `am broadcast`. A slow individual map can still exceed the broadcast deadline; this probe is not the eventual production extraction service. See Android's [receiver lifecycle documentation](https://developer.android.com/reference/android/content/BroadcastReceiver).

Disc validation and file-table loading are reported separately from rendering and encoding. The estimate uses:

```text
sum(renderMs + encodeMs) / samplePixels × 197,738,496 × 83/48
```

The last factor is the earlier Python ratio of complete resource generation to maps. It is an estimate, not a measurement of the future Kotlin 3D renderer. The complete extractor must be timed again when implemented.

Memory records use `/proc/self/status`'s `VmHWM`, the process's cumulative peak resident size, plus Java used bytes and native heap bytes at the reported point. They do not measure interface memory or isolate the live size of one map.

Temperature sampling identifies CPU sensors by their `cpu-*` or `cpuss-*` type, with a 200 ms sleep between sweeps. Reading the sensors also takes time; the observed sweep rate is recorded below. On this Thor, **thermal_zone0 is `pa`**, so its value cannot substantiate the CPU threshold. Record the maximum CPU reading over the run.

The first-milestone gate remains: estimated total at most ten minutes, all 30 maps completed without OOM, and measured CPU peak below 70 °C. A failure changes the later 3D step to generation on demand, as agreed in the design.

## Device and preservation

The owner connected and released the AYN Thor for this test. ADB serial: `<serial>`. No emulator settings, memory cards or saves are part of the benchmark.

The installed APK was copied to the private evidence directory before installing the probe. The test uses only an explicitly named temporary ISO in the app's external files directory and WebP outputs in its cache. Remove those test files and restore the saved APK after the run, preserving application data with `adb install -r`.

Private evidence directory: `../refs/patch-research/extractor-2026-11-05/`. It contains the sample manifest, standalone local harness and the pre-test APK. Game images, benchmark outputs and APKs stay outside Git.

## Results and decision

AYN Thor, Android 13, one worker thread, one process (PID 8501), all 30 maps successful. Dimensions match the Python report; the renderer had already passed exact comparison of all 237 maps on the Mac.

| Measurement | Result |
|---|---:|
| Maps / pixels sampled | 30 / 25,542,656 |
| Rendering | 9.319 s |
| Android WebP75 encoding | 12.893 s |
| Rendering + encoding | 22.212 s |
| Disc validation, repeated by each broadcast | 3.227 s total |
| Loading, repeated by each broadcast | 6.550 s total |
| Wall time for 30 broadcasts | 34.455 s |
| Slowest broadcast work | 2.672 s |
| Peak process RSS (`VmHWM`) | 193,064 KiB (188.5 MiB) |
| Largest sampled Java used heap | 63,567,912 bytes (60.6 MiB) |
| Largest sampled native allocated heap | 15,903,936 bytes (15.2 MiB) |
| WebP output | 4,316,384 bytes |
| Maximum observed CPU temperature | 38.3 °C |
| Temperature coverage | 18 CPU sensors, 73 sweeps / 1,314 readings |
| Extrapolated map generation | 171.958 s |
| Extrapolated complete resources (`×83/48`) | 297.344 s (4 min 57 s) |

The sensors read at most 32.4 °C immediately before the run. The 200 ms sleep produced approximately one complete sweep per 0.47 s, including file reads. This short run does not establish the temperature of a continuous five-minute extraction. The future complete extractor must repeat the timing and thermal checks.

**Decision: proceed with the remaining extractor.** The estimate is below 600 s, no `OutOfMemoryError` occurred, and the observed CPU maximum was below 70 °C. Task 7 can retain the planned upfront model generation, subject to measuring the actual Kotlin model renderer. The historical Python ratio is not proof that it will meet the same estimate.

The probe deliberately revalidates and reloads each map; the full extraction session should validate/load once. It writes resources outside gameplay and does not measure gameplay FPS, battery use or the current Digivice session's memory.

## Verification and restoration

- Independent review approved the three debug-probe files with no outstanding defects.
- `:core:test :extractor:test :app:check :app:assembleDebug --offline --console=plain` passed: **764 core tests, 39 extractor tests, 16 frontend tests**, no failures or skips, Android lint and frontend type checking/build successful.
- Debug APK certificate matched the previously installed APK. It was installed with `adb install -r`.
- All 30 broadcasts completed with JSON success; the local harness asserted each mode and dimensions and retained timings, memory, CPU readings and the gate calculation.
- The explicitly named temporary ISO and all 30 probe WebPs were removed. The original APK was restored with `adb install -r`, preserving application data. Its installed bytes match the saved APK (SHA-256 `89247919bb9a93011147bd9d04da93a2c6a16bdb272397130a9510613b642e88`), and the probe receiver is absent.
- No emulator configuration, game memory card or save file was modified.

Private evidence: `probe-{build-summary,checks-summary,results,run,summary}.json`, `probe-checks.log`, `broadcasts.log`, `thermal.csv`, `device-probe-record.json`, `device-state.json` and `run-probe.py` in the evidence directory above. APKs and game files remain outside Git. Reproduce with the fixed temporary ISO location, the sample list above and thirty calls to:

```sh
adb -s <serial> shell am broadcast \
  -n dev.cricamfe.digivice/.ExtractorProbeReceiver --ei mode MODE
```

Rollback boundary: remove `ExtractorProbeReceiver.kt`, its debug manifest entry and `debugImplementation(project(":extractor"))`; the pure JVM extractor and playing application remain independent. Nothing has been published.
