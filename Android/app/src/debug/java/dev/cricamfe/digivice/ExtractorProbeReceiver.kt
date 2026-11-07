package dev.cricamfe.digivice

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.util.Log
import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.GameCheck
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.checkGame
import dev.cricamfe.digivice.extractor.disc.openDisc
import dev.cricamfe.digivice.extractor.maps.MapRenderer
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** Debug only: one map per broadcast keeps each asynchronous receiver short. */
class ExtractorProbeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val mode = intent.getIntExtra("mode", -1)
        val result = JSONObject().put("success", false).put("pid", Process.myPid()).put("mode", mode)
        if (!running.compareAndSet(false, true)) {
            respond(pending, result.put("error", "A probe is already running"))
            return
        }
        try {
            thread(name = "extractor-probe") {
                val started = SystemClock.elapsedRealtimeNanos()
                try {
                    render(context.applicationContext, mode, result)
                    result.put("success", true)
                } catch (error: Throwable) {
                    result.put("error", "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
                } finally {
                    running.set(false)
                    try {
                        result.put("totalMs", elapsedMs(started))
                    } finally {
                        respond(pending, result)
                    }
                }
            }
        } catch (error: Throwable) {
            running.set(false)
            try {
                result.put("error", "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
            } finally {
                respond(pending, result)
            }
        }
    }

    private fun render(context: Context, mode: Int, result: JSONObject) {
        require(mode >= 0) { "A map mode is required" }
        val external = checkNotNull(context.getExternalFilesDir(null)) { "External app storage is unavailable" }
        FileByteSource(File(external, "extractor-probe.iso")).use { source ->
            val validationStarted = SystemClock.elapsedRealtimeNanos()
            val validation = checkGame(source)
            result.put("validationMs", elapsedMs(validationStarted))
            check(validation == GameCheck.Valid) { "Invalid game: ${validation.javaClass.simpleName}" }

            val loadStarted = SystemClock.elapsedRealtimeNanos()
            checkNotNull(openDisc(source)) { "Disc format is unsupported" }.use { disc ->
                val files = DiscFiles(disc)
                val renderer = MapRenderer(files, GameFileTable(files))
                require(mode in renderer.maps) { "Mode is not a representative map" }
                result.put("loadMs", elapsedMs(loadStarted))

                val renderStarted = SystemClock.elapsedRealtimeNanos()
                val picture = renderer.render(mode)
                result.put("renderMs", elapsedMs(renderStarted))
                    .put("width", picture.width).put("height", picture.height)
                    .put("pixels", picture.argb.size)

                val encodeStarted = SystemClock.elapsedRealtimeNanos()
                val folder = File(context.cacheDir, "extractor-probe")
                check(folder.isDirectory || folder.mkdirs()) { "Cannot create probe cache" }
                val temporary = File(folder, ".$mode.webp.tmp")
                val output = File(folder, "$mode.webp")
                try {
                    val bitmap = Bitmap.createBitmap(picture.argb, picture.width, picture.height, Bitmap.Config.ARGB_8888)
                    try {
                        temporary.outputStream().use { stream ->
                            check(bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, stream)) { "WebP encoding failed" }
                        }
                        check(temporary.renameTo(output)) { "Cannot commit probe image" }
                        result.put("encodeMs", elapsedMs(encodeStarted)).put("webpBytes", output.length())
                        recordMemory(result)
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    if (temporary.exists()) check(temporary.delete()) { "Cannot remove probe temporary file" }
                }
            }
        }
    }

    private fun recordMemory(result: JSONObject) {
        val peakRss = File("/proc/self/status").useLines { lines ->
            lines.firstOrNull { it.startsWith("VmHWM:") }?.substringAfter(':')?.trim()
                ?.takeWhile { it.isDigit() }?.toLongOrNull()
        }
        val runtime = Runtime.getRuntime()
        result.put("processPeakRssKb", peakRss ?: JSONObject.NULL)
            .put("javaUsedBytes", runtime.totalMemory() - runtime.freeMemory())
            .put("nativeHeapAllocatedBytes", Debug.getNativeHeapAllocatedSize())
    }

    private fun respond(pending: PendingResult, result: JSONObject) {
        try {
            val json = result.toString()
            if (result.optBoolean("success")) Log.i(Tag, json) else Log.e(Tag, json)
            pending.resultCode = if (result.optBoolean("success")) Activity.RESULT_OK else Activity.RESULT_CANCELED
            pending.resultData = json
        } finally {
            pending.finish()
        }
    }

    private fun elapsedMs(started: Long): Double = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0

    private companion object {
        const val Tag = "ExtractorProbe"
        val running = AtomicBoolean(false)
    }
}
