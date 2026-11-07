package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.*

internal class StageSprites(private val bytes: ByteArray) {
    data class Part(val u: Int, val v: Int, val width: Int, val height: Int, val cx: Int, val cy: Int, val mode: Int)
    data class Placement(val part: Int, val x: Int, val y: Int)
    data class Frame(val row: Int, val abr: Int, val parts: List<Placement>)
    private val partOffset = bytes.intAt(0)
    private val idOffset = bytes.intAt(4)
    private val count: Int
    val parts: List<Part>

    init {
        if (partOffset < 8 || (partOffset - 8) % 4 != 0 || idOffset < partOffset) invalidDisc("Invalid sprite sheet offsets")
        count = (partOffset - 8) / 4
        val partCount = (idOffset - partOffset) / 14
        if (count > 256 || partCount > 4096) invalidDisc("Oversized sprite sheet")
        bytes.range(idOffset, count)
        bytes.range(partOffset, partCount * 14)
        parts = (0 until partCount).map { index ->
            val p = partOffset + index * 14
            Part(bytes.shortAt(p), bytes.shortAt(p + 2), bytes.shortAt(p + 4), bytes.shortAt(p + 6),
                bytes.shortAt(p + 8), bytes.shortAt(p + 10), bytes.shortAt(p + 12)).also {
                if (it.u < 0 || it.v < 0 || it.width !in 0..512 || it.height !in 0..512 || it.mode !in 0..1) invalidDisc("Invalid sprite part")
            }
        }
    }

    fun frame(id: Int): Frame? {
        val index = (0 until count).firstOrNull { (bytes[idOffset + it].toInt() and 255) == id } ?: return null
        val offset = bytes.intAt(8 + index * 4)
        bytes.range(offset, 6)
        val n = bytes.shortAt(offset)
        if (n !in 0..1024) invalidDisc("Invalid sprite frame size")
        bytes.range(offset + 6, n * 6)
        return Frame(bytes.shortAt(offset + 2), bytes.shortAt(offset + 4), (0 until n).map {
            val p = offset + 6 + it * 6
            Placement(bytes.shortAt(p), bytes.shortAt(p + 2), bytes.shortAt(p + 4)).also { placement ->
                if (placement.part !in parts.indices) invalidDisc("Sprite frame references a missing part")
            }
        })
    }
}

internal class Vram {
    private val words = IntArray(1024 * 512)

    private fun load(x: Int, y: Int, width: Int, height: Int, data: IntArray) {
        if (x !in 0..1024 || y !in 0..512) invalidDisc("Image archive exceeds VRAM")
        val w = minOf(width, 1024 - x)
        val h = minOf(height, 512 - y)
        repeat(h) { row -> data.copyInto(words, (y + row) * 1024 + x, row * width, row * width + w) }
    }

    fun loadArchive(archive: ByteArray, imageX: Int = 0x140, imageY: Int = 0x100, paletteY: Int = 0x1f0) {
        var x = imageX
        for (entry in Pack.entries(archive)) {
            val tim = Tim.parse(Rlen.unpack(entry))
            if (tim.bpp < 2) load(0, paletteY, tim.paletteWidth, tim.paletteHeight, tim.palette)
            load(x, imageY, tim.wordWidth, tim.height, tim.words)
            x += 0x40
        }
    }

    fun part(
        part: StageSprites.Part, row: Int, clutRow: Int,
        imageX: Int = 0x140, imageY: Int = 0x100, paletteY: Int = 0x1f0,
    ): IntArray {
        val eight = part.mode != 0
        val hw = imageX + part.u / if (eight) 2 else 4
        val page = (hw / 64) * if (eight) 128 else 256
        val u0 = part.u and if (eight) 127 else 255
        val clutX = (part.cx + if (eight) 0 else imageX) and -16
        val clutY = (part.cy + row + clutRow + if (eight) paletteY - 0x100 else imageY) and 511
        if (clutX < 0 || clutX + (if (eight) 256 else 16) > 1024) invalidDisc("Sprite palette outside VRAM")
        return IntArray(part.width * part.height) { index ->
            val x = index % part.width
            val y = index / part.width
            val address = page + ((u0 + x) and 255)
            val byteAddress = (if (eight) address else address / 2) and 2047
            val word = words[((imageY + (part.v and 255) + y) and 511) * 1024 + byteAddress / 2]
            val byte = (word shr ((byteAddress and 1) * 8)) and 255
            val paletteIndex = if (eight) byte else (byte shr ((address and 1) * 4)) and 15
            words[clutY * 1024 + clutX + paletteIndex]
        }
    }
}
