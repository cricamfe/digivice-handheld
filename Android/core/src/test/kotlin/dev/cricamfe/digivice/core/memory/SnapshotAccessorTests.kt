package dev.cricamfe.digivice.core.memory

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SnapshotAccessorTests {
    private val ram = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xFF.toByte(), 0xFF.toByte(), 0x80.toByte(), 0x00)
    private val accessor = SnapshotAccessor(ram)

    @Test
    fun `readInt32 should be little endian`() {
        assertEquals(0x04030201, accessor.readInt32(0))
    }

    @Test
    fun `readInt32 should be signed`() {
        assertEquals(-1, SnapshotAccessor(ByteArray(4) { 0xFF.toByte() }).readInt32(0))
    }

    @Test
    fun `readInt16 should be little endian and signed`() {
        assertEquals(0x0201.toShort(), accessor.readInt16(0))
        assertEquals((-1).toShort(), accessor.readInt16(4))
        assertEquals(0x0080.toShort(), accessor.readInt16(6))
    }

    @Test
    fun `readBytes should return a copy of the requested range`() {
        val bytes = accessor.readBytes(2, 3)

        assertArrayEquals(byteArrayOf(0x03, 0x04, 0xFF.toByte()), bytes)
        bytes[0] = 0x7F
        assertEquals(0x03.toByte(), ram[2])
    }

    @Test
    fun `reads past the end should throw MemoryReadException with the address`() {
        val exception = assertThrows(MemoryReadException::class.java) { accessor.readInt32(6) }
        assertEquals(6L, exception.address)
        assertThrows(MemoryReadException::class.java) { accessor.readInt16(7) }
        assertThrows(MemoryReadException::class.java) { accessor.readBytes(7, 2) }
    }

    @Test
    fun `negative addresses should throw MemoryReadException`() {
        assertThrows(MemoryReadException::class.java) { accessor.readInt16(-1) }
        assertThrows(MemoryReadException::class.java) { accessor.readBytes(-1, 1) }
    }

    @Test
    fun `reads that end exactly at the end of the ram are valid`() {
        assertEquals(0x0080.toShort(), accessor.readInt16(6))
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x00), accessor.readBytes(6, 2))
    }
}
