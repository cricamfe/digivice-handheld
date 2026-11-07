package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DigimonSlotReaderTests {
    @Test
    fun `read should return slot with raw digimon id`() {
        val addresses = SlotAddresses(index = 2, address = 0x5000)
        val memoryReader = FakeMemoryReader().apply { bytes[0x5000] = 14u }

        val result = DigimonSlotReader(memoryReader).read(addresses)

        assertEquals(2, result.index)
        assertEquals(14, result.digimonId)
    }

    @Test
    fun `read should keep empty slot sentinel as raw value`() {
        val addresses = SlotAddresses(index = 3, address = 0x6000)
        val memoryReader = FakeMemoryReader().apply { bytes[0x6000] = 0xFFu }

        val result = DigimonSlotReader(memoryReader).read(addresses)

        assertEquals(3, result.index)
        // Unsigned: a C# byte 0xFF widens to int 255, not -1.
        assertEquals(0xFF, result.digimonId)
        assertNull(result.digimonResource)
    }

    @Test
    fun `read should throw MemoryReadException when memory reader fails`() {
        val addresses = SlotAddresses(index = 5, address = 0x6000)
        val memoryReader = FakeMemoryReader().apply {
            byteFailure = MemoryReadException(0x6000, "Memory session is not connected.")
        }

        assertThrows(MemoryReadException::class.java) { DigimonSlotReader(memoryReader).read(addresses) }
    }
}
