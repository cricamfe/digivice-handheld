package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.TestEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PlayerPositionProbeTests {
    private val probe = PlayerPositionProbe(PlayerReader(FakeReader), TestEnvironment.createAddressesRepository())

    private object FakeReader : dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader {
        override fun readInt32(address: Long) = 0
        override fun readInt16(address: Long): Short = 0
        override fun readByte(address: Long): UByte = 0u
        override fun readByte(address: Long, bitMask: Long): UByte = 0u
        override fun readBytes(address: Long, length: Int) = ByteArray(length)
    }

    private fun actor(id: Int = 5, key1: Int = 2, key2: Int = 0, x: Int = 776, y: Int = 224): ByteArray {
        val bytes = ByteArray(probe.size)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0, id)
            putInt(4, key1)
            putInt(8, key2)
            putInt(0x58, x)
            putInt(0x5C, y)
        }
        return bytes
    }

    @Test
    fun `size covers the header and the tile in one request`() {
        assertEquals(0x60, probe.size)
    }

    @Test
    fun `the position of a player actor`() {
        assertEquals(PlayerPositionProbe.Position(776, 224), probe.position(actor()))
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 0x6A, 0x146, 0x147])
    fun `the position of a leader that is not the usual player`(leader: Int) {
        assertEquals(PlayerPositionProbe.Position(776, 224), probe.position(actor(key1 = leader)))
    }

    @Test
    fun `no position when the bytes are not the player's actor`() {
        assertNull(probe.position(actor(id = 16)))
        assertNull(probe.position(actor(key2 = 2)))
        assertNull(probe.position(actor(key1 = 0x21)))
        assertNull(probe.position(ByteArray(8)))
    }
}
