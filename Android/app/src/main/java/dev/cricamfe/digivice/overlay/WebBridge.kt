package dev.cricamfe.digivice.overlay

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * The two-way channel with the page in [webView].
 *
 * - Page to native: `window.DigiviceNative.onReady()`, `hide()` and `exit()`. The WebView calls them on a binder thread,
 *   so each one only posts to the main thread, where [host] is told. The same three words as the notification's actions.
 * - Native to page: [deliver] runs the script that calls `window.__digivice.receive`, from the main thread, and only
 *   once the page said it is ready (before that `window.__digivice` does not exist). Events that come earlier are
 *   dropped on purpose: the page asks for the whole state when it is ready, see [Host.onPageReady].
 *
 * Only this class is exposed to the page; [install] must run before the first `loadUrl`, because the interface is
 * injected into the pages that load afterwards. [close] makes a bridge that outlived its WebView harmless.
 */
class WebBridge(private val webView: WebView, private val host: Host) {
    interface Host {
        /** The page has its handlers in place: send it everything it needs to show the current state. Main thread. */
        fun onPageReady()

        /** The user chose to hide the overlay from the page. Main thread. */
        fun onHideRequested()

        /** The user chose to exit from the page. Main thread. */
        fun onExitRequested()
    }

    private val main = Handler(Looper.getMainLooper())

    @Volatile private var closed = false

    /** Whether the page can take events. Read from the loop's thread to skip the work of serializing for no one. */
    @Volatile var isReady: Boolean = false
        private set

    fun install() {
        webView.addJavascriptInterface(this, INTERFACE_NAME)
    }

    @JavascriptInterface
    fun onReady() = postToMain {
        isReady = true
        host.onPageReady()
    }

    @JavascriptInterface
    fun hide() = postToMain { host.onHideRequested() }

    /** Whether the plain map is on the screen; read from the loop's thread. */
    @Volatile
    var mapOnScreen = true
        private set

    @JavascriptInterface
    fun setMapOnScreen(shown: Boolean) {
        mapOnScreen = shown
    }

    @JavascriptInterface
    fun exit() = postToMain { host.onExitRequested() }

    /** Runs [script] in the page, in order with the previous calls. Main thread only. */
    fun deliver(script: String) {
        if (closed || !isReady) return
        webView.evaluateJavascript(script, null)
    }

    /** Ignores everything from now on, whichever thread it comes from. */
    fun close() {
        closed = true
        isReady = false
    }

    private inline fun postToMain(crossinline action: () -> Unit) {
        main.post { if (!closed) action() }
    }

    companion object {
        const val INTERFACE_NAME = "DigiviceNative"
    }
}
