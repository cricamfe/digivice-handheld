package dev.cricamfe.digivice.core.memory

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A [MemoryAccessor] over a RAM snapshot (the 2 MiB dump of the PS1 RAM). It reads [ram] in place, without copying,
 * so the array must not be modified while the accessor is in use. Reads outside the snapshot throw
 * [MemoryReadException].
 */
class SnapshotAccessor(private val ram: ByteArray) : MemoryAccessor {
    private val buffer: ByteBuffer = ByteBuffer.wrap(ram).order(ByteOrder.LITTLE_ENDIAN)

    override fun readInt32(address: Long): Int {
        requireInRange(address, Int.SIZE_BYTES)
        return buffer.getInt(address.toInt())
    }

    override fun readInt16(address: Long): Short {
        requireInRange(address, Short.SIZE_BYTES)
        return buffer.getShort(address.toInt())
    }

    override fun readBytes(address: Long, count: Int): ByteArray {
        requireInRange(address, count)
        val start = address.toInt()
        return ram.copyOfRange(start, start + count)
    }

    private fun requireInRange(address: Long, count: Int) {
        if (count < 0 || address < 0 || address > ram.size.toLong() - count) {
            throw MemoryReadException(
                address,
                "Cannot read $count byte(s) at 0x${address.toString(16).uppercase()}: outside the ${ram.size}-byte snapshot.",
            )
        }
    }
}
