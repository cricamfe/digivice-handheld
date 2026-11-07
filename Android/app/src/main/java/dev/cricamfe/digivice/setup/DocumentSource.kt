package dev.cricamfe.digivice.setup

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import dev.cricamfe.digivice.core.setup.SetupError
import dev.cricamfe.digivice.extractor.disc.ByteSource
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest

internal const val PREPARATION_PREFERENCES = "game-preparation"
internal const val PREF_DISC = "disc"
internal const val PREF_DISC_NAME = "discName"

/** The game image is read in place through the system file picker's read-only grant. */
internal class DocumentSource(resolver: ContentResolver, uri: Uri) : ByteSource {
    private val descriptor = resolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException("Game image unavailable")
    private val input = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
    private val channel = input.channel
    override val size = try {
        channel.size().also { length ->
            channel.position(0)
            if (length > 1024L * 1024 * 1024) throw SetupFailure(SetupError.NotADisc)
        }
    } catch (e: Exception) {
        input.close()
        if (e is IOException) throw SetupFailure(SetupError.LocalFile, e)
        throw e
    }

    override fun read(position: Long, into: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > into.size - length) throw IndexOutOfBoundsException()
        if (position < 0 || position > size - length) throw EOFException("Read outside the game image")
        val bytes = ByteBuffer.wrap(into, offset, length)
        var cursor = position
        while (bytes.hasRemaining()) {
            val count = channel.read(bytes, cursor)
            if (count < 0) throw EOFException("Game image ended during a read")
            if (count == 0) throw IOException("Game image read made no progress")
            cursor += count
        }
    }

    fun openChd(): ChdSource? {
        val magic = "MComprHD".toByteArray(Charsets.US_ASCII)
        if (size < magic.size) return null
        val header = ByteArray(magic.size)
        read(0, header, 0, header.size)
        return if (header.contentEquals(magic)) ChdSource(descriptor.fd) else null
    }

    fun fingerprint(checkpoint: () -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = ByteArray(1024 * 1024)
        var offset = 0L
        while (offset < size) {
            checkpoint()
            val count = minOf(bytes.size.toLong(), size - offset).toInt()
            read(offset, bytes, 0, count)
            digest.update(bytes, 0, count)
            offset += count
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun close() = input.close()
}
