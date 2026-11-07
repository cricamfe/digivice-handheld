package dev.cricamfe.digivice.extractor

import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.writePng
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PngSink(directory: File) : AssetSink {
    private val root = directory.canonicalFile

    init {
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Cannot create output directory")
    }

    private fun resolve(path: String): File {
        require(path.isNotEmpty() && !path.startsWith('/') && '\\' !in path && path.none { it.code < 32 } &&
            path.split('/').all { it.isNotEmpty() && it != "." && it != ".." }) { "Invalid asset path: $path" }
        val file = File(root, path).canonicalFile
        require(file != root && file.toPath().startsWith(root.toPath())) { "Asset path escapes output directory" }
        return file
    }

    override fun has(path: String): Boolean {
        val file = resolve(path)
        return (path.endsWith(".png") || path.endsWith(".json")) && file.isFile && file.length() > 0
    }

    override fun picture(path: String, picture: Picture) {
        resolve(path)
        atomic("$path.png") { picture.writePng(it) }
    }

    override fun text(path: String, content: String) {
        resolve(path)
        atomic("$path.json") { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    override fun commit(manifest: Manifest) {
        require(manifest.version == FORMAT_VERSION) { "Unsupported manifest version" }
        for ((key, path) in manifest.assets) {
            resolve(key)
            require(has(path)) { "Manifest references an incomplete asset: $path" }
        }
        atomic("manifest.json") { it.write(Json.encodeToString(manifest).toByteArray(Charsets.UTF_8)) }
    }

    private fun atomic(path: String, write: (OutputStream) -> Unit) {
        val target = resolve(path)
        val parent = target.parentFile
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create asset directory")
        val temporary = Files.createTempFile(parent.toPath(), ".${target.name}.", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use { write(it) }
            Files.move(temporary, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
