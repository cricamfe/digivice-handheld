package dev.cricamfe.digivice.extractor.cards

import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.*

data class CardHeader(val color: Int, val ap: Int, val hp: Int, val kind: Int, val rank: Int, val comboCard: Int) {
    companion object {
        internal fun parse(bytes: ByteArray): CardHeader {
            bytes.range(0, 12)
            fun byte(offset: Int) = bytes[offset].toInt() and 255
            if (byte(0) !in 1..6 || byte(3) !in 0..16 || byte(1) > 99 || byte(2) > 99) invalidDisc("Invalid card header")
            return CardHeader(byte(0), byte(1), byte(2), byte(3), bytes.shortAt(8), bytes.shortAt(10))
        }
    }
}

class CardImages(private val files: DiscFiles) {
    // The last two records are unused by the card catalogue.
    internal val ids: IntRange get() = 1000..1313

    private val frame by lazy { CardFrame(files) }
    private var cachedIndex = -1
    private var cachedFile = byteArrayOf()

    private fun record(cardId: Int): ByteArray {
        val index = cardId - 1000
        if (index !in 0 until 316) invalidDisc("Card id outside the disc's card table")
        val fileIndex = index / 64
        if (cachedIndex != fileIndex) {
            val file = files.byPath("AAA/DAT/CARD/CARDPAK$fileIndex.BIN") ?: invalidDisc("Missing card image file")
            val expected = (if (fileIndex == 4) 60 else 64) * 1580
            if (file.size != expected) invalidDisc("Invalid card image file size")
            cachedFile = files.read(file)
            cachedIndex = fileIndex
        }
        val start = (index % 64) * 1580
        return cachedFile.copyOfRange(start, start + 1580)
    }

    fun header(cardId: Int): CardHeader = CardHeader.parse(record(cardId))

    fun render(cardId: Int): Picture {
        val bytes = record(cardId)
        val header = CardHeader.parse(bytes)
        val tim = Tim.parse(bytes.copyOfRange(12, 1580))
        if (tim.bpp != 1 || tim.width != 32 || tim.height != 32) invalidDisc("Expected 32 by 32 card art")
        val colors = tim.pixels16()
        colors.indices.forEach { i -> colors[i] = colorArgb(colors[i], if (colors[i] == 0) 0 else 255) }
        return frame.render(header, Picture(32, 32, colors))
    }
}
