package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.*

fun renderWorldMaps(files: DiscFiles): Map<String, Picture> {
    val table = GameFileTable(files)
    fun read(index: Int): ByteArray {
        val file = files.atLba(table.lbaOf(index)) ?: invalidDisc("World map resource missing")
        checkWorldResourceSize(file.size)
        return files.read(file)
    }
    val overlay = files.byPath("AAA/PRO/STSTATUS.PRO") ?: invalidDisc("World map overlay missing")
    checkWorldResourceSize(overlay.size, 98716)
    val (spots, towns) = worldMapTables(files.read(overlay))
    val output = linkedMapOf<String, Picture>()
    for ((name, offset) in listOf("asuka" to 0, "amaterasu" to 2)) {
        val sheetEntries = Pack.entries(read(0x799 + offset))
        if (sheetEntries.size != 3) invalidDisc("Expected three world map sheets")
        val sheets = sheetEntries.map(::StageSprites)
        val textures = Pack.entries(read(0x79a + offset))
        if (textures.size < 3) invalidDisc("World map textures missing")
        val vram = WorldVram()
        vram.load(textures[1], 0x380, 0x100, 0, 0)
        vram.load(textures[2], 0x300, 0x100, 0, 0)
        vram.load(textures[0], 0x280, 0, 0x140, 0x100)
        val frame = sheets[0].frame(0) ?: invalidDisc("World map base frame missing")
        if (frame.parts.isEmpty()) invalidDisc("Empty world map base")
        val width = frame.parts.maxOf { it.x + sheets[0].parts[it.part].width }
        val height = frame.parts.maxOf { it.y + sheets[0].parts[it.part].height } + 16
        if (width !in 1..2048 || height !in 1..2048 || width.toLong() * height > 1_048_576) invalidDisc("Oversized world map")
        val base = Picture(width, height, IntArray(width * height))
        vram.draw(base, sheets[0], 0, 0, 16, 0x280, 0)
        val full = Picture(width, height, base.argb.copyOf())
        for (i in 1 until towns.size) vram.draw(full, sheets[1], i - 1, towns[i][0], towns[i][1] + 16, 0x380, 0x100)
        for (spot in spots.drop(1)) vram.draw(full, sheets[2], spot[0], spot[1], spot[2] + 16, 0x300, 0x100)
        output["world-$name-base"] = base
        output["world-$name"] = full
    }
    return output
}

internal fun checkWorldResourceSize(size: Int, minimum: Int = 4) {
    if (size !in minimum..1_048_576) invalidDisc("Invalid world map resource size")
}

internal fun worldMapTables(overlay: ByteArray): Pair<List<IntArray>, List<IntArray>> {
    // The original European STSTATUS overlay stores these tables consecutively.
    fun rows(offset: Int, count: Int, columns: Int): List<IntArray> {
        overlay.range(offset, count * columns * 4)
        return List(count) { row -> IntArray(columns) { column -> overlay.intAt(offset + (row * columns + column) * 4) } }
    }
    val spots = rows(97968, 47, 3)
    val towns = rows(98532, 23, 2)
    if (spots.any { it[0] !in 0..255 || it[1] !in 0..2048 || it[2] !in 0..2048 } ||
        towns.any { it.any { coordinate -> coordinate !in 0..2048 } }) invalidDisc("Invalid world map marker")
    return spots to towns
}

private class WorldVram {
    private val words = IntArray(1024 * 512)
    private var drawnPixels = 0L

    fun load(archive: ByteArray, imageX: Int, imageY: Int, clutX: Int, clutY: Int) {
        fun copy(data: IntArray, width: Int, height: Int, x: Int, y: Int) {
            if (x < 0 || y < 0 || x + width > 1024 || y + height > 512) invalidDisc("World texture exceeds VRAM")
            repeat(height) { row -> data.copyInto(words, (y + row) * 1024 + x, row * width, (row + 1) * width) }
        }
        for ((index, bytes) in Pack.entries(archive).withIndex()) {
            val tim = Tim.parse(Rlen.unpack(bytes))
            if (tim.bpp < 2) copy(tim.palette, tim.paletteWidth, tim.paletteHeight, clutX, clutY)
            copy(tim.words, tim.wordWidth, tim.height, imageX + index * 0x40, imageY)
        }
    }

    fun draw(target: Picture, sheet: StageSprites, frameId: Int, x: Int, y: Int, textureX: Int, textureY: Int) {
        val frame = sheet.frame(frameId) ?: invalidDisc("World map frame missing")
        for (placement in frame.parts) {
            val p = sheet.parts[placement.part]
            drawnPixels += p.width.toLong() * p.height
            if (drawnPixels > 16_000_000) invalidDisc("Oversized world map drawing workload")
            val eight = p.mode != 0
            val page = (textureX + p.u / if (eight) 2 else 4) / 64 * 64
            val paletteX = (if (eight) 0x140 else textureX) + p.cx
            val paletteY = (if (eight) 0 else textureY) + p.cy + frame.row
            val left = if (eight) page * 2 + (p.u and 127) else page * 4 + (p.u and 255)
            val top = textureY + p.v
            if (paletteX < 0 || paletteX + (if (eight) 256 else 16) > 1024 || paletteY !in 0..511 ||
                top < 0 || top + p.height > 512 || left < 0 || left + p.width > (if (eight) 2048 else 4096)) {
                invalidDisc("World map part outside VRAM")
            }
            repeat(p.height) { dy ->
                repeat(p.width) { dx ->
                    val address = left + dx
                    val word = words[(top + dy) * 1024 + address / (if (eight) 2 else 4)]
                    val index = if (eight) (word ushr ((address and 1) * 8)) and 255 else (word ushr ((address and 3) * 4)) and 15
                    val color = words[paletteY * 1024 + paletteX + index]
                    val outX = x + placement.x + dx
                    val outY = y + placement.y + dy
                    if (color != 0 && outX in 0 until target.width && outY in 0 until target.height) {
                        target.argb[outY * target.width + outX] = colorArgb(color, 255)
                    }
                }
            }
        }
    }
}
