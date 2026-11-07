package dev.cricamfe.digivice.extractor.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.zip.GZIPInputStream

@Serializable
data class StageData(
    val mapFile: Int?,
    val sheetEntry: Int?,
    val imageFile: Int?,
    val imageEntry: Int?,
    val floor0: Int?,
    val color: List<Int>?,
    val objects: List<StageObject>,
)

@Serializable
data class StageObject(
    val visible: Int,
    val anim: Int,
    val margin: Int,
    val depth: Int,
    val frame: Int,
    val cycle: Int,
    val cycleFirst: Int,
    val cycleLast: Int,
    val cycleDelay: Int,
    val clutRow: Int,
    val x: Int,
    val y: Int,
    val sortY: Int,
    val cycleTime: Int,
)

object ExtractorData {
    val stages: Map<Int, StageData> by lazy {
        val input = checkNotNull(javaClass.getResourceAsStream("/extractor/stages.json.gz"))
        GZIPInputStream(input).bufferedReader().use { Json.decodeFromString(it.readText()) }
    }

    fun stage(mode: Int): StageData? = stages[mode]
}
