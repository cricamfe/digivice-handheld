package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeAccessor
import dev.cricamfe.digivice.core.memory.MemoryAccessor
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.MemoryReader
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.logging.Logger

class MemoryReaderTests {
    private val silentLogger = Logger.getLogger("MemoryReaderTests").apply { useParentHandlers = false }

    /** A connected session: the accessor provider always answers with [accessor]. */
    private fun connectedReader(accessor: MemoryAccessor) = MemoryReader({ accessor }, silentLogger)

    /** A disconnected session: the accessor provider answers null, like `DuckstationSession.Accessor`. */
    private fun disconnectedReader() = MemoryReader({ null }, silentLogger)

    @Test
    fun `readInt32 should throw MemoryReadException when disconnected`() {
        val exception = assertThrows(MemoryReadException::class.java) { disconnectedReader().readInt32(0x800100) }

        assertEquals(0x800100L, exception.address)
    }

    @Test
    fun `readInt32 should throw MemoryReadException when accessor throws`() {
        val accessor = FakeAccessor().apply { onReadInt32 = { throw RuntimeException("Physical mapped I/O error") } }

        val exception = assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readInt32(0x800100) }

        assertEquals(0x800100L, exception.address)
        assertNotNull(exception.cause)
    }

    @Test
    fun `readInt32 should return int when connected and accessor succeeds`() {
        val accessor = FakeAccessor().apply { onReadInt32 = { address -> if (address == 0x800100L) 9999 else 0 } }

        val result = connectedReader(accessor).readInt32(0x800100)

        assertEquals(9999, result)
    }

    @Test
    fun `readInt16 should return short when connected and accessor succeeds`() {
        val accessor = FakeAccessor().apply {
            onReadInt16 = { address -> if (address == 0x800100L) 45.toShort() else 0 }
        }

        val result = connectedReader(accessor).readInt16(0x800100)

        assertEquals(45.toShort(), result)
    }

    @Test
    fun `readInt16 should throw MemoryReadException when accessor throws`() {
        val accessor = FakeAccessor().apply { onReadInt16 = { throw RuntimeException("I/O error") } }

        assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readInt16(0x800100) }
    }

    @Test
    fun `readInt16 should throw MemoryReadException when disconnected`() {
        assertThrows(MemoryReadException::class.java) { disconnectedReader().readInt16(0x800100) }
    }

    @Test
    fun `readByte should return byte when connected and accessor succeeds`() {
        val accessor = FakeAccessor().apply {
            onReadBytes = { address, count ->
                assertEquals(0x800100L, address)
                assertEquals(1, count)
                byteArrayOf(0x2A)
            }
        }

        val result = connectedReader(accessor).readByte(0x800100)

        assertEquals(0x2A.toUByte(), result)
    }

    @Test
    fun `readByte should be unsigned like C# byte`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> byteArrayOf(0xFF.toByte()) } }

        assertEquals(255.toUByte(), connectedReader(accessor).readByte(0x800100))
    }

    @Test
    fun `readByte should apply bit mask when bit mask is provided`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> byteArrayOf(90) } }

        val result = connectedReader(accessor).readByte(0x800100, 15)

        assertEquals(10.toUByte(), result)
    }

    @Test
    fun `readByte with bit mask should truncate the mask to a byte like the C# cast`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> byteArrayOf(0xFF.toByte()) } }

        // C#: (byte)(0xFF & 0x1F0L) == 0xF0
        assertEquals(0xF0.toUByte(), connectedReader(accessor).readByte(0x800100, 0x1F0))
    }

    @Test
    fun `readByte should throw MemoryReadException when accessor throws`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> throw RuntimeException("I/O error") } }

        assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readByte(0x800100) }
    }

    @Test
    fun `readByte with bit mask should throw MemoryReadException when accessor throws`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> throw RuntimeException("I/O error") } }

        assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readByte(0x800100, 0x0F) }
    }

    @Test
    fun `readByte should throw MemoryReadException when disconnected`() {
        assertThrows(MemoryReadException::class.java) { disconnectedReader().readByte(0x800100) }
    }

    @Test
    fun `readBytes should return bytes when connected and accessor succeeds`() {
        val expectedBytes = byteArrayOf(1, 2, 3)
        val accessor = FakeAccessor().apply {
            onReadBytes = { address, count -> if (address == 0x800100L && count == 3) expectedBytes.copyOf() else ByteArray(count) }
        }

        val result = connectedReader(accessor).readBytes(0x800100, 3)

        assertArrayEquals(expectedBytes, result)
    }

    @Test
    fun `readBytes should throw MemoryReadException when accessor throws`() {
        val accessor = FakeAccessor().apply { onReadBytes = { _, _ -> throw RuntimeException("I/O error") } }

        assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readBytes(0x800100, 3) }
    }

    @Test
    fun `readBytes should throw MemoryReadException when disconnected`() {
        assertThrows(MemoryReadException::class.java) { disconnectedReader().readBytes(0x800100, 3) }
    }

    @Test
    fun `a MemoryReadException from the accessor is rethrown as is`() {
        val original = MemoryReadException(0x10, "boom")
        val accessor = FakeAccessor().apply { onReadInt32 = { throw original } }

        val thrown = assertThrows(MemoryReadException::class.java) { connectedReader(accessor).readInt32(0x800100) }

        assertEquals(original, thrown)
    }

    @Test
    fun `the accessor constructor reads from that accessor`() {
        val accessor = FakeAccessor().apply { onReadInt32 = { 7 } }

        assertEquals(7, MemoryReader(accessor).readInt32(0))
    }
}
