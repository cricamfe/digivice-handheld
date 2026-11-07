package dev.cricamfe.digivice.core.memory.converters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class HexStringToLongConverterTests {
    @Serializable
    private data class Wrapper(
        @SerialName("Value")
        @Serializable(with = HexStringToLongConverter::class)
        val value: Long,
    )

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "\"0x7FFA20\"|8387104",
            "\"8387104\"|8387104",
            "\"-5\"|-5",
            "\"\"|0",
            "null|0",
            "\"invalid\"|0",
        ],
    )
    fun `read should correctly parse json values`(jsonValue: String, expected: Long) {
        val json = "{\"Value\": $jsonValue}"

        val result = Json.decodeFromString<Wrapper>(json)

        assertEquals(expected, result.value)
    }

    @Test
    fun `write should serialize to hexadecimal string`() {
        val json = Json.encodeToString(Wrapper(8387104L))

        assertTrue(json.contains("\"Value\":\"0x7FFA20\""), json)
    }
}
