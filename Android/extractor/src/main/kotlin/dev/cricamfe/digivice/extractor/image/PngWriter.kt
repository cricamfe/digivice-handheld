package dev.cricamfe.digivice.extractor.image

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

fun Picture.writePng(out: OutputStream) {
    val output = DataOutputStream(out)
    output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
    fun chunk(type: String, data: ByteArray) {
        val name = type.toByteArray(Charsets.US_ASCII)
        output.writeInt(data.size)
        output.write(name)
        output.write(data)
        val crc = CRC32().apply { update(name); update(data) }
        output.writeInt(crc.value.toInt())
    }
    val header = ByteArrayOutputStream()
    DataOutputStream(header).apply { writeInt(width); writeInt(height); write(byteArrayOf(8, 6, 0, 0, 0)) }
    chunk("IHDR", header.toByteArray())
    val compressed = ByteArrayOutputStream()
    DeflaterOutputStream(compressed).use { deflater ->
        val row = ByteArray(1 + width * 4)
        repeat(height) { y ->
            repeat(width) { x ->
                val color = argb[y * width + x]
                val offset = 1 + x * 4
                row[offset] = (color shr 16).toByte()
                row[offset + 1] = (color shr 8).toByte()
                row[offset + 2] = color.toByte()
                row[offset + 3] = (color ushr 24).toByte()
            }
            deflater.write(row)
        }
    }
    chunk("IDAT", compressed.toByteArray())
    chunk("IEND", byteArrayOf())
}
