package dev.cricamfe.digivice

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * What the first-run page of [LauncherActivity] can ask for: `window.DigiviceNative.openSettings()`, the button
 * «Abrir ajustes». Only that activity adds it to its WebView; the overlay's WebView has its own interface
 * ([dev.cricamfe.digivice.overlay.WebBridge]) and does not know this method.
 *
 * The WebView calls it on a binder thread, so it only posts to the main thread. [close] makes a bridge that outlived
 * its activity harmless.
 */
class LauncherBridge(private val onOpenSettings: () -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var closed = false

    @JavascriptInterface
    fun openSettings() {
        main.post { if (!closed) onOpenSettings() }
    }

    fun close() {
        closed = true
    }
}
