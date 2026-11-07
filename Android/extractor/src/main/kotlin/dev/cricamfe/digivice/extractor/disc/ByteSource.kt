package dev.cricamfe.digivice.extractor.disc

import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

interface ByteSource : Closeable {
    val size: Long
    fun read(position: Long, into: ByteArray, offset: Int, length: Int)
}

class FileByteSource(file: File) : ByteSource {
    private val input = FileInputStream(file)
    private val channel = input.channel
    override val size: Long = channel.size()

    override fun read(position: Long, into: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > into.size - length) throw IndexOutOfBoundsException()
        if (position < 0 || position > size - length) throw EOFException("Read outside the image")
        val buffer = ByteBuffer.wrap(into, offset, length)
        var cursor = position
        while (buffer.hasRemaining()) {
            val count = channel.read(buffer, cursor)
            if (count < 0) throw EOFException("Image ended during a read")
            if (count == 0) throw java.io.IOException("Image read made no progress")
            cursor += count
        }
    }

    override fun close() = input.close()
}
