package dev.cricamfe.digivice.setup

import android.content.Context
import android.util.AtomicFile
import dev.cricamfe.digivice.extractor.FORMAT_VERSION
import java.io.File
import java.io.IOException
import java.util.UUID

/** A published generation is immutable, so a WebView can keep reading it during preparation. */
internal class GameAssetsStore(context: Context) {
    val root = File(context.filesDir, "game/$FORMAT_VERSION")
    private val current = AtomicFile(File(root, "current"))
    private val staging = AtomicFile(File(root, "staging"))

    fun active(): File? = current.readMarker()?.let(::generation)
        ?.takeIf { File(it, "manifest.json").length() > 0 }

    /** Resume only when both the extraction format and the complete source image agree. */
    fun stage(sourceHash: String): File {
        val old = staging.readMarker()
        val previous = old?.substringAfter('\n', "")?.let(::generation)
        val active = active()
        if (old?.substringBefore('\n') == sourceHash && previous?.isDirectory == true && previous != active) {
            return previous
        }
        val next = File(root, "pack-${UUID.randomUUID()}")
        if (!next.mkdirs()) throw IOException("Cannot create game staging directory")
        staging.writeMarker("$sourceHash\n${next.name}")
        if (previous != active) previous?.deleteRecursively()
        return next
    }

    /** The sink validates the complete manifest before calling this. */
    fun publish(directory: File) {
        check(generation(directory.name) == directory && File(directory, "manifest.json").length() > 0)
        current.writeMarker(directory.name)
        staging.delete()
    }

    private fun generation(name: String): File? =
        name.takeIf { it.matches(GENERATION_NAME) }?.let { File(root, it) }

    private fun AtomicFile.readMarker(): String? = try {
        // AtomicFile.openRead can remove an in-flight .new file. The published base is already atomic.
        baseFile.inputStream().use { input ->
            val text = input.readNBytes(128)
            if (text.size in 1..127) text.toString(Charsets.US_ASCII) else null
        }
    } catch (_: IOException) {
        null
    }

    private fun AtomicFile.writeMarker(value: String) {
        val output = startWrite()
        try {
            output.write(value.toByteArray(Charsets.US_ASCII))
            finishWrite(output)
        } catch (e: IOException) {
            failWrite(output)
            throw e
        }
        if (readMarker() != value) throw IOException("Could not publish game resource marker")
    }

    private companion object {
        val GENERATION_NAME = Regex("pack-[a-f0-9-]{36}")
    }
}
