package dev.cricamfe.digivice.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * A WebView set up to show the handheld page at its scale, for both windows that do: the overlay on the lower display
 * and the launcher activity on the upper one. Whoever builds it still sets the [android.webkit.WebViewClient] (see
 * [AppWebViewClient]) and the JavaScript interface of its own, which must happen before the first `loadUrl`.
 */
@SuppressLint("SetJavaScriptEnabled")
fun createHandheldWebView(context: Context): WebView = WebView(context).apply {
    setBackgroundColor(Color.parseColor(PAGE_BACKGROUND))
    // 200 % of the physical width: 1240 px on the Thor lower display shows as 620 CSS px. The page is 620 x 540 CSS px
    // at most and takes the room it gets when the view is smaller (the upper display is not the same size).
    setInitialScale(INITIAL_SCALE_PERCENT)
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = false // the page comes from AppWebViewClient's https origin, not from file://
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        useWideViewPort = false
        loadWithOverviewMode = false
        textZoom = 100
        setSupportZoom(false)
        builtInZoomControls = false
    }
    isHorizontalScrollBarEnabled = false
    isVerticalScrollBarEnabled = false
    overScrollMode = WebView.OVER_SCROLL_NEVER
    if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

/** The colour of the page's background (`#000030` in `kit.css`), so no white shows before the page paints. */
const val PAGE_BACKGROUND = "#000030"

private const val INITIAL_SCALE_PERCENT = 200
