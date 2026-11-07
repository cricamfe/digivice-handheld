package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestConditionsReaderTests {
    /** A blank RAM of the size the game state reaches, little-endian as the console's. */
    private class Ram : IMemoryReader {
        val bytes = ByteArray(0x50000)
        private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        init {
            PARTY.forEach { buffer.putInt(it, -1) }
        }

        var progress: Int
            get() = buffer.getInt(0x4B370)
            set(value) { buffer.putInt(0x4B370, value) }

        fun flag(base: Int, id: Int) {
            bytes[base + (id shr 3)] = (bytes[base + (id shr 3)].toInt() or (1 shl (id and 7))).toByte()
        }

        fun item(id: Int) { bytes[0x48DB0 + id] = 1 }

        fun partner(index: Int, unlocked: Boolean, level: Int) {
            buffer.putInt(0x49494 + 0x3DC * index, if (unlocked) 1 else 0)
            buffer.putShort(0x49490 + 0x3DC * index + 0x28, level.toShort())
        }

        fun party(vararg partners: Int) = partners.forEachIndexed { slot, id -> buffer.putInt(PARTY[slot], id) }

        override fun readBytes(address: Long, length: Int) = bytes.copyOfRange(address.toInt(), address.toInt() + length)
        override fun readByte(address: Long) = bytes[address.toInt()].toUByte()
        override fun readByte(address: Long, bitMask: Long) = (bytes[address.toInt()].toLong() and bitMask).toUByte()
        override fun readInt32(address: Long) = buffer.getInt(address.toInt())
        override fun readInt16(address: Long) = buffer.getShort(address.toInt())

        companion object {
            val PARTY = listOf(0x48DA4, 0x48DA8, 0x48DAC)
        }
    }

    private fun offers(ram: Ram) = QuestConditionsReader(ram) { File(TestEnvironment.repoRoot, "Backend/Memory/Definitions/$it").readText() }.read()

    private fun Ram.withLevels(vararg levels: Int) {
        // Kotemon, Kumamon and Monmon in the party, none of the others unlocked.
        levels.forEachIndexed { slot, level -> partner(slot, true, level) }
        party(*levels.indices.toList().toIntArray())
    }

    @Test
    fun `a DRI agent asks for the party's total level, both ends, and that the partner is not unlocked`() {
        val ram = Ram().apply { progress = 0x08 }
        ram.withLevels(10, 10, 9)
        assertFalse("driAgentAgumon" in offers(ram))

        ram.withLevels(10, 10, 10)
        assertTrue("driAgentAgumon" in offers(ram))

        ram.withLevels(20, 20, 19)
        assertTrue("driAgentAgumon" in offers(ram))

        ram.withLevels(20, 20, 20)
        assertFalse("driAgentAgumon" in offers(ram))

        ram.withLevels(10, 10, 10)
        ram.partner(3, true, 1)
        assertFalse("driAgentAgumon" in offers(ram))
    }

    @Test
    fun `a character that only stands there in some chapters only offers in those`() {
        val ram = Ram()
        ram.progress = 0x06
        assertTrue("eternally" in offers(ram))
        ram.progress = 0x0B
        assertFalse("eternally" in offers(ram), "Pharaohmon is absent at 0x0B")
        ram.progress = 0x0C
        assertTrue("eternally" in offers(ram))
    }

    @Test
    fun `a quest of the extra chapter only opens in it`() {
        val ram = Ram().apply { progress = 0x26 }
        assertFalse("cardKing" in offers(ram))
        ram.progress = 0x2D
        assertTrue("cardKing" in offers(ram))
        ram.flag(0x4B3A6, 0x16)
        assertFalse("cardKing" in offers(ram), "beaten")
    }

    @Test
    fun `a place that starts it by itself asks for what the trigger does`() {
        val ram = Ram().apply { progress = 0x06 }
        assertFalse("treeBoots" in offers(ram))
        ram.item(0x192)
        assertTrue("treeBoots" in offers(ram))
        ram.flag(0x4B3AC, 0x22)
        assertFalse("treeBoots" in offers(ram))
    }

    @Test
    fun `items count also when they are equipped`() {
        val ram = Ram().apply { progress = 0x06 }
        ram.bytes[0x48F43 + 0x192] = 1
        assertTrue("treeBoots" in offers(ram))
    }

    @Test
    fun `the fishing pole is not offered with all its parts`() {
        val ram = Ram().apply { progress = 0x06 }
        ram.item(0x192)
        assertTrue("fishingPole" in offers(ram))
        listOf(7, 0x59, 0xA7).forEach(ram::item)
        assertFalse("fishingPole" in offers(ram))
    }

    @Test
    fun `every quest of the table is known`() {
        assertEquals(emptyList<String>(), offers(Ram().apply { progress = -1 }) - TABLE)
    }

    private companion object {
        val TABLE = setOf(
            "folderBag", "fishingPole", "treeBoots", "digimonOnline", "eternally", "invincible", "muramasa", "punishment",
            "superNova", "driAgentAgumon", "driAgentGuilmon", "driAgentPatamon", "driAgentRenamon", "driAgentKotemon",
            "driAgentKumamon", "driAgentMonmon", "driAgentVeemon", "asukaTrophy", "asukaRematch", "amaterasuTrophy",
            "amaterasuRematch", "cardKing",
        )
    }
}
