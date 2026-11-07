package dev.cricamfe.digivice.core.memory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class MemoryBlockReaderTests {
    @Test
    fun `readInt16 should return correct value when offset is valid`() {
        val buffer = byteArrayOf(0x01, 0x02, 0x03)
        val reader = MemoryBlockReader(buffer)

        val result = reader.readInt16(0)

        assertEquals(513.toShort(), result)
    }

    @ParameterizedTest
    @ValueSource(ints = [2, -1])
    fun `readInt16 should return zero when offset is out of bounds`(offset: Int) {
        val buffer = byteArrayOf(0x01, 0x02, 0x03)
        val reader = MemoryBlockReader(buffer)

        val result = reader.readInt16(offset)

        assertEquals(0.toShort(), result)
    }

    @Test
    fun `readInt16 should return zero when buffer is empty`() {
        val reader = MemoryBlockReader(byteArrayOf())

        val result = reader.readInt16(0)

        assertEquals(0.toShort(), result)
    }

    @Test
    fun `readInt32 should return correct value when offset is valid`() {
        val buffer = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        val reader = MemoryBlockReader(buffer)

        val result = reader.readInt32(1)

        assertEquals(67305985, result)
    }

    @ParameterizedTest
    @ValueSource(ints = [3, -1])
    fun `readInt32 should return zero when offset is out of bounds`(offset: Int) {
        val buffer = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val reader = MemoryBlockReader(buffer)

        val result = reader.readInt32(offset)

        assertEquals(0, result)
    }

    @Test
    fun `readInt32 should return zero when buffer is empty`() {
        val reader = MemoryBlockReader(byteArrayOf())

        val result = reader.readInt32(0)

        assertEquals(0, result)
    }

    // Extra cases (not in the C# suite) that pin down the signedness and little-endian conventions.

    @Test
    fun `readInt16 should sign extend like C# short`() {
        val reader = MemoryBlockReader(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))

        assertEquals((-1).toShort(), reader.readInt16(0))
    }

    @Test
    fun `readInt32 should be little endian and signed like BitConverter`() {
        val reader = MemoryBlockReader(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte()))

        assertEquals(Int.MIN_VALUE, reader.readInt32(0))
    }

    @Test
    fun `out of bounds reads should log a warning`() {
        val records = mutableListOf<LogRecord>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                records += record
            }

            override fun flush() = Unit
            override fun close() = Unit
        }
        val logger = Logger.getLogger(MemoryBlockReader::class.java.name)
        val previousUseParent = logger.useParentHandlers
        logger.useParentHandlers = false
        logger.addHandler(handler)
        try {
            MemoryBlockReader(byteArrayOf(0x01)).readInt16(0)
            MemoryBlockReader(byteArrayOf(0x01)).readInt32(0)
        } finally {
            logger.removeHandler(handler)
            logger.useParentHandlers = previousUseParent
        }

        assertEquals(2, records.size)
        assertTrue(records.all { it.level == Level.WARNING })
    }

    // Not ported: Constructor_ShouldThrowArgumentNullException_WhenBufferIsNull. The Kotlin parameter is a
    // non-nullable ByteArray, so passing null does not compile.
}
