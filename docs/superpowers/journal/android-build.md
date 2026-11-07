# Android build verification

Verified locally on November 5, 2026, on macOS with the existing locked dependencies.

## Behavior

Every Android asset-sync task depends on `:app:buildHandheld`. It runs the frontend type checker and Vite before copying the page into the APK. The frontend is rebuilt on every invocation, including when an earlier bundle exists. This also covers imports and generated assets outside the handheld entry point without maintaining a second dependency list in Gradle.

`:app:check` runs `:app:testHandheld`. Kotlin-only tests remain independent of npm. Dependency installation stays explicit: run `npm ci` in `Frontend` when preparing the checkout or changing its lockfile. Node and npm must be visible on Gradle's PATH.

## Checks performed

From `Android`, with the configured JDK:

```sh
./gradlew :app:syncDebugDigiviceAssets :app:testHandheld
```

Result: successful build, type checking and interface generation; **16 frontend tests passed**.

Four additional integration probes checked the actual task graph and generated files:

1. Added a temporary TypeScript source assigning a string to a `number`. `:app:syncDebugDigiviceAssets` failed with `TS2322`, before asset sync could run. The temporary source was removed immediately afterward.
2. Replaced the generated `dist-handheld/handheld.html` with a stale marker. The same Gradle task regenerated it and copied byte-identical fresh HTML to the generated Android assets.
3. `:core:test --dry-run` included neither frontend task.
4. `:app:check --dry-run` included `:app:testHandheld`.

The final release APK was inspected as a ZIP: its `assets/www/handheld.html` matched the newly generated frontend.

## Rollback scope

The build change consists of `Android/app/build.gradle.kts`, the `build:handheld` command in `Frontend/package.json`, the build instructions in the main and Android READMEs, and this record. Reverting it restores the previous manual frontend build without changing game readers, the session implementation or user saves.

## Limits

The Windows `cmd /c npm` launch path was not tested. No new dependencies or game assets were added. This verifies packaging and local checks, not installation or gameplay on the Thor.
