package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CharacterTables(val actors: Map<Int, List<Int>>, val items: List<Int>) {
    companion object {
        val data: CharacterTables by lazy {
            val input = checkNotNull(CharacterTables::class.java.getResourceAsStream("/extractor/characters.json"))
            input.bufferedReader().use { Json.decodeFromString(it.readText()) }
        }
    }
}

class FieldSprites(private val files: DiscFiles, private val table: GameFileTable) {
    fun all(): Sequence<Pair<String, Picture>> = sequence {
        // Only the current pack is retained; field packs may contain empty slots.
        var previousFile = -1
        var pack: ByteArray? = null
        fun actor(character: Int, direction: Int): Picture {
            val reference = CharacterTables.data.actors[character] ?: invalidDisc("Unknown field character")
            val index = reference[0]
            if (previousFile != index) {
                pack = readSpriteFile(files, table, index)
                previousFile = index
            }
            val archive = checkNotNull(pack)
            val sets = spriteEntry(archive, reference[1])
            var animation: Int? = null
            for (offset in 0 until sets.size - 11 step 12) {
                val id = sets.shortAt(offset)
                if (id == 0) break
                if (id == 1) {
                    animation = sets.shortAt(offset + 2 + (if (direction < 5) direction else 8 - direction) * 2)
                    break
                }
            }
            val frames = spriteEntry(archive, animation ?: invalidDisc("Missing standing animation"))
            frames.range(0, 16)
            if (frames.intAt(0) <= 0) invalidDisc("Empty standing animation")
            val tim = Tim.parse(spriteEntry(archive, frames.intAt(4)))
            val raw = tim.pixels16()
            val pixels = IntArray(raw.size) { i ->
                val source = if (direction < 5) i else (i / tim.width) * tim.width + tim.width - 1 - i % tim.width
                colorArgb(raw[source], if (raw[source] == 0) 0 else 255)
            }
            return Picture(tim.width, tim.height, pixels)
        }
        for (character in CharacterTables.data.actors.keys.sorted()) {
            for (direction in 0..7) yield("actors/$character-$direction" to actor(character, direction))
        }
    }
}

internal fun readSpriteFile(files: DiscFiles, table: GameFileTable, index: Int): ByteArray {
    if (index !in 0 until 2382) invalidDisc("Invalid sprite file index")
    val file = files.atLba(table.lbaOf(index)) ?: invalidDisc("Missing sprite file")
    if (file.size !in 4..MAX_DATA) invalidDisc("Invalid sprite file size")
    return files.read(file)
}

/** The field sprite variant keeps zero-offset slots, unlike a getArchiveEntry archive. */
internal fun spriteEntry(bytes: ByteArray, index: Int): ByteArray {
    var first = 0
    var cursor = 0
    while (first == 0) {
        if (cursor >= 16384) invalidDisc("Invalid sprite offset table")
        first = bytes.intAt(cursor * 4)
        cursor++
    }
    if (first % 4 != 0 || first / 4 !in cursor..16384 || first > bytes.size) invalidDisc("Invalid sprite offset table")
    val count = first / 4
    if (index !in 0 until count) invalidDisc("Missing sprite entry")
    var previous = first
    var start = 0
    var end = bytes.size
    repeat(count) { slot ->
        val offset = bytes.intAt(slot * 4)
        if (offset != 0) {
            if (offset < previous || offset > bytes.size) invalidDisc("Invalid sprite entry offset")
            previous = offset
            if (slot > index && end == bytes.size) end = offset
        }
        if (slot == index) start = offset
    }
    if (start == 0) invalidDisc("Empty sprite entry")
    return bytes.copyOfRange(start, end)
}
