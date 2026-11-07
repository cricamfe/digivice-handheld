package dev.cricamfe.digivice.core.memory.converters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HexOrIntStringToIntConverterTests {
    @Serializable
    private data class Wrapper(
        @SerialName("Value")
        @Serializable(with = HexOrIntStringToIntConverter::class)
        val value: Int,
    )

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "20|20",
            "\"20\"|20",
            "\"0x14\"|20",
            "\"-5\"|-5",
            "\"\"|0",
            "null|0",
            "\"invalid\"|0",
        ],
    )
    fun `read should correctly parse json values`(jsonValue: String, expected: Int) {
        val json = "{\"Value\": $jsonValue}"

        val result = Json.decodeFromString<Wrapper>(json)

        assertEquals(expected, result.value)
    }

    @Test
    fun `write should serialize to numeric value`() {
        val json = Json.encodeToString(Wrapper(20))

        assertTrue(json.contains("\"Value\":20"), json)
    }
}
