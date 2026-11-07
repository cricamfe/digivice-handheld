package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.cards.drawUiFrame
import dev.cricamfe.digivice.extractor.cards.loadUiTexture
import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.*
import dev.cricamfe.digivice.extractor.maps.StageSprites

class ItemIcons(private val files: DiscFiles, private val table: GameFileTable) {
    fun all(): Sequence<Pair<String, Picture>> = sequence {
        val sheet = StageSprites(Pack.entries(readSpriteFile(files, table, 0x286)).firstOrNull()
            ?: invalidDisc("Missing item sprite sheet"))
        val archive = Pack.entries(readSpriteFile(files, table, 0x287)).firstOrNull()
            ?: invalidDisc("Missing item texture archive")
        val texture = loadUiTexture(archive, 0x140)
        for (frameId in CharacterTables.data.items) {
            val frame = sheet.frame(frameId) ?: invalidDisc("Missing item icon frame")
            if (frame.parts.size != 1) invalidDisc("Expected a single-part item icon")
            val part = sheet.parts[frame.parts[0].part]
            if (part.width == 0 || part.height == 0) invalidDisc("Empty item icon")
            val result = Picture(part.width, part.height, IntArray(part.width * part.height))
            drawUiFrame(result, texture, sheet, frameId, -frame.parts[0].x, -frame.parts[0].y, 0x140)
            yield("items/item-${frameId.toString(16).padStart(2, '0')}" to result)
        }
    }
}
