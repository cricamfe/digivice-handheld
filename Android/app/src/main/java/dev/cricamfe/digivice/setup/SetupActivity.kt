package dev.cricamfe.digivice.setup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import dev.cricamfe.digivice.LauncherActivity
import dev.cricamfe.digivice.R
import dev.cricamfe.digivice.core.bridge.BridgeScript
import dev.cricamfe.digivice.core.setup.SetupAction
import dev.cricamfe.digivice.core.setup.SetupError
import dev.cricamfe.digivice.core.setup.SetupFlow
import dev.cricamfe.digivice.core.setup.SetupPage
import dev.cricamfe.digivice.overlay.AppWebViewClient
import dev.cricamfe.digivice.overlay.PAGE_BACKGROUND
import dev.cricamfe.digivice.overlay.WebBridge
import dev.cricamfe.digivice.overlay.createHandheldWebView

/**
 * The first-run preparation wizard, full screen on the upper display: the handheld page with `?launcher&setup` in a
 * WebView, like the first-run permission page of [LauncherActivity]. It is a separate window so the system file picker
 * and interrupted work do not depend on the overlay.
 *
 * The page only draws: every refresh this activity turns the [ExtractionService] state into a [SetupPage] with
 * [SetupFlow] and pushes it as `SetupState` when it changes. The page's single button (or A on the controller) comes
 * back as that page's [SetupAction]: choose a file, pause or resume the service, or continue to the connection chooser.
 */
class SetupActivity : Activity() {
    private val main = Handler(Looper.getMainLooper())
    private val preferences by lazy { getSharedPreferences(PREPARATION_PREFERENCES, Context.MODE_PRIVATE) }
    private val flow = SetupFlow()
    private var localFailure: SetupError? = null
    private var readyAtOpen = false
    private var current: SetupPage? = null
    private var pushed: String? = null
    private var pageReady = false
    private var webView: WebView? = null
    private var bridge: SetupBridge? = null

    private val refresh = object : Runnable {
        override fun run() {
            render()
            main.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.parseColor(PAGE_BACKGROUND)))
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        try {
            useOwnWebViewData()
            val page = createHandheldWebView(this)
            val newBridge = SetupBridge(::onPageReady, ::onPageAction)
            // The page only reaches its own origin, because this interface exists for whatever the WebView shows.
            page.webViewClient = AppWebViewClient(this) {
                Log.e(TAG, "the preparation page's renderer is gone")
                recreate()
            }
            page.addJavascriptInterface(newBridge, WebBridge.INTERFACE_NAME)
            val root = FrameLayout(this)
            root.setBackgroundColor(Color.parseColor(PAGE_BACKGROUND))
            root.addView(page, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            setContentView(root)
            hideSystemBars()
            webView = page
            bridge = newBridge
            page.loadUrl(AppWebViewClient.SETUP_URL)
        } catch (e: RuntimeException) {
            // No WebView to draw the page (it is being updated or missing); the work itself does not need one.
            Log.e(TAG, "could not show the preparation page", e)
            Toast.makeText(this, R.string.setup_unavailable, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        readyAtOpen = GameAssetsStore(this).active() != null
        refresh.run()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The file picker and the notification shade bring the bars back.
        if (hasFocus) hideSystemBars()
    }

    override fun onStop() {
        main.removeCallbacks(refresh)
        super.onStop()
    }

    override fun onDestroy() {
        main.removeCallbacks(refresh)
        bridge?.close()
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    /** A on the controller (or the centre of the D-pad, or Enter) presses the page's only button. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode in CONFIRM_KEYS) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) current?.let { perform(it.action) }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** After `setContentView`: the window has no decor view before it, and asking for its controller crashes. */
    private fun hideSystemBars() {
        window.decorView.windowInsetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun onPageReady() {
        pageReady = true
        pushed = null
        render()
    }

    private fun onPageAction(name: String) {
        SetupAction.named(name)?.let(::perform) ?: Log.w(TAG, "unknown page action $name")
    }

    private fun render() {
        val page = flow.page(
            service = ExtractionService.state,
            localFailure = localFailure,
            readyAtOpen = readyAtOpen,
            hasSavedDisc = savedUri() != null,
            file = preferences.getString(PREF_DISC_NAME, null),
            language = resources.configuration.locales[0].toLanguageTag(),
        )
        current = page
        webView?.keepScreenOn = page.keepsScreenOn
        val json = page.toJson()
        if (!pageReady || json == pushed) return
        pushed = json
        webView?.evaluateJavascript(BridgeScript.receive(STATE_EVENT, json), null)
    }

    /** Runs [action] only while it is what the page shows, so a late or repeated press does nothing. */
    private fun perform(action: SetupAction) {
        val page = current ?: return
        if (action != page.action || !page.enabled) return
        when (action) {
            SetupAction.Choose, SetupAction.ChooseOther -> pickDisc()
            SetupAction.Pause ->
                startService(Intent(this, ExtractionService::class.java).setAction(ExtractionService.ACTION_CANCEL))
            SetupAction.Resume, SetupAction.Retry -> savedUri()?.let(::startPreparation) ?: pickDisc()
            SetupAction.Continue -> {
                if (callingActivity == null) {
                    startActivity(Intent(this, LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                }
                setResult(RESULT_OK)
                finish()
                return
            }
        }
        render()
    }

    @Suppress("DEPRECATION")
    private fun pickDisc() {
        try {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, REQUEST_DISC)
        } catch (_: ActivityNotFoundException) {
            localFailure = SetupError.FileGone
            render()
        }
    }

    @Deprecated("Uses the platform document picker without an Activity dependency")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_DISC || resultCode != RESULT_OK) return
        val uri = data?.data?.takeIf { it.scheme == "content" } ?: return
        val old = savedUri()
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, read)
        } catch (_: SecurityException) {
            localFailure = SetupError.FileGone
            render()
            return
        }
        preferences.edit().putString(PREF_DISC, uri.toString()).putString(PREF_DISC_NAME, displayName(uri)).apply()
        if (old != null && old != uri) {
            runCatching { contentResolver.releasePersistableUriPermission(old, read) }
        }
        startPreparation(uri)
    }

    /** The file name the picker showed, for the page's source row. */
    private fun displayName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: RuntimeException) {
        Log.w(TAG, "no display name for the chosen file", e)
        null
    } ?: uri.lastPathSegment?.substringAfterLast('/')

    private fun savedUri(): Uri? = preferences.getString(PREF_DISC, null)?.let(Uri::parse)

    private fun startPreparation(uri: Uri) {
        try {
            localFailure = null
            startForegroundService(Intent(this, ExtractionService::class.java).setData(uri))
        } catch (_: IllegalStateException) {
            localFailure = SetupError.Interrupted
        } catch (_: SecurityException) {
            localFailure = SetupError.FileGone
        }
        render()
    }

    private companion object {
        const val TAG = "DigiviceSetup"
        const val REQUEST_DISC = 1
        const val REFRESH_MS = 250L
        const val STATE_EVENT = "SetupState"
        val CONFIRM_KEYS = setOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
        )

        /** The setup process has a WebView of its own; WebView refuses to share a data directory between processes. */
        private var ownWebViewData = false

        fun useOwnWebViewData() {
            if (ownWebViewData) return
            ownWebViewData = true
            WebView.setDataDirectorySuffix("setup")
        }
    }
}
