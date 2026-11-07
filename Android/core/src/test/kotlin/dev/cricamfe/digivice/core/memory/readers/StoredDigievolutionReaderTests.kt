package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryBlockReader
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StoredDigievolutionReaderTests {
    private companion object {
        const val UNLOCKED_DIGIEVOLUTIONS_START = 0x50
        const val ENTRY_STRIDE = 20
    }

    private fun createAddresses(maxUnlockedDigievolutions: Int) = DigievolutionsAddresses(
        unlockedDigievolutionsStart = UNLOCKED_DIGIEVOLUTIONS_START,
        unlockedDigievolutionEntryStride = ENTRY_STRIDE,
        maxUnlockedDigievolutions = maxUnlockedDigievolutions,
        id = 0x00,
        level = 0x02,
        dvexp = 0x04,
    )

    private fun writeEntry(block: ByteArray, entryIndex: Int, digievolutionId: Short, level: Short, dvexp: Int) {
        val entryOffset = UNLOCKED_DIGIEVOLUTIONS_START + (entryIndex * ENTRY_STRIDE)
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(entryOffset, digievolutionId)
            putShort(entryOffset + 2, level)
            putInt(entryOffset + 4, dvexp)
        }
    }

    @Test
    fun `read should return unlocked entries when ids are greater than zero`() {
        val block = ByteArray(1500)
        writeEntry(block, 0, digievolutionId = 3, level = 15, dvexp = 70000)
        writeEntry(block, 1, digievolutionId = 5, level = 40, dvexp = 250)
        writeEntry(block, 2, digievolutionId = 8, level = 20, dvexp = 0)

        val result = StoredDigievolutionReader().read(MemoryBlockReader(block), createAddresses(4))

        assertEquals(3, result.size)
        assertEquals(3, result[0].digievolutionId)
        assertEquals(15, result[0].level)
        assertEquals(70000, result[0].dvexp)
        assertEquals(5, result[1].digievolutionId)
        assertEquals(40, result[1].level)
        assertEquals(250, result[1].dvexp)
        assertEquals(8, result[2].digievolutionId)
        assertEquals(20, result[2].level)
        assertEquals(0, result[2].dvexp)
    }

    @Test
    fun `read should return empty list when first entry is empty`() {
        val block = ByteArray(1500)
        writeEntry(block, 0, digievolutionId = 0, level = 15, dvexp = 100)
        writeEntry(block, 1, digievolutionId = 99, level = 40, dvexp = 100)

        val result = StoredDigievolutionReader().read(MemoryBlockReader(block), createAddresses(2))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `read should stop at first empty entry and ignore garbage after it`() {
        val block = ByteArray(1500)
        writeEntry(block, 0, digievolutionId = 3, level = 15, dvexp = 100)
        writeEntry(block, 1, digievolutionId = 0, level = 0, dvexp = 0)
        writeEntry(block, 2, digievolutionId = 240, level = 180, dvexp = 999)

        val result = StoredDigievolutionReader().read(MemoryBlockReader(block), createAddresses(4))

        val stored = result.single()
        assertEquals(3, stored.digievolutionId)
        assertEquals(15, stored.level)
    }

    @Test
    fun `read should respect max unlocked digievolutions`() {
        val block = ByteArray(1500)
        writeEntry(block, 0, digievolutionId = 3, level = 15, dvexp = 100)
        writeEntry(block, 1, digievolutionId = 5, level = 40, dvexp = 200)
        writeEntry(block, 2, digievolutionId = 8, level = 20, dvexp = 300)

        val result = StoredDigievolutionReader().read(MemoryBlockReader(block), createAddresses(2))

        assertEquals(2, result.size)
        assertEquals(5, result[1].digievolutionId)
    }
}
