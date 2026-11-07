package dev.cricamfe.digivice.core.memory

/**
 * Raw access to the emulated PS1 RAM; port of `IMemoryAccessor`. Addresses are offsets into RAM and values are
 * little-endian, like `BitConverter`/`MemoryMappedViewAccessor`.
 *
 * This is the raw layer: [readBytes] hands out a signed [ByteArray]. The typed unsigned view of a byte
 * (C# `byte`) is `UByte`, produced by [MemoryReader.readByte].
 */
interface MemoryAccessor {
    fun readInt32(address: Long): Int
    fun readInt16(address: Long): Short
    fun readBytes(address: Long, count: Int): ByteArray
}
