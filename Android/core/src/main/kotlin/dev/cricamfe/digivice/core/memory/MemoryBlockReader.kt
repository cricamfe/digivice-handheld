package dev.cricamfe.digivice.core.memory

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Logger

/**
 * Port of `Backend/Memory/Readers/MemoryBlockReader.cs`: little-endian reads from an in-memory block. A read that
 * does not fit in the block (including a negative offset) logs a warning and answers 0, it never throws.
 */
class MemoryBlockReader(private val memoryBlock: ByteArray) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(memoryBlock).order(ByteOrder.LITTLE_ENDIAN)

    fun readInt16(offset: Int): Short {
        if (!fits(offset, Short.SIZE_BYTES)) {
            logger.warning { "MemoryBlockReader.readInt16 out of bounds: offset=$offset, length=${memoryBlock.size}" }
            return 0
        }
        return buffer.getShort(offset)
    }

    fun readInt32(offset: Int): Int {
        if (!fits(offset, Int.SIZE_BYTES)) {
            logger.warning { "MemoryBlockReader.readInt32 out of bounds: offset=$offset, length=${memoryBlock.size}" }
            return 0
        }
        return buffer.getInt(offset)
    }

    private fun fits(offset: Int, size: Int): Boolean =
        memoryBlock.isNotEmpty() && offset >= 0 && offset.toLong() + size <= memoryBlock.size

    private companion object {
        val logger: Logger = Logger.getLogger(MemoryBlockReader::class.java.name)
    }
}
