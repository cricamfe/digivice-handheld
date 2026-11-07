package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.MemoryAccessor
import dev.cricamfe.digivice.core.memory.MemoryReadException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A [MemoryAccessor] over RetroArch's network commands. RetroArch answers one command per frame (about 20 ms), so the
 * readers' many small reads are served from pages of [pageSize] bytes: the first read in a page asks RetroArch for the
 * whole page, the rest of that cycle's reads in it come from memory, and [beginCycle] forgets the pages so the next
 * cycle sees fresh data. Inside a cycle every read of a page sees the same instant, which keeps a state coherent.
 *
 * A failed page is never cached; its exception ([MemoryReadRefused], [MemoryReadLost]) passes through
 * `MemoryReader` unchanged because both are [MemoryReadException]s. Not thread safe: one loop owns it.
 */
class PagedMemoryAccessor(
    private val client: RetroArchClient,
    private val pageSize: Int = RetroArchClient.MAX_READ_BYTES,
) : MemoryAccessor {
    private val pages = HashMap<Long, ByteArray>()

    /**
     * Called, on the loop's thread, just before each request for a page goes out (so between the requests of a cycle,
     * never inside one). [GameLoop] uses it to read the player's position between the pages of a long cycle. It must not
     * read through this accessor and must not throw.
     */
    var beforeRequest: (() -> Unit)? = null

    /** The requests sent to RetroArch since [beginCycle] (a failed request counts, a cached page does not). */
    var requestsInCycle: Int = 0
        private set

    init {
        require(pageSize in 1..RetroArchClient.MAX_READ_BYTES) { "pageSize must be 1..${RetroArchClient.MAX_READ_BYTES}" }
    }

    fun beginCycle() {
        pages.clear()
        requestsInCycle = 0
    }

    override fun readInt32(address: Long): Int = littleEndian(readBytes(address, Int.SIZE_BYTES)).int

    override fun readInt16(address: Long): Short = littleEndian(readBytes(address, Short.SIZE_BYTES)).short

    override fun readBytes(address: Long, count: Int): ByteArray {
        if (count < 0 || address < 0 || address > RAM_SIZE - count) {
            throw MemoryReadException(
                address,
                "Cannot read $count byte(s) at 0x${address.toString(16).uppercase()}: outside the $RAM_SIZE-byte RAM.",
            )
        }
        val result = ByteArray(count)
        var copied = 0
        while (copied < count) {
            val position = address + copied
            val pageStart = position / pageSize * pageSize
            val page = page(pageStart)
            val offset = (position - pageStart).toInt()
            val length = minOf(count - copied, page.size - offset)
            System.arraycopy(page, offset, result, copied, length)
            copied += length
        }
        return result
    }

    private fun page(start: Long): ByteArray {
        pages[start]?.let { return it }
        beforeRequest?.invoke()
        requestsInCycle++
        val page = client.read(start, minOf(pageSize.toLong(), RAM_SIZE - start).toInt())
        pages[start] = page
        return page
    }

    private fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    private companion object {
        /** The PS1's main RAM, 2 MiB. */
        const val RAM_SIZE = 0x200000L
    }
}
