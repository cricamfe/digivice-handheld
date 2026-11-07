package dev.cricamfe.digivice.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.widget.Toast
import dev.cricamfe.digivice.LauncherActivity
import dev.cricamfe.digivice.R
import dev.cricamfe.digivice.core.GameSession
import dev.cricamfe.digivice.core.GameSource
import dev.cricamfe.digivice.core.OverlayPolicy
import dev.cricamfe.digivice.core.Reachability
import dev.cricamfe.digivice.core.bridge.BridgeScript
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.memory.repositories.AssetDefinitions
import dev.cricamfe.digivice.setup.GameAssetsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service (`specialUse`) that runs a game session and shows its state in an overlay on the
 * presentation display.
 *
 * The core ([GameSession]) runs on an IO thread; each event it emits is turned into a script there and handed to the main
 * thread, which is where everything else happens: the [OverlayPolicy], the overlay and its WebView. The loop's store is
 * never read from here. [OverlayPolicy] says when the overlay is on screen and when to poll; the service only carries
 * out what it says:
 *  - The overlay appears when the game answers and goes away after five seconds without an answer. The service keeps
 *    running while it is away, so the session notices the game coming back.
 *  - Hide (the notification or the page) keeps it away until the user shows it again (the notification or opening the
 *    app), whatever the game does. Exit stops everything.
 *  - With the lower display off the polling stops. The game does not answer while the console sleeps.
 *
 * Hiding destroys the WebView. A new one asks for the state when its page is ready (`DigiviceNative.onReady`), which
 * the loop answers with an `InitialState` and the health at its next tick.
 */
class OverlayService : Service(), WebBridge.Host {

    private val main = Handler(Looper.getMainLooper())
    private var policy = OverlayPolicy()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var displayManager: DisplayManager
    private lateinit var powerManager: PowerManager
    private lateinit var session: GameSession

    private var overlay: OverlayWindow? = null
    /** Written on the main thread, read by the loop's thread to know whether a page could take an event. */
    @Volatile private var bridge: WebBridge? = null
    private var displayOn: Boolean? = null
    private var shownNotification: NotificationState? = null
    private var exiting = false
    private var resourcesReady = false

    private val displayListener = object : DisplayManager.DisplayListener {
        // The lower display is the only one that matters, and the listener is cheap: re-read it whatever changed.
        override fun onDisplayAdded(displayId: Int) = refreshDisplay()

        override fun onDisplayRemoved(displayId: Int) {
            // The system takes the window with the display. Drop our copy so it can be rebuilt.
            if (overlay?.display?.displayId == displayId) dropOverlay()
            refreshDisplay()
        }

        override fun onDisplayChanged(displayId: Int) = refreshDisplay()
    }

