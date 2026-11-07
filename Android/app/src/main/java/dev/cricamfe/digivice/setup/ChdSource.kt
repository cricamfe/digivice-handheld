package dev.cricamfe.digivice.setup

import dev.cricamfe.digivice.core.setup.SetupError
import dev.cricamfe.digivice.extractor.disc.ByteSource
import java.io.EOFException

/** Exposes the raw data track as a BIN, leaving out each frame's 96 subchannel bytes. */
internal class ChdSource(fd: Int) : ByteSource {
    private var handle = ChdNative.open(fd).takeIf { it != 0L } ?: throw SetupFailure(SetupError.InvalidChd)
    override val size: Long = ChdNative.size(handle)

    override fun read(position: Long, into: ByteArray, offset: Int, length: Int) {
        check(handle != 0L)
        if (offset < 0 || length < 0 || offset > into.size - length) throw IndexOutOfBoundsException()
        if (position < 0 || position > size - length) throw EOFException("Read outside the CHD data track")
        if (!ChdNative.read(handle, position, into, offset, length)) throw SetupFailure(SetupError.InvalidChd)
    }

    override fun close() {
        ChdNative.close(handle)
        handle = 0
    }
}

private object ChdNative {
    init {
        System.loadLibrary("digivice_chd")
    }

    external fun open(fd: Int): Long
    external fun size(handle: Long): Long
    external fun read(handle: Long, position: Long, output: ByteArray, offset: Int, length: Int): Boolean
    external fun close(handle: Long)
}
