package dev.cricamfe.digivice.extractor.disc

internal class IsoDisc(private val source: ByteSource) : Disc {
    override fun read(lba: Int, bytes: Int): ByteArray {
        validateRead(source, lba, bytes, 2048)
        return ByteArray(bytes).also { source.read(lba.toLong() * 2048, it, 0, bytes) }
    }
    override fun close() = source.close()
}
