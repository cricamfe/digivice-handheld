package dev.cricamfe.digivice.core.memory.converters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HexStringListToLongListConverterTests {
    @Serializable
    private data class Wrapper(
        @SerialName("BitMasks")
        @Serializable(with = HexStringListToLongListConverter::class)
        val bitMasks: List<Long>,
    )

    @Test
    fun `read should deserialize hex string array`() {
        val json = """
            {
                "BitMasks": ["0x01", "0x08"]
            }
        """.trimIndent()

        val result = Json.decodeFromString<Wrapper>(json)

        assertEquals(listOf(0x01L, 0x08L), result.bitMasks)
    }

    @Test
    fun `read should deserialize empty array`() {
        val json = """
            {
                "BitMasks": []
            }
        """.trimIndent()

        val result = Json.decodeFromString<Wrapper>(json)

        assertTrue(result.bitMasks.isEmpty())
    }

    @Test
    fun `write should serialize to hex string array`() {
        val json = Json.encodeToString(Wrapper(listOf(0x01L, 0x08L)))

        assertTrue(json.replace(" ", "").contains("\"BitMasks\":[\"0x1\",\"0x8\"]"), json)
    }

    @Test
    fun `read should skip empty entries and fall back to zero for invalid ones`() {
        val json = """{"BitMasks": ["0x01", "", "invalid", "8"]}"""

        val result = Json.decodeFromString<Wrapper>(json)

        assertEquals(listOf(0x01L, 0L, 8L), result.bitMasks)
    }

    @Test
    fun `read should return empty list when the value is not an array`() {
        val result = Json.decodeFromString<Wrapper>("""{"BitMasks": "0x01"}""")

        assertTrue(result.bitMasks.isEmpty())
    }
}
