package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.data.ExtractorData
import dev.cricamfe.digivice.extractor.disc.Disc
import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MapResourceTests {
    @Test fun `map slot count and alignment must match the grid`() {
        val valid = mapBytes()
        assertEquals(1, MapFile.parse(valid).entries.size)
        val missing = valid.copyOf(2048)
        val extra = valid.copyOf(2048 + 32)
        val partial = valid.copyOf(valid.size + 1)
        for (bytes in listOf(missing, extra, partial)) assertThrows(IOException::class.java) { MapFile.parse(bytes) }
    }

    @Test fun `a missing declared map prevents incomplete map discovery`() {
        val files = files(imagePresent = true, mapPresent = false)
        val error = assertThrows(IOException::class.java) { MapRenderer(files, GameFileTable(files)) }
        assertTrue(error.message!!.contains("Map file missing"), error.message)
    }

    @Test fun `a missing declared image cannot silently remove map layers`() {
        val files = files(imagePresent = false)
        val renderer = MapRenderer(files, GameFileTable(files))
        val error = assertThrows(IOException::class.java) { renderer.render(206) }
        assertTrue(error.message!!.contains("image file missing"), error.message)
    }

    @Test fun `a missing declared sheet cannot silently remove map objects`() {
        for (sheetPresent in listOf(false, true)) {
            val files = files(imagePresent = true, sheetPresent = sheetPresent)
            val renderer = MapRenderer(files, GameFileTable(files))
            val error = assertThrows(IOException::class.java) { renderer.render(206) }
            assertTrue(error.message!!.contains("sprite sheet missing"), error.message)
        }
    }
}

private fun mapBytes(): ByteArray = ByteArray(2048 + 16).apply {
    put32(0, 1); put32(4, 1); put32(8, 1); put32(12, 16)
}

private fun files(imagePresent: Boolean, mapPresent: Boolean = true, sheetPresent: Boolean = false): DiscFiles {
    val bytes = ByteArray(256 * 2048)
    val exeSize = 0x3576c + 2382 * 4
    val stage = ExtractorData.stage(206)!!
    repeat(2382) { bytes.put32(24 * 2048 + 0x3576c + it * 4, 160) }
    bytes.put32(24 * 2048 + 0x3576c + stage.mapFile!! * 4, if (mapPresent) 160 else 180)
    bytes.put32(24 * 2048 + 0x3576c + stage.imageFile!! * 4, 164)
    bytes.put32(24 * 2048 + 0x3576c + (stage.sheetEntry!! ushr 16) * 4, 166)
    val pvd = 16 * 2048
    bytes[pvd] = 1; "CD001".toByteArray().copyInto(bytes, pvd + 1); bytes[pvd + 6] = 1
    bytes.put32(pvd + 80, 256); bytes.put16(pvd + 128, 2048); bytes.put32(pvd + 140, 18)
    record(20, 2048, byteArrayOf(0), true).copyInto(bytes, pvd + 156)
    var pathOffset = 18 * 2048
    listOf(byteArrayOf(0), "AAA".toByteArray(), "DAT".toByteArray(), "Z_STAGE".toByteArray()).forEachIndexed { index, name ->
        bytes[pathOffset] = name.size.toByte(); bytes.put32(pathOffset + 2, 20 + index); bytes.put16(pathOffset + 6, maxOf(1, index))
        name.copyInto(bytes, pathOffset + 8); pathOffset += 8 + name.size + (name.size and 1)
    }
    bytes.put32(pvd + 132, pathOffset - 18 * 2048)
    repeat(4) { index ->
        var offset = (20 + index) * 2048
        val entries = mutableListOf(record(20 + index, 2048, byteArrayOf(0), true), record(20, 2048, byteArrayOf(1), true))
        if (index == 0) {
            entries.add(record(24, exeSize, "SLES_039.36;1".toByteArray()))
            if (imagePresent) entries.add(record(164, 4, "IMAGE.BIN;1".toByteArray()))
            if (sheetPresent) entries.add(record(166, 4, "SHEET.BIN;1".toByteArray()))
        }
        if (index == 3) entries.add(record(160, 2064, "S206PACK.BIN;1".toByteArray()))
        entries.forEach { it.copyInto(bytes, offset); offset += it.size }
    }
    mapBytes().copyInto(bytes, 160 * 2048)
    val image = bytes
    return DiscFiles(object : Disc {
        override fun read(lba: Int, bytes: Int): ByteArray = image.copyOfRange(lba * 2048, lba * 2048 + bytes)
        override fun close() = Unit
    })
}

private fun record(lba: Int, size: Int, name: ByteArray, directory: Boolean = false): ByteArray =
    ByteArray(33 + name.size + if (name.size % 2 == 0) 1 else 0).apply {
        this[0] = this.size.toByte(); put32(2, lba); put32(10, size)
        this[25] = if (directory) 2 else 0; this[32] = name.size.toByte(); name.copyInto(this, 33)
    }
private fun ByteArray.put32(offset: Int, value: Int) { ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value) }
private fun ByteArray.put16(offset: Int, value: Int) { ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value.toShort()) }
