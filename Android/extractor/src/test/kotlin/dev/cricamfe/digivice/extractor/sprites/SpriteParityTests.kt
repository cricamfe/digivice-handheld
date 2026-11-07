package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.Picture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import javax.imageio.ImageIO

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
class SpriteParityTests {
    private val root = File(System.getProperty("digivice.repositoryRoot"))

    @Test fun `actors equal the repo's PNGs`() = withFiles { files, table ->
        val actual = FieldSprites(files, table).all().filter { it.first.startsWith("actors/") }.toMap()
        val references = File(root, "Frontend/src/handheld/assets/actors").listFiles()!!.filter { it.extension == "png" }
        assertEquals(2472, references.size)
        assertEquals(references.map { "actors/${it.nameWithoutExtension}" }.toSet(), actual.keys)
        for (reference in references) assertExact(reference, actual.getValue("actors/${reference.nameWithoutExtension}"))
    }

    @Test fun `items equal the repo's PNGs`() = withFiles { files, table ->
        val actual = ItemIcons(files, table).all().toMap()
        val references = File(root, "Frontend/src/handheld/assets/items").listFiles()!!.filter { it.extension == "png" }
        assertEquals(24, references.size)
        assertEquals(references.map { "items/${it.nameWithoutExtension}" }.toSet(), actual.keys)
        for (reference in references) assertExact(reference, actual.getValue("items/${reference.nameWithoutExtension}"))
    }

    @Test fun `tamers npcs bosses stores and duel island have a sprite`() = withFiles { files, table ->
        val sprites = FieldSprites(files, table).all().filter { it.first.startsWith("actors/") }.toMap()
        val replacements = setOf("Bob", "Genji", "Natsumi", "Tomomi", "Wong")
        // No character has an alias named after it: the app's tables give their character (`fieldActor`). The pictures
        // of `Frontend/src/assets`, from the original project, are named after them; the five replacements are upstream
        // edits of the same character.
        val digimon = mapOf(
            "Datamon" to "165-0", "HiAndromon" to "150-0", "MasterTyrannomon" to "90-0", "Pharaohmon" to "59-0",
            "WaruMonzaemon" to "151-0", "Zanbamon" to "133-0", "Divermon" to "206-0", "KingDivermon" to "99-0",
        )
        val people = mapOf(
            "Akiba" to "51-1", "Alice" to "46-1", "Andy" to "45-1", "Bob" to "57-1", "Catherine" to "46-1",
            "Chris" to "47-1", "Genji" to "57-1", "George" to "49-1", "Gloria" to "50-1", "Gordon" to "53-1",
            "Jessica" to "58-1", "Lucia" to "56-1", "Mei Lin" to "50-1", "Mitch" to "47-1", "Nacky" to "52-1",
            "Nakano" to "51-1", "Natsumi" to "48-1", "Robert" to "53-1", "Steve" to "47-1", "Tomomi" to "48-1",
            "Wong" to "55-1", "A.o.A Attacker" to "194-1", "Byakko Leader" to "122-1", "Fake Byakko Leader" to "152-1",
            "Game Master" to "108-1", "Guard" to "109-1", "Keith" to "102-1", "Seiryu Leader" to "60-1",
            "Soldier" to "115-1", "Suzaku Leader" to "141-1", "Trooper" to "164-1",
        )
        val database = File(root, "Frontend/src/database")
        val fieldActors = listOf(
            "enemy.json", "shop/card-shop.json", "npc/duel-island.json", "npc/npc.json", "npc/tamer.json",
        ).flatMap { file ->
            Regex("\"fieldActor\": \"([0-9]+-[0-7])\"").findAll(File(database, file).readText()).map { it.groupValues[1] }
        }.toSet()
        assertEquals((digimon.values + people.values).toSet(), fieldActors)
        var count = 0
        for (group in listOf("tamers", "npcs", "bosses", "stores", "duel-island")) {
            for (reference in File(root, "Frontend/src/assets/$group").listFiles()!!.filter { it.extension == "png" }) {
                count++
                val name = reference.nameWithoutExtension
                val actual = sprites.getValue("actors/" + (digimon[name] ?: people.getValue(name)))
                assertTrue(actual.argb.any { it ushr 24 != 0 }, reference.name)
                if (reference.nameWithoutExtension in replacements) continue
                val expected = ImageIO.read(reference)
                val pixels = expected.getRGB(0, 0, expected.width, expected.height, null, 0, expected.width)
                val a = trimmed(actual.width, actual.height, actual.argb)
                val b = trimmed(expected.width, expected.height, pixels)
                assertEquals(b.width, a.width, reference.name)
                assertEquals(b.height, a.height, reference.name)
                var error = 0L
                var channels = 0L
                for (i in a.argb.indices) {
                    assertEquals(b.argb[i] ushr 24, a.argb[i] ushr 24, reference.name)
                    if (a.argb[i] ushr 24 == 0) continue
                    for (shift in listOf(0, 8, 16)) {
                        error += kotlin.math.abs((a.argb[i] shr shift and 255) - (b.argb[i] shr shift and 255))
                        channels++
                    }
                }
                assertTrue(error.toDouble() / channels <= 4.0, "${reference.name}: ${error.toDouble() / channels}")
            }
        }
        assertEquals(40, count)
    }

    private fun assertExact(file: File, actual: Picture) {
        val expected = ImageIO.read(file)
        assertEquals(expected.width, actual.width, file.name)
        assertEquals(expected.height, actual.height, file.name)
        assertArrayEquals(expected.getRGB(0, 0, actual.width, actual.height, null, 0, actual.width), actual.argb, file.name)
    }

    private fun trimmed(width: Int, height: Int, pixels: IntArray): Picture {
        val used = pixels.indices.filter { pixels[it] ushr 24 != 0 }
        val x0 = used.minOf { it % width }; val x1 = used.maxOf { it % width }
        val y0 = used.minOf { it / width }; val y1 = used.maxOf { it / width }
        assertTrue(y1 < height)
        return Picture(x1 - x0 + 1, y1 - y0 + 1, IntArray((x1 - x0 + 1) * (y1 - y0 + 1)) { i ->
            pixels[(y0 + i / (x1 - x0 + 1)) * width + x0 + i % (x1 - x0 + 1)]
        })
    }

    private fun withFiles(block: (DiscFiles, GameFileTable) -> Unit) {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val files = DiscFiles(disc)
            block(files, GameFileTable(files))
        }
    }
}
