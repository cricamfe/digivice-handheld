package dev.cricamfe.digivice.setup

import android.graphics.Bitmap
import dev.cricamfe.digivice.extractor.AssetSink
import dev.cricamfe.digivice.extractor.FORMAT_VERSION
import dev.cricamfe.digivice.extractor.Manifest
import dev.cricamfe.digivice.extractor.image.Picture
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

internal class WebpSink(private val root: File) : AssetSink {
    override val pictureExtension = "webp"
    var reused = 0
        private set

    override fun has(path: String): Boolean = isComplete(path).also { if (it) reused++ }

    private fun isComplete(path: String): Boolean = resolve(path).let { it.isFile && it.length() > 0 }

    override fun picture(path: String, picture: Picture) {
        val bitmap = Bitmap.createBitmap(picture.argb, picture.width, picture.height, Bitmap.Config.ARGB_8888)
        try {
            val lossy = path.substringBefore('/') in LOSSY_GROUPS
            val format = if (lossy) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP_LOSSLESS
            write("$path.webp") { output ->
                if (!bitmap.compress(format, if (lossy) 75 else 100, output)) throw IOException("Could not encode $path")
            }
        } finally {
            bitmap.recycle()
        }
    }

    override fun text(path: String, content: String) = write("$path.json") { it.write(content.toByteArray(Charsets.UTF_8)) }

    override fun commit(manifest: Manifest) {
        check(manifest.version == FORMAT_VERSION && manifest.assets.isNotEmpty())
        check(manifest.assets.keys.containsAll(REQUIRED_TABLES)) { "Missing game data tables" }
        check(manifest.assets.values.toSet().all(::isComplete)) { "Manifest references an incomplete asset" }
        text("manifest", Json.encodeToString(manifest))
    }

    private fun resolve(path: String): File {
        require(path.matches(RESOURCE_PATH) && path.split('/').none { it == "." || it == ".." }) { "Invalid resource path: $path" }
        return File(root, path)
    }

    private fun write(path: String, output: (OutputStream) -> Unit) {
        val target = resolve(path)
        val parent = checkNotNull(target.parentFile)
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create resource directory")
        val temporary = File(parent, ".${target.name}.tmp")
        try {
            temporary.outputStream().use(output)
            Files.move(temporary.toPath(), target.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private companion object {
        val LOSSY_GROUPS = setOf("maps", "enemies", "battle-enemies", "battle-stages")
        val REQUIRED_TABLES = setOf("tables/cards", "tables/battle")
        val RESOURCE_PATH = Regex("[a-zA-Z0-9_. -]+(?:/[a-zA-Z0-9_. -]+)*")
    }
}
