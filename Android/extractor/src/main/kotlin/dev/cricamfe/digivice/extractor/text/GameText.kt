package dev.cricamfe.digivice.extractor.text

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.disc.u32
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

enum class GameLanguage(val folder: String, val prefix: String, val locale: String) {
    ENG("ENG", "ES", "en-US"), SPN("SPN", "SS", "es-ES"), FRA("FRA", "FS", "fr-FR"),
    GER("GER", "DS", "de-DE"), ITA("ITA", "IS", "it-IT"),
}

class GameText(private val files: DiscFiles) {
    /**
     * A text table by name (`sklnam`), or a table of a story scene: `dmg800/9` is entry 9 of the archive `ESDMG800.BIN`,
     * the scenes' texts as ddw3's lang_file splits them.
     */
    fun table(language: GameLanguage, name: String): List<String> {
        val scene = sceneTable.matchEntire(name)
        require(scene != null || name.matches(Regex("[a-zA-Z0-9]+"))) { "Invalid text table name" }
        val fileName = scene?.groupValues?.get(1) ?: name
        val path = "AAA/DAT/COUNTRY/${language.folder}/${language.prefix}${fileName.uppercase(Locale.ROOT)}.BIN"
        val file = files.byPath(path) ?: invalidDisc("Missing text table: $path")
        if (file.size > MAX_TEXT_BYTES) invalidDisc("Text table exceeds supported size")
        val bytes = files.read(file)
        return decodeTextTable(if (scene == null) bytes else archiveEntry(bytes, scene.groupValues[2].toInt()))
    }
}

private val sceneTable = Regex("(dmg[0-9]{3})/([0-9]{1,3})")

/** Entry `index` of an archive of text tables: offsets from its start, the first of them giving where the offsets end. */
internal fun archiveEntry(bytes: ByteArray, index: Int): ByteArray {
    if (bytes.size < 4 || bytes.size > MAX_TEXT_BYTES) invalidDisc("Invalid text archive size")
    val first = bytes.u32(0)
    if (first < 4 || first % 4 != 0L || first > bytes.size) invalidDisc("Invalid text archive header")
    val count = (first / 4).toInt()
    if (index !in 0 until count) invalidDisc("Missing text archive entry")
    val start = bytes.u32(index * 4)
    val end = if (index + 1 < count) bytes.u32((index + 1) * 4) else bytes.size.toLong()
    if (start < first || end < start || end > bytes.size) invalidDisc("Text archive entry outside archive")
    return bytes.copyOfRange(start.toInt(), end.toInt())
}

private const val MAX_TEXT_BYTES = 2 * 1024 * 1024
private const val MAX_TEXT_LINES = 16_384
private const val MAX_LINE_BYTES = 65_536
private const val MAX_EXPANDED_BYTES = 4 * 1024 * 1024

internal fun decodeTextTable(bytes: ByteArray): List<String> {
    if (bytes.size < 4 || bytes.size > MAX_TEXT_BYTES) invalidDisc("Invalid text table size")
    val count = bytes.u32(0)
    val header = 4 + count * 4
    if (count > MAX_TEXT_LINES || header > bytes.size) invalidDisc("Invalid text table count")
    var encodedTotal = 0L
    var decodedTotal = 0L
    return List(count.toInt()) { index ->
        val offset = bytes.u32(4 + index * 4)
        if (offset < header || offset >= bytes.size) invalidDisc("Text offset outside table")
        val start = offset.toInt()
        var end = start
        while (end < bytes.size && bytes[end] != 0.toByte() && end - start <= MAX_LINE_BYTES) end++
        if (end == bytes.size || end - start > MAX_LINE_BYTES) invalidDisc("Unterminated or oversized text line")
        encodedTotal += end - start
        if (encodedTotal > MAX_EXPANDED_BYTES) invalidDisc("Repeated text offsets exceed supported expansion")
        decodeLine(bytes, start, end).also {
            decodedTotal += it.length
            if (decodedTotal > MAX_EXPANDED_BYTES) invalidDisc("Decoded text exceeds supported expansion")
        }
    }
}

private fun decodeLine(bytes: ByteArray, start: Int, end: Int): String = buildString {
    val codes = charset
    var position = start
    while (position < end) {
        var code = 0L
        var found: String? = null
        var consumed = 0
        for (length in 1..minOf(6, end - position)) {
            code = (code shl 8) or (bytes[position + length - 1].toLong() and 255)
            codes[code or (length.toLong() shl 48)]?.let { found = it; consumed = length }
        }
        if (found != null) {
            append(found)
            position += consumed
        } else {
            val value = bytes[position++].toInt() and 255
            append("\\x").append("0123456789abcdef"[value ushr 4]).append("0123456789abcdef"[value and 15])
        }
    }
}

private val charset: Map<Long, String> by lazy {
    val resource = GameText::class.java.getResourceAsStream("/extractor/charset.json")
        ?: error("Missing extractor charset facts")
    val entries = resource.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
    check(entries.size == 358) { "Invalid extractor charset facts" }
    entries.mapKeys { (hex, _) ->
        check(hex.length in 2..12 && hex.length % 2 == 0)
        hex.toLong(16) or ((hex.length / 2).toLong() shl 48)
    }.mapValues { (_, value) -> value.jsonPrimitive.content }
}
