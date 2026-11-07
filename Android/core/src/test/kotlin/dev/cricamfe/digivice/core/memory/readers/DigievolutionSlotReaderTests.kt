package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DigievolutionSlotReaderTests {
    private fun blockWithInt16At64(value: Short): MemoryBlockReader {
        val block = ByteArray(256)
        block[64] = (value.toInt() and 0xFF).toByte()
        block[65] = ((value.toInt() shr 8) and 0xFF).toByte()
        return MemoryBlockReader(block)
    }

    @Test
    fun `read should return digievolution slot with correct id`() {
        val slotAddresses = SlotAddresses(index = 4, address = 64)

        val result = DigievolutionSlotReader().read(blockWithInt16At64(85), slotAddresses)

        assertEquals(4, result.index)
        assertEquals(85, result.digievolutionId)
    }

    @Test
    fun `read should return zero digievolution id when memory value is zero`() {
        val slotAddresses = SlotAddresses(index = 2, address = 64)

        val result = DigievolutionSlotReader().read(blockWithInt16At64(0), slotAddresses)

        assertEquals(2, result.index)
        assertEquals(0, result.digievolutionId)
    }
}
