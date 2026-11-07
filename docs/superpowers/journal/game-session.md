# Game session verification

Verified locally on November 5, 2026. This refactor follows the [Android build change](android-build.md).

## Behavior

`GameSession` owns the selected source's concrete client, reader and polling loop. Android's `OverlayService` uses the session's lifecycle and port state without constructing or closing UDP clients.

`stop()` suspends the session and allows `start()` to resume it. `close()` retires it permanently, rejects subsequent restarts and discards later events. Events already queued on Android's main thread carry their originating session, so replacing the source cannot deliver the previous session's state to the new page.

Game readers, protocol parsers, polling intervals and display policy are unchanged. This adds no native gameplay capabilities.

## Checks performed

Three new session regressions exercise both source identities with a controlled loop: suspend/resume and initial replay; idempotent closure with callbacks during and after stopping; and source replacement with independent initial state and rejection of queued old events.

The complete local verification ran from `Android`, using the Android Studio JDK:

```sh
./gradlew :core:test :app:check :app:assembleRelease --offline --console=plain
```

- **764 Kotlin tests passed**, with zero failures, errors or skips.
- **16 frontend tests passed**, and frontend type checking and production generation passed.
- Android lint and the release APK build passed.
- The APK contains the new `GameSession` class and byte-identical fresh frontend HTML.
- An independent code review found no pending defects in the build and session changes.

An existing localhost UDP loss test failed once in a preliminary expanded run. It passed in isolation alongside the session and policy tests, and again in the complete suite above. No transport code was changed.

Local artifact: `Android/app/build/outputs/apk/release/app-release.apk`, 72,875,625 bytes. SHA256:

```text
ce93952bce16cdd7e69343308ae92f7ea7b81c27d1d50c62c3b676329140dd62
```

The APK is a private development build and is excluded from Git. The Thor was absent from `adb devices`; this APK was not installed or physically tested. The native game's APK was not changed.

## Rollback scope

Reverting `GameSession.kt`, `GameSessionTests.kt` and the corresponding `OverlayService.kt` changes restores source creation and connection ownership to the Android service. The build tasks are independent and remain usable. The architecture description, bridge record link and this verification record accompany that refactor.
