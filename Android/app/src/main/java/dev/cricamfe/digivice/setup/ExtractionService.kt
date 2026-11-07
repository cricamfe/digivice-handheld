package dev.cricamfe.digivice.setup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.system.ErrnoException
import android.system.OsConstants
import android.util.Log
import dev.cricamfe.digivice.R
import dev.cricamfe.digivice.core.retroarch.RetroArchClient
import dev.cricamfe.digivice.core.retroarch.RetroArchStatus
import dev.cricamfe.digivice.core.setup.ExtractionState
import dev.cricamfe.digivice.core.setup.SetupError
import dev.cricamfe.digivice.extractor.Extractor
import dev.cricamfe.digivice.extractor.disc.ByteSource
import dev.cricamfe.digivice.extractor.disc.GameCheck
import dev.cricamfe.digivice.extractor.disc.checkGame
import dev.cricamfe.digivice.extractor.disc.openDisc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException

internal class SetupFailure(val reason: SetupError, cause: Exception? = null) : Exception(cause)

/** Preparation and its activity share a process separate from the gameplay overlay. */
class ExtractionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var lastNotification = 0L
    /** The user paused (the page's button or the notification's action), so a cancelled run ends as [ExtractionState.Paused]. */
    @Volatile private var pauseRequested = false
    @Volatile private var destroyed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            if (job?.isActive == true) {
                pauseRequested = true
                state = ExtractionState.Cancelling
                job?.cancel()
            } else if (job == null) stopSelf(startId)
            return START_NOT_STICKY
        }

        // Keep even a cancelling job in charge of staging until it has closed all its files.
        if (job != null) return START_NOT_STICKY
        val uri = intent?.data
        if (uri == null || uri.scheme != "content") {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notifications = getSystemService(NotificationManager::class.java)
        pauseRequested = false
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.setup_title), NotificationManager.IMPORTANCE_LOW))
        state = ExtractionState.Checking
        startForeground(NOTIFICATION_ID, notification(state), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        var result: ExtractionState = ExtractionState.Failed(SetupError.Interrupted)
        val running = scope.launch {
            val client = RetroArchClient(timeoutMs = 120)
            val gate = GameGate(client)
            val checkpoint = {
                coroutineContext.ensureActive()
                gate.waitUntilIdle { coroutineContext.ensureActive() }
            }
            try {
                preparationLock.withLock { prepare(uri, checkpoint, ::report) }
                result = ExtractionState.Ready
            } catch (_: CancellationException) {
                // The staging generation stays available to a later run.
            } catch (e: Exception) {
                val reason = errorFor(e)
                Log.w(TAG, "Preparation failed: $reason", e)
                result = ExtractionState.Failed(reason)
            } finally {
                client.close()
            }
        }
        job = running
        // An immediately cancelled launch may never enter the worker at all.
        running.invokeOnCompletion {
            main.post {
                if (destroyed || job !== running) return@post
                if ((result as? ExtractionState.Failed)?.reason?.needsNewDisc == true) {
                    val preferences = getSharedPreferences(PREPARATION_PREFERENCES, MODE_PRIVATE)
                    if (preferences.getString(PREF_DISC, null) == uri.toString()) {
                        preferences.edit().remove(PREF_DISC).apply()
                        runCatching { contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    }
                }
                state = if (pauseRequested && result == ExtractionState.Failed(SetupError.Interrupted)) {
                    ExtractionState.Paused
                } else {
                    result
                }
                job = null
                Log.i(TAG, "Preparation finished: $state")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun prepare(uri: Uri, checkpoint: () -> Unit, progress: (ExtractionState.Progress) -> Unit) {
        DocumentSource(contentResolver, uri).use { document ->
            checkpoint()
            document.openChd().use { chd ->
                val source: ByteSource = chd ?: document
                when (checkGame(source)) {
                    GameCheck.Valid -> Unit
                    GameCheck.NotADisc -> throw SetupFailure(SetupError.NotADisc)
                    GameCheck.WrongGame -> throw SetupFailure(SetupError.WrongGame)
                    GameCheck.Truncated -> throw SetupFailure(SetupError.Truncated)
                }
                val hash = document.fingerprint(checkpoint)
                extract(source, hash, checkpoint, progress)
            }
        }
    }

    private fun extract(
        source: ByteSource,
        hash: String,
        checkpoint: () -> Unit,
        progress: (ExtractionState.Progress) -> Unit,
    ) {
        val store = GameAssetsStore(this)
        val staging = store.stage(hash)
        Log.i(TAG, "Working in ${staging.name}")
        val written = staging.walkTopDown().filter { it.isFile }.sumOf {
            if (it.name.startsWith('.') && it.name.endsWith(".tmp")) {
                it.delete()
                0L
            } else it.length()
        }
        val needed = maxOf(MINIMUM_HEADROOM, FIRST_RUN_HEADROOM - written)
        if (StatFs(staging.path).availableBytes < needed) throw SetupFailure(SetupError.NoSpace)

        // Preparation owns the document and the optional CHD reader.
        val borrowed = object : ByteSource by source {
            override fun close() = Unit
        }
        val disc = openDisc(borrowed) ?: throw SetupFailure(SetupError.NotADisc)
        val sink = WebpSink(staging)
        val manifest = disc.use {
            val extractor = Extractor(it, sink)
            extractor.run(checkpoint) { piece, done, total ->
                if (done == 0) Log.i(TAG, "Starting ${piece.name} ($total resources)")
                if (done == total) Log.i(TAG, "Completed ${piece.name} ($total resources)")
                progress(ExtractionState.Progress(piece.name, extractor.pieces.indexOf(piece) + 1, extractor.pieces.size, done, total))
            }
        }
        checkpoint()
        store.publish(staging)
        Log.i(TAG, "Published ${manifest.assets.size} resource keys; reused ${sink.reused} completed files")
    }

    private fun report(value: ExtractionState, force: Boolean = false) {
        if (destroyed || state == ExtractionState.Cancelling) return
        state = value
        val now = SystemClock.elapsedRealtime()
        if (force || now - lastNotification >= NOTIFY_EVERY_MS) {
            lastNotification = now
            main.post {
                if (!destroyed && job?.isActive == true) {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
                }
            }
        }
    }

    private inner class GameGate(private val client: RetroArchClient) {
        private var nextCheck = 0L

        fun waitUntilIdle(checkCancellation: () -> Unit) {
            if (SystemClock.elapsedRealtime() < nextCheck) return
            val previous = state
            while (true) {
                checkCancellation()
                when (client.status()) {
                    is RetroArchStatus.Playing, is RetroArchStatus.Paused -> {
                        if (state != ExtractionState.PausedForGame) {
                            Log.i(TAG, "Waiting for RetroArch to close its game")
                            report(ExtractionState.PausedForGame, force = true)
                        }
                        Thread.sleep(GAME_CHECK_MS)
                    }
                    RetroArchStatus.Contentless, RetroArchStatus.Unreachable -> {
                        nextCheck = SystemClock.elapsedRealtime() + GAME_CHECK_MS
                        if (state == ExtractionState.PausedForGame) report(previous, force = true)
                        return
                    }
                }
            }
        }
    }

    private fun notification(value: ExtractionState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, SetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this, 0, Intent(this, ExtractionService::class.java).setAction(ACTION_CANCEL), PendingIntent.FLAG_IMMUTABLE,
        )
        val progress = value as? ExtractionState.Progress
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.setup_title))
            .setContentText(value.display(this))
            .setProgress(progress?.total ?: 0, progress?.done ?: 0, progress == null)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), getString(R.string.setup_cancel), cancel).build())
            .build()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        scope.cancel()
        state = ExtractionState.Failed(SetupError.Interrupted)
        stopSelf()
    }

    override fun onDestroy() {
        destroyed = true
        scope.cancel()
        super.onDestroy()
    }

    private fun errorFor(error: Exception): SetupError = when (error) {
        is SetupFailure -> error.reason
        is EOFException -> SetupError.Truncated
        is FileNotFoundException, is SecurityException -> SetupError.FileGone
        else -> {
            val diskFull = generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<ErrnoException>().any { it.errno == OsConstants.ENOSPC || it.errno == OsConstants.EDQUOT }
            if (diskFull || (error is IOException && filesDir.usableSpace < MINIMUM_HEADROOM)) SetupError.NoSpace
            else SetupError.Failed
        }
    }

    internal companion object {
        @Volatile var state: ExtractionState = ExtractionState.Idle
            private set

        const val ACTION_CANCEL = "dev.cricamfe.digivice.action.CANCEL_EXTRACTION"
        private val preparationLock = Mutex()
        private const val TAG = "DigiviceExtraction"
        private const val CHANNEL_ID = "preparation"
        private const val NOTIFICATION_ID = 2
        private const val GAME_CHECK_MS = 1_500L
        private const val NOTIFY_EVERY_MS = 500L
        private const val FIRST_RUN_HEADROOM = 200L * 1024 * 1024
        private const val MINIMUM_HEADROOM = 20L * 1024 * 1024
    }
}

/** The notification's text. It is only posted while the worker runs, so only those states have words of their own. */
internal fun ExtractionState.display(context: Context): String = when (this) {
    ExtractionState.PausedForGame -> context.getString(R.string.setup_game_running)
    ExtractionState.Cancelling -> context.getString(R.string.setup_cancelling)
    is ExtractionState.Progress -> {
        val label = context.getString(when (piece) {
            "Cards" -> R.string.setup_cards
            "Sprites" -> R.string.setup_sprites
            "Icons" -> R.string.setup_icons
            "World" -> R.string.setup_world
            "Maps" -> R.string.setup_maps
            "Models" -> R.string.setup_models
            else -> R.string.setup_data
        })
        context.getString(R.string.setup_progress, index, pieces, label, done, total)
    }
    else -> context.getString(R.string.setup_checking)
}
