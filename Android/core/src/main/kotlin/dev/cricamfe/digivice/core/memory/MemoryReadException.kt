package dev.cricamfe.digivice.core.memory

/**
 * Port of `Backend/Memory/MemoryReadException.cs`: a failed read at [address], with the failure as [cause].
 * Open so the RetroArch accessor can say why a read failed (`MemoryReadRefused`, `MemoryReadLost`) and still pass
 * through `MemoryReader` untouched.
 */
open class MemoryReadException(
    val address: Long,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
