package dev.cricamfe.digivice.core.memory.readers.interfaces

/** Port of `Backend/Memory/Readers/Interfaces/IMemoryReader.cs`. C# `byte` is [UByte] here, `short` is [Short]. */
interface IMemoryReader {
    fun readBytes(address: Long, length: Int): ByteArray
    fun readByte(address: Long): UByte
    fun readByte(address: Long, bitMask: Long): UByte
    fun readInt32(address: Long): Int
    fun readInt16(address: Long): Short
}
