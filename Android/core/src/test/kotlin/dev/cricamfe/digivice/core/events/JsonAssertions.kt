package dev.cricamfe.digivice.core.events

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.fail

/**
 * Strict comparison of two JSON documents as trees: object key order does not matter, but everything else does
 * (`null` is not the same as an absent key, `1` is not `"1"`, array order and length). On a mismatch the failure
 * names the path of the first difference, as `$.party.slots[0].digimon.hp.max`.
 */
fun assertJsonEquals(expected: String, actual: String, message: String? = null) {
    val difference = firstJsonDifference(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual), "$")
    if (difference != null) {
        fail<Unit>((if (message != null) "$message: " else "") + difference)
    }
}

/** Reads a golden document written by the original C# backend (`src/test/resources/golden/<path>`). */
fun golden(path: String): String {
    val resource = "/golden/$path"
    return object {}.javaClass.getResourceAsStream(resource)?.bufferedReader()?.use { it.readText() }
        ?: error("Golden file not found: $resource")
}

/** Describes the first difference found walking [expected] and [actual] together, or null when they are equal. */
internal fun firstJsonDifference(expected: JsonElement, actual: JsonElement, path: String): String? = when {
    expected is JsonObject && actual is JsonObject -> objectDifference(expected, actual, path)
    expected is JsonArray && actual is JsonArray -> arrayDifference(expected, actual, path)
    expected is JsonNull && actual is JsonNull -> null
    expected is JsonPrimitive && actual is JsonPrimitive && expected !is JsonNull && actual !is JsonNull ->
        if (expected == actual) null else mismatch(path, expected, actual)
    else -> mismatch(path, expected, actual)
}

private fun objectDifference(expected: JsonObject, actual: JsonObject, path: String): String? {
    for ((key, expectedChild) in expected) {
        val actualChild = actual[key] ?: if (key in actual) JsonNull else return "$path.$key: expected $expectedChild but the key is absent"
        firstJsonDifference(expectedChild, actualChild, "$path.$key")?.let { return it }
    }
    val extra = actual.keys - expected.keys
    return if (extra.isEmpty()) null else "$path: unexpected keys ${extra.sorted()} (value of ${extra.first()}: ${actual[extra.first()]})"
}

private fun arrayDifference(expected: JsonArray, actual: JsonArray, path: String): String? {
    for (index in 0 until minOf(expected.size, actual.size)) {
        firstJsonDifference(expected[index], actual[index], "$path[$index]")?.let { return it }
    }
    return if (expected.size == actual.size) null else "$path: expected ${expected.size} items but was ${actual.size}"
}

private fun mismatch(path: String, expected: JsonElement, actual: JsonElement) = "$path: expected $expected but was $actual"
