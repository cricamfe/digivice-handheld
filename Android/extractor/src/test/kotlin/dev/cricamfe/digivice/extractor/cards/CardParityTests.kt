package dev.cricamfe.digivice.extractor.cards

import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.Tim
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class CardHeaderTests {
    @Test fun `card header preserves unsigned stats and signed references`() {
        val bytes = byteArrayOf(6, 25, 13, 0, 0, 1, 5, 0, 0xff.toByte(), 0x7f, 0xff.toByte(), 0xff.toByte())
        val header = CardHeader.parse(bytes)
        assertEquals(6, header.color)
        assertEquals(25, header.ap)
        assertEquals(13, header.hp)
        assertEquals(32767, header.rank)
        assertEquals(-1, header.comboCard)
    }

    @Test fun `short and invalid card headers are rejected`() {
        assertThrows(IOException::class.java) { CardHeader.parse(ByteArray(11)) }
        assertThrows(IOException::class.java) { CardHeader.parse(ByteArray(12)) }
        assertThrows(IOException::class.java) { CardHeader.parse(ByteArray(12).also { it[0] = 1; it[3] = 127 }) }
    }
}

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
class CardParityTests {
    private val root = File(System.getProperty("digivice.repositoryRoot"))
    private val database = Json.parseToJsonElement(File(root, "Frontend/src/database/tcg/card.json").readText()).jsonObject

    @Test fun `card art equals the repo's at offset 4 2 within rounding`() = withCards { cards, files ->
        var absoluteError = 0L
        var samples = 0L
        val families = sortedMapOf<String, LongArray>()
        for ((id, value) in database) {
            val cardId = id.toInt()
            val image = cards.render(cardId)
            assertEquals(40, image.width)
            assertEquals(46, image.height)
            // The database renamed 1018 while its upstream PNG retains the old asset key.
            val name = if (id == "1018") "Volcanic Gatlin" else value.jsonObject.getValue("imageName").jsonPrimitive.content
            val reference = ImageIO.read(File(root, "Frontend/src/assets/cards/$name.png"))
            val discIndex = cardId - 1000
            val file = files.read(files.byPath("AAA/DAT/CARD/CARDPAK${discIndex / 64}.BIN")!!)
            val start = (discIndex % 64) * 1580
            val mask = Tim.parse(file.copyOfRange(start + 12, start + 1580)).pixels16()
            var artError = 0L
            var artSamples = 0L
            for (y in 2 until 34) for (x in 4 until 36) {
                // A transparent texel belongs to the underlying frame, not to the artwork.
                if (mask[(y - 2) * 32 + x - 4] == 0) continue
                val actual = image.argb[y * 40 + x]
                val expected = reference.getRGB(x, y)
                for (shift in listOf(0, 8, 16)) {
                    artError += kotlin.math.abs((actual shr shift and 255) - (expected shr shift and 255))
                    artSamples++
                }
            }
            assertTrue(artError.toDouble() / artSamples <= 0.5, "$id: ${artError.toDouble() / artSamples}/255")
            absoluteError += artError
            samples += artSamples
            val family = "${cards.header(cardId).color}-${value.jsonObject.getValue("type").jsonPrimitive.content}"
            for (y in 0 until 46) for (x in 0 until 40) {
                val region = if (y >= 34) "strip" else if (x in 4..35 && y in 2..33 && mask[(y - 2) * 32 + x - 4] != 0) "art" else "frame"
                val stats = families.getOrPut("$family-$region") { LongArray(4) }
                val actualAlpha = image.argb[y * 40 + x] ushr 24
                val expectedAlpha = reference.getRGB(x, y) ushr 24
                if (actualAlpha != expectedAlpha) stats[3]++
                if (actualAlpha == 0 || expectedAlpha == 0) continue
                for (shift in listOf(0, 8, 16)) {
                    val error = kotlin.math.abs((image.argb[y * 40 + x] shr shift and 255) - (reference.getRGB(x, y) shr shift and 255))
                    stats[0] += error
                    stats[1]++
                    stats[2] = maxOf(stats[2], error.toLong())
                }
            }
        }
        assertEquals(314, database.size)
        assertTrue(absoluteError.toDouble() / samples <= 0.5, "Mean card art difference: ${absoluteError.toDouble() / samples}/255")
        println("314 card art mean difference: ${absoluteError.toDouble() / samples}/255")
        for ((family, stats) in families) println("Card $family: visible MAE=${stats[0].toDouble() / stats[1]}/255 max=${stats[2]} alphaMismatch=${stats[3]}")
    }

    @Test fun `card id is 1000 plus disc index and ap hp come from the card header`() = withCards { cards, _ ->
        assertEquals(5, cards.header(1224).ap)
        assertEquals(3, cards.header(1224).hp)
        for ((id, value) in database) {
            val header = cards.header(id.toInt())
            val points = value.jsonObject["points"]?.jsonObject ?: continue
            assertEquals(points.getValue("ap").jsonPrimitive.int, header.ap, id)
            assertEquals(points.getValue("hp").jsonPrimitive.int, header.hp, id)
        }
        assertThrows(IOException::class.java) { cards.render(999) }
        assertThrows(IOException::class.java) { cards.render(1316) }
    }

    @Test fun `extracted catalogue matches the type and points of all 314 playable cards`() = withCards { cards, _ ->
        val actual = Json.parseToJsonElement(buildCardFacts(cards)).jsonObject
        assertEquals(database.keys, actual.keys)
        for ((id, expectedValue) in database) {
            val expected = expectedValue.jsonObject
            val fact = actual.getValue(id).jsonObject
            assertEquals(expected["type"], fact["type"], id)
            assertEquals(expected["points"], fact["points"], id)
            assertFalse("imageName" in fact, id)
        }
    }

    private fun withCards(block: (CardImages, DiscFiles) -> Unit) {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val files = DiscFiles(disc)
            block(CardImages(files), files)
        }
    }
}
