package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.disc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

class WorldMapParityTests {
    @Test fun `oversized world resources are rejected before reading or parsing`() {
        for (size in listOf(-1, 0, 3, 1_048_577, Int.MAX_VALUE)) {
            assertThrows(IOException::class.java) { checkWorldResourceSize(size) }
        }
        assertThrows(IOException::class.java) { checkWorldResourceSize(98715, 98716) }
        checkWorldResourceSize(4)
        checkWorldResourceSize(100936, 98716)
        checkWorldResourceSize(1_048_576)
    }

    @Test fun `world map tables reject truncation and invalid markers`() {
        assertThrows(IOException::class.java) { worldMapTables(ByteArray(98532)) }
        val bytes = ByteArray(98716)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(97968 + 12, 256)
        assertThrows(IOException::class.java) { worldMapTables(bytes) }
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(97968 + 12, 1).putInt(97968 + 16, -1)
        assertThrows(IOException::class.java) { worldMapTables(bytes) }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
    fun `both full and base world maps equal Python pixel by pixel`() {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val maps = renderWorldMaps(DiscFiles(disc))
            assertEquals(setOf("world-asuka", "world-asuka-base", "world-amaterasu", "world-amaterasu-base"), maps.keys)
            for ((key, actual) in maps) {
                val reference = ImageIO.read(File(System.getenv("DW3_REFERENCE"), "../world/$key.png"))
                assertEquals(reference.width, actual.width, key)
                assertEquals(reference.height, actual.height, key)
                assertArrayEquals(reference.getRGB(0, 0, actual.width, actual.height, null, 0, actual.width), actual.argb, key)
            }
        }
    }
}
