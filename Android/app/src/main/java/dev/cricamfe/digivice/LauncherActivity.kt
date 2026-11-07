package dev.cricamfe.digivice

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import dev.cricamfe.digivice.core.LauncherFlow
import dev.cricamfe.digivice.core.GameSource
import dev.cricamfe.digivice.overlay.AppWebViewClient
import dev.cricamfe.digivice.overlay.OverlayService
import dev.cricamfe.digivice.overlay.PAGE_BACKGROUND
import dev.cricamfe.digivice.overlay.WebBridge
import dev.cricamfe.digivice.overlay.createHandheldWebView
import dev.cricamfe.digivice.setup.GameAssetsStore
import dev.cricamfe.digivice.setup.SetupActivity

/**
 * Starts the overlay service and finishes, as long as the app may draw over other apps. If it may not, it stays on the
 * upper display with the first-run page (the handheld page with `?launcher`, in a WebView at the same scale as the
 * overlay's) explaining the permission and how to hide or exit; its button opens the system screen where it is granted.
 * Back from there with the permission given, it starts the service and finishes. [LauncherFlow] decides which.
 */
class LauncherActivity : Activity() {
    private val flow = LauncherFlow()
    private var awaitingNotifications = false
    private var awaitingPreparation = false
    private var webView: WebView? = null
    private var bridge: LauncherBridge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        awaitingPreparation = savedInstanceState?.getBoolean(SAVED_PREPARATION) == true
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            awaitingNotifications = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            proceed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // The foreground service runs without the notification permission, so go on either way.
        if (requestCode == REQUEST_NOTIFICATIONS) {
            awaitingNotifications = false
            proceed()
        }
    }

    /** Back from the system settings (or from anything else that covered the page): the permission may be there now. */
    override fun onResume() {
        super.onResume()
        if (!awaitingNotifications) proceed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(SAVED_PREPARATION, awaitingPreparation)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Uses the platform Activity result without an Activity dependency")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PREPARATION) {
            awaitingPreparation = false
            if (resultCode == RESULT_OK) proceed() else finish()
        }
    }

    override fun onDestroy() {
        bridge?.close()
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webView = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun proceed() {
        if (isFinishing || awaitingPreparation) return
        if (GameAssetsStore(this).active() == null) {
            awaitingPreparation = true
            startActivityForResult(Intent(this, SetupActivity::class.java), REQUEST_PREPARATION)
            return
        }
        when (flow.onCheck(Settings.canDrawOverlays(this))) {
            LauncherFlow.Action.StartOverlayAndFinish -> {
                chooseSource()
            }
            LauncherFlow.Action.ShowFirstRun -> showFirstRun()
            LauncherFlow.Action.None -> Unit
        }
    }

    private fun chooseSource() {
        val requested = GameSource.fromId(intent.getStringExtra(OverlayService.EXTRA_SOURCE))
        if (requested != null) {
            OverlayService.start(this, requested)
            finish()
            return
        }
        OverlayService.start(this, GameSource.RETROARCH)
        finish()
    }

    private fun showFirstRun() {
        try {
            val page = createHandheldWebView(this)
            val newBridge = LauncherBridge(::openOverlaySettings)
            // The page only reaches its own origin, because this interface exists for whatever the WebView shows.
            page.webViewClient = AppWebViewClient(this) {
                Log.e(TAG, "the first-run page's renderer is gone")
                finish()
            }
            page.addJavascriptInterface(newBridge, WebBridge.INTERFACE_NAME)

            val root = FrameLayout(this)
            root.setBackgroundColor(Color.parseColor(PAGE_BACKGROUND))
            // The window reaches under the system bars; the page gets what is left, so nothing of it is hidden.
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                WindowInsets.CONSUMED
            }
            root.addView(page, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            setContentView(root)

            webView = page
            bridge = newBridge
            page.loadUrl(AppWebViewClient.LAUNCHER_URL)
        } catch (e: RuntimeException) {
            // No WebView to draw the page (it is being updated or missing): do what the app did before it had one.
            Log.e(TAG, "could not show the first-run page", e)
            Toast.makeText(this, "Allow \"Display over other apps\" for Digivice, then open it again", Toast.LENGTH_LONG).show()
            openOverlaySettings()
            finish()
        }
    }

    /** The system screen where this package is allowed to draw over other apps. */
    private fun openOverlaySettings() {
        if (isFinishing || isDestroyed) return
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "no screen for the overlay permission", e)
            Toast.makeText(this, "Open Settings > Apps > Digivice and allow \"Display over other apps\"", Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val TAG = "DigiviceLauncher"
        const val REQUEST_NOTIFICATIONS = 1
        const val REQUEST_PREPARATION = 2
        const val SAVED_PREPARATION = "awaitingPreparation"
    }
}
