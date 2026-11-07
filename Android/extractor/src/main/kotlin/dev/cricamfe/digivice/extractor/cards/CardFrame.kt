package dev.cricamfe.digivice.extractor.cards

import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.*
import dev.cricamfe.digivice.extractor.maps.StageSprites
import dev.cricamfe.digivice.extractor.sprites.readSpriteFile

/** STCDSPCS/TM's six native frames, marks and digits; no artwork is embedded. */
internal class CardFrame(files: DiscFiles) {
    private val table = GameFileTable(files)
    private val sheet = StageSprites(Pack.entries(readSpriteFile(files, table, 0x63e)).firstOrNull()
        ?: invalidDisc("Missing card sprite sheet"))
    private val texture = loadUiTexture(Pack.entries(readSpriteFile(files, table, 0x642)).firstOrNull()
        ?: invalidDisc("Missing card texture archive"), 0x280)

    fun render(header: CardHeader, art: Picture): Picture {
        val result = Picture(40, 46, IntArray(40 * 46))
        drawUiFrame(result, texture, sheet, header.color - 1, 0, 0, 0x280)
        repeat(art.height) { y -> repeat(art.width) { x ->
            val color = art.argb[y * art.width + x]
            if (color ushr 24 != 0) result.argb[(y + 2) * 40 + x + 4] = color
        } }
        if (header.kind == 0 || header.kind == 15 || header.kind == 16) {
            for ((number, x) in listOf(header.ap to 4, header.hp to 23)) {
                if (number >= 10) drawUiFrame(result, texture, sheet, 0x1e + number / 10, x, 33, 0x280)
                drawUiFrame(result, texture, sheet, 0x1e + number % 10, x + 7, 33, 0x280)
            }
            drawUiFrame(result, texture, sheet, 0x1c, 18, 33, 0x280)
        } else {
            drawUiFrame(result, texture, sheet, 0x1d, 4, 33, 0x280)
        }
        return result
    }
}

// Menu sheets and card sheets use the same on-disc format and horizontal TIM loader.
internal fun loadUiTexture(archive: ByteArray, textureX: Int): IntArray {
    val words = IntArray(1024 * 512)
    var x = textureX
    val entries = Pack.entries(archive)
    if (entries.isEmpty()) invalidDisc("Empty menu texture archive")
    for (entry in entries) {
        val tim = Tim.parse(Rlen.unpack(entry))
        fun load(left: Int, width: Int, height: Int, data: IntArray) {
            if (left < 0 || left + width > 1024 || height > 512) invalidDisc("Menu texture exceeds VRAM")
            repeat(height) { row -> data.copyInto(words, row * 1024 + left, row * width, row * width + width) }
        }
        if (tim.bpp < 2) load(textureX, tim.paletteWidth, tim.paletteHeight, tim.palette)
        load(x, tim.wordWidth, tim.height, tim.words)
        x += 64
    }
    return words
}

internal fun drawUiFrame(target: Picture, words: IntArray, sheet: StageSprites, frameId: Int, x: Int, y: Int, textureX: Int) {
    val frame = sheet.frame(frameId) ?: invalidDisc("Missing menu sprite frame")
    for (placement in frame.parts) {
        val part = sheet.parts[placement.part]
        if (part.mode != 0) invalidDisc("Expected a four-bit menu sprite")
        val page = (textureX + part.u / 4) / 64 * 64
        val clutX = (textureX + part.cx) and -16
        val clutY = part.cy + frame.row
        if (clutX !in 0..1008 || clutY !in 0..511 || part.v + part.height > 512 ||
            page * 4 + (part.u and 255) + part.width > 4096) invalidDisc("Menu sprite exceeds VRAM")
        // Clip before walking texels, so a malformed sheet cannot multiply a tiny
        // output into hundreds of millions of offscreen pixel operations.
        val left = x + placement.x
        val top = y + placement.y
        val firstX = maxOf(0, -left)
        val lastX = minOf(part.width, target.width - left)
        val firstY = maxOf(0, -top)
        val lastY = minOf(part.height, target.height - top)
        for (dy in firstY until lastY) for (dx in firstX until lastX) {
            val nibble = page * 4 + (part.u and 255) + dx
            val word = words[(part.v + dy) * 1024 + nibble / 4]
            val index = word shr ((nibble and 3) * 4) and 15
            val color = words[clutY * 1024 + clutX + index]
            val px = left + dx
            val py = top + dy
            if (color != 0) {
                target.argb[py * target.width + px] = colorArgb(color)
            }
        }
    }
}
