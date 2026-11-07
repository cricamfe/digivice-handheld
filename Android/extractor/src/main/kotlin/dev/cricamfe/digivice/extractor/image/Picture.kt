package dev.cricamfe.digivice.extractor.image

class Picture(val width: Int, val height: Int, val argb: IntArray) {
    init {
        require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= MAX_PIXELS)
        require(argb.size.toLong() == width.toLong() * height)
    }
}

// shortcut: current game images fit below 4M pixels; revisit before supporting larger output resolutions.
internal const val MAX_PIXELS = 4 * 1024 * 1024
internal const val MAX_DATA = 8 * 1024 * 1024

internal fun colorArgb(color: Int, alpha: Int = 255): Int {
    fun expand(c: Int) = (c shl 3) or (c shr 2)
    return (alpha shl 24) or (expand(color and 31) shl 16) or
        (expand((color shr 5) and 31) shl 8) or expand((color shr 10) and 31)
}
