package dev.cricamfe.digivice.extractor.text

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TextTests {
    @Test fun `a table with accents and control codes decodes`() {
        val bytes = table(byteArrayOf(0x0e, 0x01, 0x51, 0x02, 0x01, 0x02, 0x07, 0x0f))
        assertEquals(listOf("Aé\n[name]B"), decodeTextTable(bytes))
    }

    @Test fun `an unknown code is preserved without crashing`() {
        assertEquals(listOf("\\xff"), decodeTextTable(table(byteArrayOf(0xff.toByte()))))
    }

    @Test fun `zero terminates even if a charset entry contains zero`() {
        assertEquals(listOf("\\x02"), decodeTextTable(table(byteArrayOf(0x02))))
    }

    @Test fun `malformed counts offsets and unterminated strings are rejected`() {
        for (bytes in listOf(
            byteArrayOf(), byteArrayOf(-1, -1, -1, -1),
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(4).array(),
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(8).array(),
            table(byteArrayOf(0x41)).copyOf(9),
        )) assertThrows(IOException::class.java) { decodeTextTable(bytes) }
    }

    @Test fun `a scene archive gives each of its text tables`() {
        val first = table(byteArrayOf(0x0e))
        val second = table(byteArrayOf(0x02))
        val archive = ByteBuffer.allocate(8 + first.size + second.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(8).putInt(8 + first.size).put(first).put(second).array()
        assertArrayEquals(first, archiveEntry(archive, 0))
        assertArrayEquals(second, archiveEntry(archive, 1))
        assertEquals(listOf("A"), decodeTextTable(archiveEntry(archive, 0)))
    }

    @Test fun `malformed scene archives and missing entries are rejected`() {
        val archive = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(4).putInt(0).array()
        for ((bytes, index) in listOf(
            byteArrayOf() to 0,
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(6).array() to 0,
            ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(64).array() to 0,
            ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(8).putInt(4).array() to 1,
            archive to 1,
        )) assertThrows(IOException::class.java) { archiveEntry(bytes, index) }
    }

    @Test fun `oversized or overlapping string expansions are rejected`() {
        assertThrows(IOException::class.java) { decodeTextTable(table(ByteArray(65_537) { 0x41 })) }
        val count = 256
        val bytes = ByteBuffer.allocate(4 + count * 4 + 65_001).order(ByteOrder.LITTLE_ENDIAN)
        bytes.putInt(count)
        repeat(count) { bytes.putInt(4 + count * 4) }
        repeat(65_000) { bytes.put(0x41) }
        bytes.put(0)
        assertThrows(IOException::class.java) { decodeTextTable(bytes.array()) }
    }

    @Test fun `shipped word locations contain only table coordinates and the two explicit part selectors`() {
        val resource = javaClass.getResourceAsStream("/extractor/text-keys.json")!!
        val facts = resource.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
        val words = facts.getValue("words").jsonArray.map { it.jsonObject }
        assertEquals(2_381, words.size)
        val pieces = mutableSetOf<String>()
        for (word in words) {
            val table = word.getValue("table").jsonPrimitive.content
            val line = word.getValue("line").jsonPrimitive.int
            val key = word.getValue("key").jsonPrimitive.content
            assertTrue(table.matches(Regex("[a-z0-9]+(/[0-9]+)?")))
            assertTrue(line >= 0)
            val selector = word["selector"]?.jsonPrimitive?.content
            if (selector?.startsWith("part:") == true) {
                pieces += key
                assertEquals("$table:$line:$selector", key)
            } else {
                assertTrue(selector == null || selector == "speaker")
                assertEquals("$table:$line", key)
            }
        }
        assertEquals(setOf("htlnam:17:part:0", "htlnam:17:part:1"), pieces)
    }

    private fun table(line: ByteArray): ByteArray =
        ByteBuffer.allocate(9 + line.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(1).putInt(8).put(line).put(0).array()
}
