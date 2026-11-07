package dev.cricamfe.digivice.extractor.text

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.disc.u16
import dev.cricamfe.digivice.extractor.disc.u32
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class Technique(val mp: Int, val power: Int, val type: String?, val element: String, val elementStrength: Int)

@Serializable
internal data class BattleForm(val nameId: Int, val signature: Int)

@Serializable
internal data class BattleWords(val names: List<String>, val descriptions: List<String>, val forms: List<String>)

@Serializable
internal data class BattleCatalog(
    val techniques: List<Technique>,
    val forms: Map<Int, BattleForm>,
    val partners: List<Int>,
    val words: Map<String, BattleWords>,
)

private const val TECHNIQUE_COUNT = 443
private const val FORM_COUNT = 55
private val elements = listOf("none", "none", "fire", "water", "ice", "wind", "thunder", "machine", "dark")

internal fun buildBattleCatalog(files: DiscFiles): String {
    val exe = files.byPath("SLES_039.36") ?: invalidDisc("Missing PAL executable")
    if (exe.size != 923648) invalidDisc("Unexpected PAL executable length")
    val bytes = files.read(exe)
    if (bytes.decodeToString(0, 8) != "PS-X EXE") invalidDisc("Invalid PS-X EXE header")
    val loadAddress = bytes.u32(0x18)
    val payloadSize = bytes.u32(0x1c)
    fun table(address: Long, size: Int): Int {
        val offset = address - loadAddress
        if (offset < 0 || offset + size > minOf(payloadSize, (bytes.size - 2048).toLong())) {
            invalidDisc("Battle table outside executable payload")
        }
        return 2048 + offset.toInt()
    }
    fun u8(offset: Int): Int = bytes[offset].toInt() and 255

    // Addresses and record layouts from the matching EU dw3_decomp symbols and game_state.h.
    val techStart = table(0x80042BDC, TECHNIQUE_COUNT * 0x12)
    val techniques = List(TECHNIQUE_COUNT) { index ->
        val row = techStart + index * 0x12
        val power = bytes.u16(row + 2)
        val type = if (u8(row + 5) == 2 && power > 0) when (u8(row + 4)) {
            2 -> "physical"
            3 -> "magical"
            else -> null
        } else null
        val element = elements.getOrElse(u8(row + 7)) { "none" }
        Technique(bytes.u16(row), power, type, element, if (element == "none") 0 else u8(row + 8))
    }

    val formStart = table(0x8003EF5C, FORM_COUNT * 0x58)
    val forms = linkedMapOf<Int, BattleForm>()
    repeat(FORM_COUNT) { index ->
        val row = formStart + index * 0x58
        val form = BattleForm(u8(row + 0x55), bytes.u16(row + 0x28))
        if (form.signature !in 0..TECHNIQUE_COUNT || forms.put(bytes.u16(row), form) != null) {
            invalidDisc("Invalid battle form")
        }
    }
    val text = GameText(files)
    val words = GameLanguage.entries.associate { language ->
        val names = text.table(language, "sklnam")
        val descriptions = text.table(language, "sklinf")
        val formNames = text.table(language, "dignam")
        if (forms.values.any { it.nameId !in formNames.indices }) invalidDisc("Missing battle form name")
        language.locale to BattleWords(
            List(TECHNIQUE_COUNT) { flat(names.getOrElse(it + 1) { "" }) },
            List(TECHNIQUE_COUNT) { flat(descriptions.getOrElse(it + 1) { "" }) },
            formNames.map(::flat),
        )
    }
    return Json.encodeToString(BattleCatalog(techniques, forms, forms.keys.take(8), words))
}
