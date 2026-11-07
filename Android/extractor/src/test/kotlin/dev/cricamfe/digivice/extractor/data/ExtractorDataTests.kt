package dev.cricamfe.digivice.extractor.data

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.zip.GZIPInputStream

class ExtractorDataTests {
    @Test fun `there are 294 stages and each has a map file or none`() {
        assertEquals(294, ExtractorData.stages.size)
        assertEquals(setOf(260, 415, 815), ExtractorData.stages.filterValues { it.mapFile == null }.keys)
        assertTrue(ExtractorData.stages.values.all { it.mapFile == null || it.mapFile in 0 until 2382 })
        assertTrue(ExtractorData.stages.values.all { it.color == null || it.color.size == 4 })
        assertNull(ExtractorData.stage(-1))
    }

    @Test fun `representative stages retain their sprite setup and object fields`() {
        val stage = ExtractorData.stage(206)!!
        assertEquals(1195, stage.mapFile)
        assertEquals(78381056, stage.sheetEntry)
        assertEquals(1194, stage.imageFile)
        assertNull(stage.imageEntry)
        assertEquals(78381057, stage.floor0)
        assertEquals(listOf(128, 128, 128, 0), stage.color)
        assertEquals(47, stage.objects.size)
        assertEquals(StageObject(1, 0, 128, 2, 0, 0, 0, 0, 0, 0, 384, 384, 0, 0), stage.objects.first())
        assertEquals(1384, ExtractorData.stage(331)!!.mapFile)
        assertEquals(62, ExtractorData.stage(331)!!.objects.size)
        assertEquals(9542, ExtractorData.stages.values.sumOf { it.objects.size })
    }

    @Test fun `resource values contain only numbers nulls and containers`() {
        val input = javaClass.getResourceAsStream("/extractor/stages.json.gz")!!
        val json = GZIPInputStream(input).bufferedReader().use { Json.parseToJsonElement(it.readText()) }
        fun check(value: JsonElement) {
            when (value) {
                is JsonObject -> value.values.forEach(::check)
                is JsonArray -> value.forEach(::check)
                JsonNull -> Unit
                is JsonPrimitive -> assertTrue(!value.isString && value.intOrNull != null, value.toString())
            }
        }
        check(json)
    }
}
