package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.openDisc
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
class BattleAnimationTests {
    @Test fun `each enemy has 48 padded idle frames and the boss really moves`() {
        checkNotNull(openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))).use { disc ->
            val files = DiscFiles(disc)
            val models = FighterModels(files, GameFileTable(files))
            assertEquals(173, models.enemies.size)
            assertTrue(444 in models.enemies)
            for (id in models.enemies) {
                val atlas = models.renderBattleAnimation(id)
                val side = 176
                assertEquals(side * 8, atlas.width, "Enemy $id width")
                assertEquals(side * 6, atlas.height, "Enemy $id height")
                for (frame in 0 until 48) {
                    val frameX = frame % 8 * side
                    val frameY = frame / 8 * side
                    var visible = false
                    for (y in 0 until side) for (x in 0 until side) {
                        if (atlas.argb[(frameY + y) * atlas.width + frameX + x] ushr 24 == 0) continue
                        visible = true
                        check(x in 4 until side - 4 && y in 4 until side - 4) {
                            "Enemy $id clips at frame $frame, pixel $x,$y"
                        }
                    }
                    assertTrue(visible, "Enemy $id has an empty frame $frame")
                }
                if (id == 444 || id == 76) {
                    assertTrue((0 until side).any { y -> (0 until side).any { x ->
                        val row = y * atlas.width
                        atlas.argb[row + x] != atlas.argb[row + 3 * side * atlas.width + x]
                    } }, "Enemy $id has identical idle poses")
                }
            }
            assertTrue(models.render(444, 320).argb.any { it ushr 24 != 0 })
        }
    }
}
