package dev.cricamfe.digivice.extractor.image

import dev.cricamfe.digivice.extractor.maps.MapFile
import dev.cricamfe.digivice.extractor.maps.blendColor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

class ImageTests {
    @Test fun `tim 4 and 8 bit decode palettes and replicate five bit colours`() {
        for (bits in listOf(0, 1)) {
            val tim = Tim.parse(tim(bits))
            assertEquals(if (bits == 0) 4 else 2, tim.width)
            val pixels = tim.picture().argb
            assertEquals(0xff848484.toInt(), pixels[0])
            assertEquals(0xffff0000.toInt(), pixels[1])
        }
    }

    @Test fun `tim transparency preserves opaque black and the semitransparent bit`() {
        val tim = Tim.parse(tim(0))
        val pixels = tim.picture().argb
        assertEquals(0, pixels[2])
        assertEquals(0xff000000.toInt(), pixels[3])
        assertEquals(0x8000, tim.pixels16()[3])
    }

    @Test fun `tim rejects internally short palette and image payloads instead of padding`() {
        val palette = tim(0).apply { ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putShort(16, 17) }
        val image = tim(1).let { original -> original.copyOf(original.size - 1).apply {
            ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(8 + 12 + 256 * 2, 13)
        } }
        listOf(palette, image).forEach { assertThrows(java.io.IOException::class.java) { Tim.parse(it) } }
    }

    @Test fun `png round trips through ImageIO including alpha`() {
        val expected = intArrayOf(0, 0xffff0000.toInt(), 0x7f123456, 0xffabcdef.toInt())
        val out = ByteArrayOutputStream()
        Picture(2, 2, expected).writePng(out)
        val image = ImageIO.read(ByteArrayInputStream(out.toByteArray()))
        assertArrayEquals(expected, image.getRGB(0, 0, 2, 2, null, 0, 2))
    }

    @Test fun `rlen supports literals and repeats and rejects invalid output lengths`() {
        val bytes = "RLEN".toByteArray() + le(5) + byteArrayOf(2, 1, 2, 0x83.toByte(), 9)
        assertArrayEquals(byteArrayOf(1, 2, 9, 9, 9), Rlen.decode(bytes))
        listOf(bytes.copyOf(9), "RLEN".toByteArray() + le(Int.MAX_VALUE), "RLEN".toByteArray() + le(1) + byteArrayOf(0x82.toByte(), 7))
            .forEach { assertThrows(java.io.IOException::class.java) { Rlen.decode(it) } }
    }

    @Test fun `pack offsets stop at zero and reject overlapping or oversized tables`() {
        val bytes = le(12, 14, 0) + byteArrayOf(1, 2, 3)
        val entries = Pack.entries(bytes)
        assertEquals(2, entries.size)
        assertArrayEquals(byteArrayOf(1, 2), entries[0])
        assertArrayEquals(byteArrayOf(3), entries[1])
        val terminated = Pack.entries(le(16, 0, 18, 0) + byteArrayOf(1, 2, 3))
        assertEquals(1, terminated.size)
        assertArrayEquals(byteArrayOf(1, 2), terminated.single())
        listOf(le(Int.MAX_VALUE), le(8, 4), le(8, 999)).forEach {
            assertThrows(java.io.IOException::class.java) { Pack.entries(it) }
        }
    }

    @Test fun `malformed pictures tims and map dimensions fail before large allocations`() {
        assertThrows(IllegalArgumentException::class.java) { Picture(Int.MAX_VALUE, 2, intArrayOf()) }
        assertThrows(java.io.IOException::class.java) { Tim.parse(tim(1).copyOf(20)) }
        val largeTim = tim(1).apply { this[size - 6] = 0xff.toByte(); this[size - 5] = 0x7f }
        assertThrows(java.io.IOException::class.java) { Tim.parse(largeTim) }
        listOf(le(1, Int.MAX_VALUE, 1, 1), le(1, 1, 1, 0), le(0, 1, 1, 1)).forEach {
            assertThrows(java.io.IOException::class.java) { MapFile.parse(it) }
        }
    }

    @Test fun `an indexed stage archive preserves leading and internal holes`() {
        val bytes = le(0, 16, 0, 18) + byteArrayOf(1, 2, 3)
        assertEquals(emptyList<ByteArray>(), Pack.entries(bytes))
        assertNull(Pack.entry(bytes, 0))
        assertArrayEquals(byteArrayOf(1, 2), Pack.entry(bytes, 1))
        assertNull(Pack.entry(bytes, 2))
        assertArrayEquals(byteArrayOf(3), Pack.entry(bytes, 3))
        assertNull(Pack.entry(bytes, 4))
    }

    @Test fun `15 bit blending matches PS1 stp tint clipping and all abr modes`() {
        val back = 20 or (10 shl 5) or (5 shl 10)
        val front = 0x8000 or 12 or (20 shl 5) or (30 shl 10)
        fun rgb(r: Int, g: Int, b: Int) = r or (g shl 5) or (b shl 10)
        assertEquals(rgb(12, 20, 30), blendColor(back, front, null, -1))
        assertEquals(rgb(16, 15, 17), blendColor(back, front, null, 0))
        assertEquals(rgb(31, 30, 31), blendColor(back, front, null, 1))
        assertEquals(rgb(8, 0, 0), blendColor(back, front, null, 2))
        assertEquals(rgb(23, 15, 12), blendColor(back, front, null, 3))
        assertEquals(rgb(12, 20, 30), blendColor(back, front and 0x7fff, null, 0))
        assertEquals(rgb(6, 31, 0), blendColor(back, front, listOf(64, 255, 0, 1), -1))
        assertEquals(back, blendColor(back, 0, null, 0))
    }
}

private fun le(vararg values: Int): ByteArray = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
    values.forEach(::putInt)
}.array()

private fun tim(bits: Int): ByteArray {
    val palette = if (bits == 0) 16 else 256
    val bytes = ByteBuffer.allocate(8 + 12 + palette * 2 + 14).order(ByteOrder.LITTLE_ENDIAN)
    bytes.putInt(0x10).putInt(8 or bits)
    bytes.putInt(12 + palette * 2).putShort(0).putShort(0).putShort(palette.toShort()).putShort(1)
    repeat(palette) { bytes.putShort(when (it) { 0 -> 16 or (16 shl 5) or (16 shl 10); 1 -> 31; 3 -> 0x8000; else -> 0 }.toShort()) }
    bytes.putInt(14).putShort(0).putShort(0).putShort(1).putShort(1)
    bytes.put(if (bits == 0) 0x10 else 0).put(if (bits == 0) 0x32 else 1)
    return bytes.array()
}
