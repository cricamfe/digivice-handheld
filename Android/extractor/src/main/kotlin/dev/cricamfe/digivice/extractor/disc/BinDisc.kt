package dev.cricamfe.digivice.extractor.disc

internal class BinDisc(private val source: ByteSource) : Disc {
    override fun read(lba: Int, bytes: Int): ByteArray {
        validateRead(source, lba, bytes, 2352)
        val out = ByteArray(bytes)
        val header = ByteArray(24)
        var offset = 0
        var sector = lba.toLong()
        while (offset < bytes) {
            val position = sector * 2352
            source.read(position, header, 0, header.size)
            if (!hasRawSync(header) || header[15].toInt() != 2) invalidDisc("Expected a MODE2 sector")
            val count = minOf(2048, bytes - offset)
            source.read(position + 24, out, offset, count)
            offset += count
            sector++
        }
        return out
    }
    override fun close() = source.close()
}
