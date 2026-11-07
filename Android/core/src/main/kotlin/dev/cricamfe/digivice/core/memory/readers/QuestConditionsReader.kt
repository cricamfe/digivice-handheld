package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** GAME.progress in European game RAM. */
internal const val GAME_PROGRESS_ADDRESS = 0x4B370L

/**
 * Which quests the game would start now, by its own conditions: those of the character or the place that gives each one,
 * from `quest-conditions.json` (`Tools/FieldData/generate_quest_conditions.py`). A way holds when all its pairs do, as
 * `checkConditions` tests them. What talking to that character makes true, and whether the player is there, are not asked.
 *
 * Facts from the scripts in the matching decompilation of the European version, ReGame-Labs/dw3_decomp (MIT). It says
 * nothing of a quest being under way already: the page knows.
 */
class QuestConditionsReader(private val memoryReader: IMemoryReader, readDefinition: (String) -> String) {
    private val quests: Map<String, QuestConditions> by lazy {
        json.decodeFromString<Map<String, QuestConditions>>(readDefinition(DEFINITION))
    }

    /** The ids of the quests that can be started, in the order of the table. */
    fun read(): List<String> =
        quests.filterValues { quest -> quest.ways.any { way -> way.conditions.all { (code, value) -> holds(code, value) } } }
            .keys.toList()

    /** `checkCondition`. A group it does not know holds, as in the game. */
    private fun holds(code: Int, value: Int): Boolean {
        val group = (code shr 8) and 0xFE
        val id = code and 0x1FF
        val expected = value != 0
        val flags = FLAG_GROUPS[group]
        return when {
            flags != null -> isSet(flags, id) == expected
            group == 0x60 -> (progress() == id) == expected
            group == 0x70 -> special(id) == expected
            group in 0x80..0x8E -> hasItem(id) == expected
            else -> true
        }
    }

    private fun isSet(base: Long, id: Int): Boolean {
        val byte = memoryReader.readByte(base + (id shr 3)).toInt()
        return byte and (1 shl (id and 7)) != 0
    }

    private fun progress(): Int = memoryReader.readInt32(GAME_PROGRESS_ADDRESS)

    private fun hasItem(id: Int): Boolean =
        memoryReader.readByte(ITEMS + id).toInt() != 0 || memoryReader.readByte(EQUIPPED_ITEMS + id).toInt() != 0

    private fun isUnlocked(partner: Int): Boolean = memoryReader.readInt32(partnerBase(partner) + UNLOCKED) != 0

    private fun levelOf(partner: Int): Int = memoryReader.readInt16(partnerBase(partner) + LEVEL).toInt()

    private fun partyLevel(): Int =
        PARTY.map { memoryReader.readInt32(it) }.filter { it >= 0 }.sumOf { levelOf(it) }

    /** `SPECIAL_CONDITIONS`, for the ids the table uses. One it does not know holds, like an unknown code. */
    private fun special(id: Int): Boolean {
        PROGRESS_RANGES[id]?.let { range -> return progress() in range }
        PARTY_LEVELS[id]?.let { level -> return partyLevel() >= level }
        LOCKED[id]?.let { partner -> return !isUnlocked(partner) }
        return when (id) {
            SPECIAL_VEEMON_UNLOCKED -> isUnlocked(VEEMON)
            SPECIAL_ALL_PARTNERS_LEVEL_45 -> (0 until PARTNERS).none { isUnlocked(it) && levelOf(it) < ALL_PARTNERS_LEVEL }
            SPECIAL_HAS_FISHING_POLE_PARTS -> FISHING_POLE_PARTS.all(::hasItem)
            else -> true
        }
    }

    @Serializable
    private data class QuestConditions(val ways: List<Way>)

    @Serializable
    private data class Way(val conditions: List<List<Int>>)

    private operator fun List<Int>.component1() = this[0]

    private operator fun List<Int>.component2() = this[1]

    private companion object {
        const val DEFINITION = "quest-conditions.json"
        val json = Json { ignoreUnknownKeys = true }

        val FLAG_GROUPS = mapOf(
            0x00 to 0x48ABCL,
            0x02 to 0x4B378L,
            0x04 to 0x4B38AL,
            0x06 to 0x4B38CL,
            0x08 to 0x4B38DL,
            0x0A to 0x4B38EL,
            0x0C to 0x4B392L,
            0x0E to 0x4B39AL,
            0x10 to 0x4B3A6L,
            0x18 to 0x4B3AAL,
            0x1A to 0x4B3ACL,
            0x1C to 0x4B3B5L,
            0x20 to 0x4B3C0L,
            0x40 to 0x4B3DEL,
        )

        const val ITEMS = 0x48DB0L
        const val EQUIPPED_ITEMS = 0x48F43L
        val PARTY = listOf(0x48DA4L, 0x48DA8L, 0x48DACL)

        const val PARTNERS = 8
        const val VEEMON = 4
        const val PARTNER_BASE = 0x49490L
        const val PARTNER_STRIDE = 0x3DCL
        const val UNLOCKED = 4L
        const val LEVEL = 0x28L
        fun partnerBase(partner: Int) = PARTNER_BASE + PARTNER_STRIDE * partner

        /** `checkProgressRange`: both ends count. 0x63 is past the end of the story. */
        val PROGRESS_RANGES = mapOf(
            0x09 to 0x04..0x63,
            0x15 to 0x05..0x0A,
            0x16 to 0x0F..0x12,
            0x17 to 0x14..0x16,
            0x18 to 0x18..0x1A,
            0x19 to 0x1B..0x25,
            0x1A to 0x27..0x28,
            0x22 to 0x04..0x26,
            0x24 to 0x0E..0x26,
            0x3B to 0x11..0x26,
            0x93 to 0x0F..0x63,
        )

        /** `checkPartner` op 5: the sum of the levels of the party, at least this. */
        val PARTY_LEVELS = mapOf(0x25 to 30, 0x26 to 45, 0x27 to 60, 0x28 to 75, 0x29 to 90)

        /** `checkPartner` op 6: that partner is not unlocked. */
        val LOCKED = mapOf(0x2A to 3, 0x2C to 5, 0x2D to 7, 0x2E to 0, 0x2F to 2, 0x30 to 1, 0x36 to 6)

        const val SPECIAL_VEEMON_UNLOCKED = 0x0D
        const val SPECIAL_HAS_FISHING_POLE_PARTS = 0x3A
        const val SPECIAL_ALL_PARTNERS_LEVEL_45 = 0x95
        const val ALL_PARTNERS_LEVEL = 0x2D
        val FISHING_POLE_PARTS = listOf(7, 0x59, 0xA7)
    }
}
