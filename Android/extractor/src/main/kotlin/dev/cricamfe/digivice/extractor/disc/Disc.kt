package dev.cricamfe.digivice.extractor.disc

import java.io.Closeable
import java.io.EOFException
import java.io.IOException

/** A disc owns its source; unsupported detection leaves the source with the caller. */
interface Disc : Closeable {
    fun read(lba: Int, bytes: Int): ByteArray
}

fun openDisc(source: ByteSource): Disc? {
    if (source.size >= 16) {
        val header = ByteArray(16)
        source.read(0, header, 0, header.size)
        if (hasRawSync(header) && header[15].toInt() == 2) {
            if (source.size % 2352 != 0L) throw EOFException("Incomplete raw sector")
            return BinDisc(source)
        }
    }
    if (source.size >= 16L * 2048 + 7) {
        val header = ByteArray(7)
        source.read(16L * 2048, header, 0, header.size)
        if (header[0].toInt() == 1 && header.copyOfRange(1, 6).decodeToString() == "CD001") {
            if (source.size % 2048 != 0L) throw EOFException("Incomplete logical sector")
            return IsoDisc(source)
        }
    }
    return null
}

internal fun hasRawSync(bytes: ByteArray): Boolean =
    bytes.size >= 12 && bytes[0] == 0.toByte() && bytes[11] == 0.toByte() && (1..10).all { bytes[it] == 0xff.toByte() }

internal fun validateRead(source: ByteSource, lba: Int, bytes: Int, sectorSize: Int) {
    require(lba >= 0 && bytes >= 0 && bytes <= MAX_READ_BYTES) { "Invalid or oversized read" }
    val sectors = (bytes.toLong() + 2047) / 2048
    if ((lba.toLong() + sectors) * sectorSize > source.size) throw EOFException("Extent outside the image")
}

// shortcut: no game resource read exceeds 64 MiB; use streaming reads if audio/video extraction is added.
internal const val MAX_READ_BYTES = 64 * 1024 * 1024
internal class InvalidDisc(message: String) : IOException(message)
internal fun invalidDisc(message: String): Nothing = throw InvalidDisc(message)

internal fun ByteArray.u16(offset: Int): Int =
    (this[offset].toInt() and 255) or ((this[offset + 1].toInt() and 255) shl 8)
internal fun ByteArray.u32(offset: Int): Long =
    (0..3).fold(0L) { value, i -> value or ((this[offset + i].toLong() and 255) shl (8 * i)) }
internal fun ByteArray.discInt(offset: Int): Int = u32(offset).let {
    if (it > Int.MAX_VALUE) invalidDisc("Unsigned value exceeds supported disc limits")
    it.toInt()
}