    /** `ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` catch a sleep that the lower display's own state would not report. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshDisplay()
    }

    private val tick = Runnable { evaluate() }

    /** Asks every second what the session's last request found of the game port, with or without anything on screen. */
    private val watchPort = object : Runnable {
        override fun run() {
            if (exiting) return
            session.portClosed?.let { policy.onPortState(it, SystemClock.uptimeMillis()) }
            if (policy.shouldExit) {
                Log.i(TAG, "Game source was closed: leaving")
                shutDown()
                return
            }
            main.postDelayed(this, PORT_CHECK_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        resourcesReady = GameAssetsStore(this).active() != null
        displayManager = getSystemService(DisplayManager::class.java)
        powerManager = getSystemService(PowerManager::class.java)
        createChannel()
        session = createSession(selectedSource(this))
        displayManager.registerDisplayListener(displayListener, main)
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
        refreshDisplay()
        main.postDelayed(watchPort, PORT_CHECK_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resourcesReady = GameAssetsStore(this).active() != null
        val requestedSource = GameSource.fromId(intent?.getStringExtra(EXTRA_SOURCE))
        if (requestedSource != null && requestedSource != session.source) {
            session.close()
            dropOverlay()
            policy = OverlayPolicy()
            displayOn = null
            session = createSession(requestedSource)
            refreshDisplay()
        }
        when (intent?.action) {
            ACTION_EXIT -> exiting = true
            ACTION_HIDE -> policy.onUserHide()
            else -> {
                policy.onUserShow()
                if (presentationDisplay() == null) {
                    Log.w(TAG, "no presentation display; waiting for one")
                    Toast.makeText(this, "No presentation display found", Toast.LENGTH_LONG).show()
                }
            }
        }
        // Started with startForegroundService, so promote to foreground whatever the action was.
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        shownNotification = notificationState()
        if (exiting) {
            shutDown()
            return START_NOT_STICKY
        }
        evaluate()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cleanUp()
        super.onDestroy()
        // The system would keep the process around, with its memory, in case it was opened again.
        Process.killProcess(Process.myPid())
    }

    // ---- the page asks (main thread) -----------------------------------------------------------------------------

    override fun onPageReady() {
        session.requestInitialEvents()
    }

    override fun onHideRequested() {
        policy.onUserHide()
        evaluate()
    }

    override fun onExitRequested() = shutDown()

    // ---- what the core says (loop thread) --------------------------------------------------------------------------

    private fun onGameEvent(event: Event, origin: GameSession) {
        val reachable = Reachability.of(event)
        // Serialize here, off the main thread, and not at all while no page could take it. A failure to serialize must
        // not escape: the loop would report it as a failure to compose the state.
        val script = try {
            if (bridge?.isReady == true) BridgeScript.receive(event) else null
        } catch (e: RuntimeException) {
            Log.e(TAG, "could not serialize ${event.type}", e)
            null
        }
        if (reachable == null && script == null) return
        main.post {
            if (exiting || origin !== session) return@post
            if (reachable != null) {
                val now = SystemClock.uptimeMillis()
                if (reachable) policy.onReachable(now) else policy.onUnreachable(now)
            }
            // Goes to the page that is up now; if evaluate() creates a new one below, that page asks for the state itself.
            if (script != null) bridge?.deliver(script)
            if (reachable != null) evaluate()
        }
    }

    // ---- carrying out the policy (main thread) ---------------------------------------------------------------------

    /** Brings polling, overlay, notification and the next countdown in line with the policy at this moment. */
    private fun evaluate() {
        main.removeCallbacks(tick)
        if (!resourcesReady) {
            session.stop()
            dropOverlay()
            return
        }
        val now = SystemClock.uptimeMillis()
        policy.onTick(now)

        if (policy.shouldPoll) session.start(scope) else session.stop()

        val display = presentationDisplay()
        if (policy.shouldShow && display != null) {
            if (overlay == null) showOverlay(display)
        } else {
            dropOverlay()
        }

        val notification = notificationState()
        if (notification != shownNotification) {
            shownNotification = notification
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        }

        policy.hideDeadlineMs?.let { main.postDelayed(tick, (it - now).coerceAtLeast(0)) }
    }

    /** Tells the policy whether the lower display is on, if that changed. */
    private fun refreshDisplay() {
        val display = presentationDisplay()
        val interactive = powerManager.isInteractive
        val on = display != null && interactive && display.state.isOn()
        Log.i(TAG, "lower display: ${display?.let { stateName(it.state) } ?: "none"}, interactive=$interactive -> ${if (on) "on" else "off"}")
        if (on == displayOn) return
        displayOn = on
        policy.onDisplayChanged(on)
        if (!exiting) evaluate()
    }

    private fun Int.isOn() = this == Display.STATE_ON || this == Display.STATE_ON_SUSPEND || this == Display.STATE_VR

    private fun stateName(state: Int) = when (state) {
        Display.STATE_ON -> "ON"
        Display.STATE_OFF -> "OFF"
        Display.STATE_DOZE -> "DOZE"
        Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
        Display.STATE_ON_SUSPEND -> "ON_SUSPEND"
        Display.STATE_VR -> "VR"
        else -> "UNKNOWN($state)"
    }

    private fun presentationDisplay(): Display? =
        displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull()

    private fun showOverlay(display: Display) {
        val window = try {
            OverlayWindow(this, display)
        } catch (e: RuntimeException) {
            Log.e(TAG, "could not create the overlay", e)
            return
        }
        val newBridge = WebBridge(window.webView, this)
        window.webView.webViewClient = AppWebViewClient(this) { onRenderProcessGone(newBridge) }
        newBridge.install()
        try {
            window.show(AppWebViewClient.HOME_URL)
        } catch (e: RuntimeException) {
            // Typically BadTokenException when the overlay permission is missing.
            Log.e(TAG, "could not show overlay", e)
            newBridge.close()
            window.destroy()
            Toast.makeText(this, "Could not show overlay: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        overlay = window
        bridge = newBridge
    }

    private fun onRenderProcessGone(gone: WebBridge) {
        main.post {
            if (bridge !== gone) return@post
            dropOverlay()
            if (!exiting) evaluate()
        }
    }

    private fun dropOverlay() {
        bridge?.close()
        bridge = null
        overlay?.destroy()
        overlay = null
    }

    private fun shutDown() {
        exiting = true
        cleanUp()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Stops the loop and drops every callback and window. Safe to call twice. */
    private fun cleanUp() {
        exiting = true
        main.removeCallbacksAndMessages(null)
        session.close()
        scope.cancel()
        dropOverlay()
        displayManager.unregisterDisplayListener(displayListener)
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered by an earlier cleanUp.
        }
    }

    private fun createSession(source: GameSource): GameSession {
        val definitions = AssetDefinitions(
            openAsset = { path -> assets.open(path) },
            listAssets = { path -> assets.list(path) },
        )
        // The position is read between cycles only while a page is there to draw it: not while the overlay is hidden, nor on
        // a screen other than the map.
        return GameSession.create(
            source,
            definitions::readText,
            definitions::listFiles,
            ::onGameEvent,
            positionWanted = { bridge?.let { it.isReady && it.mapOnScreen } == true },
            actorsWanted = { bridge?.mapOnScreen == true },
        )
    }

    // ---- notification ----------------------------------------------------------------------------------------------

    private enum class NotificationState { Showing, Waiting, Hidden, NeedsResources }

    private fun notificationState() = when {
        !resourcesReady -> NotificationState.NeedsResources
        policy.userHidden -> NotificationState.Hidden
        policy.shouldShow -> NotificationState.Showing
        else -> NotificationState.Waiting
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Digivice overlay", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val icon = Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel)
        val text = when (notificationState()) {
            NotificationState.Showing -> "Overlay on the lower display"
            NotificationState.Waiting -> "Waiting for RetroArch. Tap to show it now"
            NotificationState.Hidden -> "Overlay hidden. Tap to show it again"
            NotificationState.NeedsResources -> getString(R.string.setup_overlay_missing)
        }
        val open = if (resourcesReady) servicePendingIntent(ACTION_SHOW, 0) else PendingIntent.getActivity(
            this, 0, Intent(this, LauncherActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Digivice")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(icon, "Hide", servicePendingIntent(ACTION_HIDE, 1)).build(),
            )
            .addAction(
                Notification.Action.Builder(icon, "Exit", servicePendingIntent(ACTION_EXIT, 2)).build(),
            )
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, OverlayService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val ACTION_SHOW = "dev.cricamfe.digivice.action.SHOW"
        const val ACTION_HIDE = "dev.cricamfe.digivice.action.HIDE"
        const val ACTION_EXIT = "dev.cricamfe.digivice.action.EXIT"
        const val EXTRA_SOURCE = "source"

        private const val TAG = "DigiviceOverlay"
        private const val PORT_CHECK_MS = 1_000L
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1

        fun selectedSource(context: Context): GameSource = GameSource.fromId(
            context.getSharedPreferences("connection", Context.MODE_PRIVATE).getString(EXTRA_SOURCE, null),
        ) ?: GameSource.RETROARCH

        fun start(context: Context, source: GameSource = selectedSource(context)) {
            context.getSharedPreferences("connection", Context.MODE_PRIVATE).edit().putString(EXTRA_SOURCE, source.id).apply()
            context.startForegroundService(
                Intent(context, OverlayService::class.java).setAction(ACTION_SHOW).putExtra(EXTRA_SOURCE, source.id),
            )
        }
    }
}
