package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.disc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import javax.imageio.ImageIO

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
class PortraitTests {
    @Test fun `all eight partner portraits are their field sprites with transparent background`() {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val files = DiscFiles(disc)
            val portraits = partnerPortraits(FieldSprites(files, GameFileTable(files)))
            val characters = listOf(182, 180, 181, 34, 35, 64, 65, 66)
            assertEquals((0..7).map { "portraits/$it" }.toSet(), portraits.keys)
            for ((id, character) in characters.withIndex()) {
                val actual = portraits.getValue("portraits/$id")
                val reference = ImageIO.read(File(System.getProperty("digivice.repositoryRoot"), "Frontend/src/handheld/assets/actors/$character-1.png"))
                assertEquals(reference.width, actual.width)
                assertEquals(reference.height, actual.height)
                assertArrayEquals(reference.getRGB(0, 0, actual.width, actual.height, null, 0, actual.width), actual.argb)
                assertTrue(actual.argb.any { it ushr 24 == 0 })
                assertTrue(actual.argb.any { it ushr 24 == 255 })
            }
        }
    }
}
