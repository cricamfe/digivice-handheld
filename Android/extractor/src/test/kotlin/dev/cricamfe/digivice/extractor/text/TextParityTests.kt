package dev.cricamfe.digivice.extractor.text

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.FileByteSource
import dev.cricamfe.digivice.extractor.disc.openDisc
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
class TextParityTests {
    private val references = File(File(System.getenv("DW3_REFERENCE")).parentFile, "texts")

    @Test fun `all 195 language tables match independent TOML references except the documented Spanish escape`() = withText { text ->
        var lines = 0
        var knownDifferences = 0
        for (language in GameLanguage.entries) {
            val tables = read(File(references, "${language.locale}/tables.json"))
            assertEquals(39, tables.size, language.locale)
            var languageLines = 0
            for ((name, values) in tables) {
                val expected = values.jsonArray.map { it.jsonPrimitive.content }
                val actual = text.table(language, name)
                assertEquals(expected.size, actual.size, "${language.folder}/$name count")
                for (index in expected.indices) {
                    val location = "${language.folder}/$name:$index"
                    if (language == GameLanguage.SPN && name == "traini" && index == 101) {
                        // The TOML escapes bytes 05/0b instead of decoding their ordinary digit codes.
                        assertEquals(43, expected[index].length, location)
                        assertEquals(37, actual[index].length, location)
                        assertTrue(expected[index].contains("\\x05\\x0b"), location)
                        assertTrue(expected[index].replace("\\x05\\x0b", "17") == actual[index], location)
                        assertTrue(expected[index] != actual[index], location)
                        knownDifferences++
                    } else assertTrue(expected[index] == actual[index], location)
                }
                languageLines += actual.size
            }
            assertEquals(10_833, languageLines, language.locale)
            lines += languageLines
        }
        assertEquals(54_165, lines)
        assertEquals(1, knownDifferences)
    }

    @Test fun `messages match five reference locales and words preserve all four legacy translations through stable references`() = withText { text ->
        val outputs = buildTexts(text)
        assertEquals(GameLanguage.entries.map { it.locale }.toSet(), outputs.keys)
        // English literal keys exist only in these private fixtures, never in the shipped facts.
        val bridge = read(File(references, "legacy-word-refs.json"))
        assertEquals(2_066, bridge.size)
        for (language in GameLanguage.entries) {
            val output = Json.parseToJsonElement(outputs.getValue(language.locale)).jsonObject
            val messages = output.getValue("messages").jsonObject
            val expected = read(File(references, "${language.locale}/game.generated.json"))
            assertTrue(messages == expected, "${language.locale} messages differ")
            assertEquals(1_365, leaves(messages), language.locale)
            val words = output.getValue("words").jsonObject
            assertEquals(2_381, words.size, language.locale)
            val legacyKeys = bridge.values.map { it.jsonPrimitive.content }.toSet()
            assertTrue(words.keys.containsAll(legacyKeys), language.locale)
            // The rest are the speakers of the field's characters (ActorNames) and of the app's sentences (quest-words.ts)
            // that no table name reads already, the lines of the tables the app reads whole (WHOLE_TABLES of
            // generate_text_keys.py) and the menu lines the app's words name by themselves (the equipment slots of
            // game-words.ts, the field menu's "No technique." and the currency in STATUS, the kinds and the amounts in
            // ITSHOP, the card prices in CRDSHP, the battle command in MFIGHT).
            // The speakers of the story scenes (`dmg800/9:1`) are speakers too.
            val speakers = (words.keys - legacyKeys).filter { it.startsWith("talk") || it.startsWith("dmg") }
            assertEquals(54, speakers.size, language.locale)
            for (key in speakers) {
                assertTrue(key.matches(Regex("(talk0\\d|dmg\\d{3}/\\d+):\\d+")), key)
                assertTrue(words.getValue(key).jsonPrimitive.content.isNotBlank(), key)
            }
            val tables = read(File(references, "${language.locale}/tables.json"))
            val whole = words.keys - legacyKeys - speakers.toSet()
            assertEquals(mapOf("starea" to 21, "dignam" to 224, "status" to 7, "itshop" to 7, "mfight" to 1, "crdshp" to 1), whole.groupingBy { it.substringBefore(':') }.eachCount(), language.locale)
            for (key in whole) {
                val (table, line) = key.split(':')
                val expected = flat(tables.getValue(table).jsonArray[line.toInt()].jsonPrimitive.content)
                assertTrue(expected.isNotBlank() && words.getValue(key).jsonPrimitive.content == expected, "${language.locale} $key")
            }
            if (language == GameLanguage.ENG) continue
            val legacy = read(File(references, "${language.locale}/words.generated.json"))
            assertTrue(bridge.keys == legacy.keys, "${language.locale} private bridge differs")
            var different = 0
            for ((englishKey, value) in legacy) {
                val stableKey = bridge.getValue(englishKey).jsonPrimitive.content
                if (value != words[stableKey]) different++
            }
            assertEquals(0, different, "${language.locale} word values differ")
        }
    }

    private fun read(file: File): JsonObject = Json.parseToJsonElement(file.readText()).jsonObject

    private fun leaves(value: JsonElement): Int =
        if (value is JsonObject) value.values.sumOf { leaves(it) } else 1

    private fun withText(block: (GameText) -> Unit) {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc -> block(GameText(DiscFiles(disc))) }
    }
}
