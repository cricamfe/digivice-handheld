package dev.cricamfe.digivice.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebView

/**
 * A full-screen, non-focusable `TYPE_APPLICATION_OVERLAY` window on [display] that hosts a WebView.
 *
 * It does not take input focus, so the game keeps the gamepad, but it still receives touches.
 * A window belongs to one display: if that display goes away, build a new instance for the new one.
 */
class OverlayWindow(context: Context, val display: Display) {

    private val windowContext: Context = context
        .createDisplayContext(display)
        .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)

    private val windowManager: WindowManager = windowContext.getSystemService(WindowManager::class.java)

    val webView: WebView = createHandheldWebView(windowContext)

    var isShowing: Boolean = false
        private set

    /**
     * Adds the window and loads [url]. Set the WebView's client and JavaScript interfaces first: the interfaces only
     * reach pages that load after they were added. If this throws (typically `BadTokenException`, when the overlay
     * permission is missing) the window is not added and the caller still owns the WebView: call [destroy].
     */
    fun show(url: String) {
        if (!isShowing) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            val params = WindowManager.LayoutParams(
                bounds.width(),
                bounds.height(),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                title = "Digivice overlay"
            }
            windowManager.addView(webView, params)
            isShowing = true
            Log.i(TAG, "overlay added on display ${display.displayId} (${bounds.width()}x${bounds.height()})")
        }
        webView.loadUrl(url)
    }

    fun hide() {
        if (!isShowing) return
        isShowing = false
        try {
            windowManager.removeViewImmediate(webView)
        } catch (e: IllegalArgumentException) {
            // The system already removed the window, for example with its display.
            Log.w(TAG, "overlay was already detached", e)
        }
    }

    /** Hides the window and frees the WebView. The instance cannot be used afterwards. */
    fun destroy() {
        hide()
        webView.destroy()
    }

    private companion object {
        const val TAG = "DigiviceOverlay"
    }
}
