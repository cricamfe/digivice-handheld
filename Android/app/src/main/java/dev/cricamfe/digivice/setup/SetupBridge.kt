package dev.cricamfe.digivice.setup

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * What the preparation page of [SetupActivity] can ask for, as `window.DigiviceNative`: `onReady()` once it listens
 * for `SetupState`, and `setupAction(name)` when its button is pressed. Only that activity adds it to its WebView.
 *
 * The WebView calls it on a binder thread, so it only posts to the main thread. [close] makes a bridge that outlived
 * its activity harmless.
 */
class SetupBridge(
    private val onPageReady: () -> Unit,
    private val onAction: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var closed = false

    @JavascriptInterface
    fun onReady() {
        main.post { if (!closed) onPageReady() }
    }

    @JavascriptInterface
    fun setupAction(action: String) {
        main.post { if (!closed) onAction(action) }
    }

    fun close() {
        closed = true
    }
}
