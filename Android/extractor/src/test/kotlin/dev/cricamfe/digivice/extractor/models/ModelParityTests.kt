package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.openDisc
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.writePng
import java.io.File
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.function.Executable

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
class ModelParityTests {
    private val reference = File(System.getenv("DW3_REFERENCE")).parentFile.resolve("models")

    @Test fun `all 172 enemies equal the Python reference`() = withFiles { files, table ->
        val models = FighterModels(files, table)
        val referenceIds = models.enemies.filterNot { it == 444 }
        assertEquals(172, referenceIds.size)
        assertAll(referenceIds.map { id -> Executable { compare("enemy-$id", models.render(id, 320)) } })
    }

    @Test fun `all 44 evolution forms render and equal the Python reference`() = withFiles { files, table ->
        val models = FighterModels(files, table)
        assertEquals(44, models.partners.size)
        assertTrue(models.enemies.intersect(models.partners.toSet()).isEmpty())
        assertAll(models.partners.map { id -> Executable {
            val picture = models.render(id, 320)
            assertTrue(picture.argb.any { it ushr 24 != 0 }, "Empty partner $id")
            compare("partner-$id", picture)
        } })
    }

    @Test fun `all 28 battle stages equal the Python reference`() = withFiles { files, table ->
        val models = StageModels(files, table)
        assertEquals(28, models.stages.size)
        assertAll(models.stages.map { id -> Executable { compare("stage-$id", models.render(id)) } })
    }

    private fun compare(name: String, actual: Picture) {
        val expected = checkNotNull(ImageIO.read(reference.resolve("$name.png")))
        assertEquals(expected.width, actual.width, "$name width")
        assertEquals(expected.height, actual.height, "$name height")
        val pixels = expected.getRGB(0, 0, expected.width, expected.height, null, 0, expected.width)
        val differences = pixels.indices.count { pixels[it] != actual.argb[it] }
        if (differences != 0) {
            val output = File("build/model-differences").also { it.mkdirs() }
            output.resolve("$name-actual.png").outputStream().use(actual::writePng)
            println("MODEL_DIFF $name pixels=$differences/${pixels.size}")
        }
        assertEquals(0, differences, "$name differing pixels")
    }

    private fun withFiles(block: (DiscFiles, GameFileTable) -> Unit) {
        checkNotNull(openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))).use { disc ->
            val files = DiscFiles(disc)
            block(files, GameFileTable(files))
        }
    }
}
