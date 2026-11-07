package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.openDisc
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.writePng
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.function.Executable

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
class EvolutionTests {
    private val reference = File(System.getenv("DW3_REFERENCE")).parentFile.resolve("models/evolutions")
    private val selected = reference.resolve("accepted")

    @Test fun `four accepted previews equal the balanced cinematic prototype`() = withModels { models ->
        assertAll(listOf(5, 12, 144, 59).map { id -> Executable {
            compare(id, selected.resolve("$id.png"), models.renderEvolution(id, 320))
        } })
    }

    @Test fun `all 44 cinematic evolutions equal independent Python renders and retain margins`() = withModels { models ->
        assertEquals(44, models.partners.size)
        assertAll(models.partners.map { id -> Executable {
            val actual = models.renderEvolution(id, 320)
            compare(id, reference.resolve("$id.png"), actual)
            val visible = actual.argb.indices.filter { actual.argb[it] ushr 24 != 0 }
            assertTrue(visible.isNotEmpty(), "Empty evolution $id")
            assertTrue(visible.all { it % actual.width in 8 until actual.width - 8 &&
                it / actual.width in 8 until actual.height - 8 }, "Cropped evolution $id")
        } })
    }

    @Test fun `cinematic rendering rejects invalid sizes and non-evolution ids`() = withModels { models ->
        assertThrows(IllegalArgumentException::class.java) { models.renderEvolution(5, 0) }
        assertThrows(IllegalArgumentException::class.java) { models.renderEvolution(5, 481) }
        assertThrows(IllegalArgumentException::class.java) { models.renderEvolution(5, 961) }
        assertThrows(IllegalArgumentException::class.java) { models.renderEvolution(4, 320) }
        assertThrows(IllegalArgumentException::class.java) { models.renderEvolution(-1, 320) }
    }

    @Test fun `the largest cinematic size stays within the raster allocation budget`() = withModels { models ->
        val picture = models.renderEvolution(5, 480)
        assertTrue(picture.argb.any { it ushr 24 != 0 })
        assertTrue(picture.width <= 500 && picture.height <= 500)
    }

    private fun compare(id: Int, file: File, actual: Picture) {
        val expected = checkNotNull(ImageIO.read(file))
        assertEquals(expected.width, actual.width, "Evolution $id width")
        assertEquals(expected.height, actual.height, "Evolution $id height")
        val pixels = expected.getRGB(0, 0, expected.width, expected.height, null, 0, expected.width)
        val differences = pixels.indices.count { pixels[it] != actual.argb[it] }
        val alphaDifferences = pixels.indices.count { pixels[it] ushr 24 != actual.argb[it] ushr 24 }
        val maxRgbDifference = pixels.indices.maxOf { i ->
            listOf(0, 8, 16).maxOf { shift -> abs(((pixels[i] ushr shift) and 255) - ((actual.argb[i] ushr shift) and 255)) }
        }
        if (differences != 0) {
            File("build/evolution-differences").also { it.mkdirs() }.resolve("$id-actual.png")
                .outputStream().use(actual::writePng)
        }
        println("EVOLUTION_PARITY $id pixels=$differences/${pixels.size} maxRGB=$maxRgbDifference alphaDifferences=$alphaDifferences")
        // View/normal floating-point operations can place a saturated channel one ULP below its ceiling.
        // This new presentation path allows one RGB level; RAW model tests retain exact pixel equality.
        assertEquals(0, alphaDifferences, "Evolution $id alpha differences")
        assertTrue(maxRgbDifference <= 1, "Evolution $id maximum RGB difference: $maxRgbDifference")
    }

    private fun withModels(block: (FighterModels) -> Unit) {
        checkNotNull(openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))).use { disc ->
            val files = DiscFiles(disc)
            block(FighterModels(files, GameFileTable(files)))
        }
    }
}
