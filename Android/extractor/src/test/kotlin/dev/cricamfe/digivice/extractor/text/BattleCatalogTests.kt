package dev.cricamfe.digivice.extractor.text

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.openDisc
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.security.MessageDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
class BattleCatalogTests {
    @Test fun `PAL executable decodes all techniques and forms with five disc languages`() {
        val file = File(System.getenv("DW3_ISO"))
        val catalog = requireNotNull(openDisc(FileByteSource(file))).use { disc ->
            Json.decodeFromString<BattleCatalog>(buildBattleCatalog(DiscFiles(disc)))
        }
        // Facts checked against the former EU catalog independently of the executable reader.
        assertEquals("1656ec23fdcc5a472c5c76ac5f77992a9931389758dd04395bab3e16f4ca5ce0", hash(Json.encodeToString(catalog.techniques)))
        assertEquals("0614309187e9a0e1b3bfbbb3e6133f06df5666850dfac947a3731c8992b0f143", hash(Json.encodeToString(catalog.forms)))
        assertEquals(listOf(383, 385, 384, 3, 145, 366, 373, 31), catalog.partners)
        assertEquals(GameLanguage.entries.map { it.locale }.toSet(), catalog.words.keys)
        for (words in catalog.words.values) {
            assertEquals(443, words.names.size)
            assertEquals(443, words.descriptions.size)
            assertEquals(251, words.forms.size)
        }
        assertNotEquals(catalog.words.getValue("en-US").descriptions, catalog.words.getValue("es-ES").descriptions)
    }

    private fun hash(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.encodeToByteArray()).joinToString("") { "%02x".format(it) }
}
