package dev.cricamfe.digivice.overlay

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import dev.cricamfe.digivice.setup.GameAssetsStore
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * Serves the app's page and private extracted resources at [HOME_URL]'s origin and lets the WebView reach nothing else.
 *
 * The bundle's ES module script does not load from `file:///android_asset`, so [WebViewAssetLoader] maps
 * `https://appassets.androidplatform.net/<path>` to `assets/www/<path>`; the definitions next to it in the assets are not
 * served. Because `window.DigiviceNative` is exposed to whatever the WebView shows, every navigation or request to
 * another origin is refused, and an unknown path on this origin is a 404 instead of a trip to the real host.
 */
class AppWebViewClient(
    context: Context,
    private val onRenderProcessGone: () -> Unit,
) : WebViewClient() {
    private val loader = WebViewAssetLoader.Builder()
        .setDomain(HOST)
        .apply {
            GameAssetsStore(context).active()?.let { generation ->
                addPathHandler("/game/", WebViewAssetLoader.InternalStoragePathHandler(context, generation))
            }
        }
        .addPathHandler("/", BundleHandler(context.assets))
        .build()

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        // data: and blob: carry their own bytes (small assets the bundler inlined, URLs the page made): nothing to fetch.
        if (request.url.scheme == "data" || request.url.scheme == "blob") return null
        if (!isOwnOrigin(request.url)) {
            Log.w(TAG, "blocked request to ${request.url}")
            return response(403, "Forbidden")
        }
        return loader.shouldInterceptRequest(request.url) ?: response(404, "Not Found")
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val allowed = isOwnOrigin(request.url)
        if (!allowed) Log.w(TAG, "blocked navigation to ${request.url}")
        return !allowed
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        Log.e(TAG, "WebView renderer gone (crashed=${detail.didCrash()})")
        onRenderProcessGone()
        return true // the app survives; the owner builds a new WebView
    }

    private fun isOwnOrigin(uri: Uri) = uri.scheme == "https" && uri.host == HOST && (uri.port == -1 || uri.port == 443)

    private fun response(status: Int, reason: String) =
        WebResourceResponse("text/plain", "utf-8", status, reason, emptyMap(), ByteArrayInputStream(ByteArray(0)))

    /** `/x/y` is `assets/www/x/y`; the only way out of `www` would be a `..` segment, which is refused. */
    private class BundleHandler(private val assets: AssetManager) : WebViewAssetLoader.PathHandler {
        override fun handle(path: String): WebResourceResponse? {
            val relative = path.ifEmpty { INDEX }
            if (relative.split('/').any { it == ".." || it == "." || it.isEmpty() }) return null
            val mimeType = mimeTypeOf(relative)
            return try {
                val encoding = if (mimeType.startsWith("text/") || mimeType == "application/json") "utf-8" else null
                WebResourceResponse(mimeType, encoding, assets.open("$BUNDLE_FOLDER/$relative"))
            } catch (e: FileNotFoundException) {
                null
            }
        }

        private fun mimeTypeOf(path: String) = when (path.substringAfterLast('.', "").lowercase()) {
            "html" -> "text/html"
            "js", "mjs" -> "text/javascript"
            "css" -> "text/css"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "jpg", "jpeg" -> "image/jpeg"
            "woff2" -> "font/woff2"
            "woff" -> "font/woff"
            "ico" -> "image/x-icon"
            else -> "application/octet-stream"
        }
    }

    companion object {
        const val HOST = "appassets.androidplatform.net"
        private const val INDEX = "handheld.html"
        const val HOME_URL = "https://$HOST/$INDEX"

        /** The same page for the launcher activity's WebView: with `?launcher` it shows the first-run permission page. */
        const val LAUNCHER_URL = "$HOME_URL?launcher"

        /** With `?launcher&setup` it shows the preparation wizard of the setup activity, which pushes its state. */
        const val SETUP_URL = "$HOME_URL?launcher&setup"
        private const val BUNDLE_FOLDER = "www"
        private const val TAG = "DigiviceWeb"
    }
}
