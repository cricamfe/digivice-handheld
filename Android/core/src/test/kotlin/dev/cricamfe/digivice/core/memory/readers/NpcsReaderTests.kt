package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.NpcAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcsAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpcsReaderTests {
    @Test
    fun `read should extract battle value from address and bit mask`() {
        val addresses = NpcsAddresses(
            catherine = NpcAddresses(
                battles = mapOf("first" to NpcBattleAddresses(address = 0x0004B39A, bitMask = 0x08)),
            ),
        )
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B39A, 0x7B) }

        val result = NpcsReader(memoryReader).read(addresses)

        val battle = result.catherine.battles.single()
        assertEquals("first", battle.id)
        assertEquals(0x08.toUByte(), battle.value)
        assertEquals(listOf(0x0004B39AL to 0x08L), memoryReader.maskedByteCalls)
    }

    @Test
    fun `read should return zero value when bit mask is not set`() {
        val addresses = NpcsAddresses(
            lucia = NpcAddresses(
                battles = mapOf("first" to NpcBattleAddresses(address = 0x0004B39A, bitMask = 0x10)),
            ),
        )
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B39A, 0x0B) }

        val result = NpcsReader(memoryReader).read(addresses)

        val battle = result.lucia.battles.single()
        assertEquals(0x00.toUByte(), battle.value)
    }

    @Test
    fun `read should read second byte when battle uses 0x4B39B`() {
        val addresses = NpcsAddresses(
            chris = NpcAddresses(
                battles = mapOf("first" to NpcBattleAddresses(address = 0x0004B39B, bitMask = 0x02)),
            ),
        )
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B39B, 0x03) }

        val result = NpcsReader(memoryReader).read(addresses)

        val battle = result.chris.battles.single()
        assertEquals(0x02.toUByte(), battle.value)
        assertEquals(listOf(0x0004B39BL to 0x02L), memoryReader.maskedByteCalls)
    }

    @Test
    fun `read should read third byte when battle uses 0x4B39C`() {
        val addresses = NpcsAddresses(
            nakano = NpcAddresses(
                battles = mapOf("first" to NpcBattleAddresses(address = 0x0004B39C, bitMask = 0x01)),
            ),
        )
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B39C, 0x01) }

        val result = NpcsReader(memoryReader).read(addresses)

        val battle = result.nakano.battles.single()
        assertEquals(0x01.toUByte(), battle.value)
        assertEquals(listOf(0x0004B39CL to 0x01L), memoryReader.maskedByteCalls)
    }

    @Test
    fun `read should read 48Dbx byte when battle uses 0x48DB9`() {
        val addresses = NpcsAddresses(
            seiryuLeader = NpcAddresses(
                battles = mapOf("first" to NpcBattleAddresses(address = 0x00048DB9, bitMask = 0x01)),
            ),
        )
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x00048DB9, 0x01) }

        val result = NpcsReader(memoryReader).read(addresses)

        val battle = result.seiryuLeader.battles.single()
        assertEquals(0x01.toUByte(), battle.value)
        assertEquals(listOf(0x00048DB9L to 0x01L), memoryReader.maskedByteCalls)
    }

    @Test
    fun `read should read multiple battles for same npc`() {
        val addresses = NpcsAddresses(
            genji = NpcAddresses(
                battles = linkedMapOf(
                    "first" to NpcBattleAddresses(address = 0x0004B3DF, bitMask = 0x20),
                    "second" to NpcBattleAddresses(address = 0x0004B39A, bitMask = 0x01),
                ),
            ),
        )
        val memoryReader = FakeMemoryReader().apply {
            setMaskedByte(0x0004B3DF, 0xA0)
            setMaskedByte(0x0004B39A, 0x03)
        }

        val result = NpcsReader(memoryReader).read(addresses)

        assertEquals(2, result.genji.battles.size)
        assertEquals(0x20.toUByte(), result.genji.battles.single { it.id == "first" }.value)
        assertEquals(0x01.toUByte(), result.genji.battles.single { it.id == "second" }.value)
        assertEquals(listOf("first", "second"), result.genji.battles.map { it.id })
    }
}
