package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader

/**
 * The value rule shared by [RequisiteReader] and [StepReader] (each has its own private copy in the C# code):
 * no masks reads the raw byte, one mask reads the byte through it, several masks read 0 unless every mask is set and
 * otherwise answer the first mask applied to the byte.
 */
internal fun readBitMaskedValue(memoryReader: IMemoryReader, address: Long, bitMasks: List<Long>): UByte {
    if (bitMasks.isEmpty()) {
        return memoryReader.readByte(address)
    }

    if (bitMasks.size == 1) {
        return memoryReader.readByte(address, bitMasks[0])
    }

    val rawValue = memoryReader.readByte(address).toLong()
    for (bitMask in bitMasks) {
        if ((rawValue and bitMask) == 0L) {
            return 0u
        }
    }

    return (rawValue and bitMasks[0]).toUByte()
}
