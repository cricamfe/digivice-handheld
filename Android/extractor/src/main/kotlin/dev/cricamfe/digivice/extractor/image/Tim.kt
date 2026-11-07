package dev.cricamfe.digivice.extractor.image

import dev.cricamfe.digivice.extractor.disc.discInt
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.disc.u16

class Tim private constructor(
    val bpp: Int,
    val wordWidth: Int,
    val height: Int,
    val words: IntArray,
    val paletteWidth: Int,
    val paletteHeight: Int,
    val palette: IntArray,
) {
    val width: Int = wordWidth * when (bpp) { 0 -> 4; 1 -> 2; else -> 1 }

    fun pixels16(row: Int = 0): IntArray {
        if (bpp == 2) return words.copyOf()
        if (row !in 0 until paletteHeight) invalidDisc("TIM palette row outside the image")
        return IntArray(width * height) { i ->
            val x = i % width
            val word = words[(i / width) * wordWidth + x / (if (bpp == 0) 4 else 2)]
            val index = if (bpp == 0) (word shr ((x and 3) * 4)) and 15 else (word shr ((x and 1) * 8)) and 255
            if (index < paletteWidth) palette[row * paletteWidth + index] else 0
        }
    }

    fun picture(row: Int = 0): Picture {
        val pixels = pixels16(row)
        pixels.indices.forEach { index ->
            val color = pixels[index]
            val alpha = if (color == 0) 0 else if (color and 0x7fff == 0 || color and 0x8000 == 0) 255 else 127
            pixels[index] = colorArgb(color, alpha)
        }
        return Picture(width, height, pixels)
    }

    companion object {
        fun parse(bytes: ByteArray): Tim {
            if (bytes.intAt(0) != 0x10) invalidDisc("Invalid TIM signature")
            val flags = bytes.intAt(4)
            val bpp = flags and 3
            if (flags and 0xfffffff4.toInt() != 0 || bpp == 3) invalidDisc("Unsupported TIM format")
            var offset = 8
            fun block(): Triple<Int, Int, IntArray> {
                bytes.range(offset, 12)
                val size = bytes.intAt(offset)
                val w = bytes.wordAt(offset + 8)
                val h = bytes.wordAt(offset + 10)
                if (size < 12 || w !in 1..1024 || h !in 1..512) invalidDisc("Invalid TIM block")
                if (size < 12 + w * h * 2) invalidDisc("Truncated TIM pixel payload")
                bytes.range(offset, size)
                val words = IntArray(w * h)
                repeat(w * h) { words[it] = bytes.wordAt(offset + 12 + it * 2) }
                offset += size
                return Triple(w, h, words)
            }
            val clut = if (flags and 8 != 0) block() else null
            if (bpp < 2 && clut == null) invalidDisc("Indexed TIM has no palette")
            val image = block()
            return Tim(bpp, image.first, image.second, image.third, clut?.first ?: 0, clut?.second ?: 0, clut?.third ?: intArrayOf())
        }
    }
}

internal fun ByteArray.range(offset: Int, count: Int) {
    if (offset < 0 || count < 0 || offset > size - count) invalidDisc("Truncated image resource")
}
internal fun ByteArray.intAt(offset: Int): Int { range(offset, 4); return discInt(offset) }
internal fun ByteArray.wordAt(offset: Int): Int { range(offset, 2); return u16(offset) }
internal fun ByteArray.shortAt(offset: Int): Int = wordAt(offset).toShort().toInt()
