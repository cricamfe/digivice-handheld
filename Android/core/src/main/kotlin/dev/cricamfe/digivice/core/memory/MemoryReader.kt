package dev.cricamfe.digivice.core.memory

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Port of `Backend/Memory/Readers/MemoryReader.cs`.
 *
 * [accessorProvider] plays the role of `IDuckstationSession.Accessor`: it answers the current accessor, or null while
 * there is no memory session, in which case every read throws [MemoryReadException]. Any other failure of the
 * accessor is logged and rethrown as [MemoryReadException] with the original exception as its cause.
 */
class MemoryReader(
    private val accessorProvider: () -> MemoryAccessor?,
    private val logger: Logger = Logger.getLogger(MemoryReader::class.java.name),
) : IMemoryReader {
    /** A reader over an accessor that is always connected (for example a [SnapshotAccessor]). */
    constructor(accessor: MemoryAccessor) : this({ accessor })

    private fun getConnectedAccessor(address: Long): MemoryAccessor =
        accessorProvider() ?: throw MemoryReadException(address, "Memory session is not connected.")

    private inline fun <T> readValue(address: Long, read: (MemoryAccessor) -> T): T {
        val accessor = getConnectedAccessor(address)

        try {
            return read(accessor)
        } catch (e: MemoryReadException) {
            throw e
        } catch (e: Exception) {
            val hex = address.toString(16).uppercase()
            logger.log(Level.SEVERE, "Failed to read memory at 0x$hex: ${e.message}")
            throw MemoryReadException(address, "Failed to read memory at 0x$hex.", e)
        }
    }

    override fun readInt32(address: Long): Int = readValue(address) { it.readInt32(address) }

    override fun readInt16(address: Long): Short = readValue(address) { it.readInt16(address) }

    override fun readByte(address: Long): UByte = readBytes(address, 1)[0].toUByte()

    /** C# `(byte)(ReadByte(address) & bitMask)`: the mask is a long, the result is truncated to a byte. */
    override fun readByte(address: Long, bitMask: Long): UByte =
        (readByte(address).toLong() and bitMask).toUByte()

    override fun readBytes(address: Long, length: Int): ByteArray =
        readValue(address) { it.readBytes(address, length) }
}
