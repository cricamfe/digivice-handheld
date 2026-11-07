package dev.cricamfe.digivice.extractor

import dev.cricamfe.digivice.extractor.image.Picture
import kotlinx.serialization.Serializable

const val FORMAT_VERSION = 10

@Serializable
data class Manifest(val version: Int, val assets: Map<String, String>)

interface AssetSink {
    /** Bare extension; Android can write WebP while the development sink writes PNG. */
    val pictureExtension: String get() = "png"

    /** A completed relative filename, including its extension. Temporary files do not count. */
    fun has(path: String): Boolean

    /** Both writers receive asset keys without an extension. Text outputs are UTF-8 JSON. */
    fun picture(path: String, picture: Picture)
    fun text(path: String, content: String)

    /** Publish only after every asset referenced by this manifest is complete. */
    fun commit(manifest: Manifest)
}
