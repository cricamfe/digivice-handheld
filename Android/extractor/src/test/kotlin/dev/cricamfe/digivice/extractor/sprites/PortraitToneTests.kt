package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.disc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import javax.imageio.ImageIO

class PortraitToneTests {
    @Test fun `moderate shading preserves transparent pixels linework highlights and source`() {
        val pixels = intArrayOf(0x00123456, 0xff101010.toInt(), 0xffaaaaaa.toInt(), 0xffffffff.toInt(),
            0xff646464.toInt(), 0xffa0a0a0.toInt(), 0x7f2864a0)
        val original = pixels.copyOf()
        val result = shadePortrait(Picture(7, 1, pixels))
        assertEquals(7, result.width)
        assertEquals(1, result.height)
        assertArrayEquals(original, pixels)
        assertArrayEquals(intArrayOf(0x00123456, 0xff101010.toInt(), 0xffaaaaaa.toInt(), 0xffffffff.toInt(),
            0xff575757.toInt(), 0xff9d9d9d.toInt(), 0x7f225689), result.argb)
    }

    @Test fun `the tone curve preserves grayscale order and alpha at every level`() {
        val pixels = IntArray(256) { (it shl 24) or (it shl 16) or (it shl 8) or it }
        val result = shadePortrait(Picture(256, 1, pixels))
        var previous = -1
        for ((value, color) in result.argb.withIndex()) {
            val tone = color and 255
            assertEquals(value, color ushr 24)
            assertEquals(tone, color ushr 8 and 255)
            assertEquals(tone, color ushr 16 and 255)
            assertTrue(tone in previous..value)
            if (value <= 16 || value >= 170) assertEquals(value, tone)
            previous = tone
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
    fun `all eight shaded portraits match the selected preview pixel for pixel`() {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val files = DiscFiles(disc)
            for ((key, raw) in partnerPortraits(FieldSprites(files, GameFileTable(files)))) {
                val actual = shadePortrait(raw)
                val reference = ImageIO.read(File(System.getenv("DW3_REFERENCE"), "$key.png"))
                assertEquals(reference.width, actual.width, key)
                assertEquals(reference.height, actual.height, key)
                assertArrayEquals(reference.getRGB(0, 0, actual.width, actual.height, null, 0, actual.width), actual.argb, key)
            }
        }
    }
}
