package dev.cricamfe.digivice.extractor.text

import dev.cricamfe.digivice.extractor.disc.invalidDisc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

@Serializable
private data class TextKeys(val messages: List<MessageKey>, val words: List<WordKey>)

@Serializable
private data class MessageKey(val path: List<String>, val table: String, val line: Int)

/** Only speaker and the two htlnam:17 pieces need selectors; all other keys address a full line. */
@Serializable
private data class WordKey(val key: String, val table: String, val line: Int, val selector: String? = null)

private val textKeys by lazy {
    val resource = GameText::class.java.getResourceAsStream("/extractor/text-keys.json")
        ?: error("Missing extractor text-key facts")
    resource.bufferedReader().use { Json.decodeFromString<TextKeys>(it.readText()) }
}

// Android's regex engine does not accept Java's (?U) flag.
private val whitespace = Regex("[\\u0009-\\u000D\\u0085\\p{Z}]+")
private val speakerName = Regex("^\\[name\\](.*?)\\[name\\]")

fun buildTexts(text: GameText): Map<String, String> = GameLanguage.entries.associate { language ->
    // Tables are shared within one locale only; do not retain all five languages in memory.
    val tables = mutableMapOf<String, List<String>>()
    fun line(table: String, index: Int): String =
        tables.getOrPut(table) { text.table(language, table) }.getOrNull(index)
            ?: invalidDisc("Missing referenced text line: $table:$index")
    val messages = mutableMapOf<String, JsonElement>()
    for (key in textKeys.messages) put(messages, key.path, JsonPrimitive(flat(line(key.table, key.line))))
    val words = textKeys.words.associate { key ->
        val value = line(key.table, key.line)
        val selected = when (key.selector) {
            null -> flat(value)
            "part:0" -> flat(value.split('\n').getOrNull(0) ?: invalidDisc("Missing first text part"))
            "part:1" -> flat(value.split('\n').getOrNull(1) ?: invalidDisc("Missing second text part"))
            "speaker" -> {
                val speaker = speakerName.find(value)?.groupValues?.get(1)
                    ?: invalidDisc("Missing speaker in referenced text line")
                val name = flat(speaker).trimEnd('.')
                name.take(1).uppercase(Locale.ROOT) + name.drop(1)
            }
            else -> error("Unsupported text selector")
        }
        key.key to JsonPrimitive(selected)
    }
    language.locale to JsonObject(mapOf("messages" to JsonObject(messages), "words" to JsonObject(words))).toString()
}

internal fun flat(value: String): String =
    value.replace("\\n", " ").replace(whitespace, " ").trim().replace('β', 'ß')

private fun put(tree: MutableMap<String, JsonElement>, path: List<String>, value: JsonElement) {
    check(path.isNotEmpty())
    if (path.size == 1) tree[path[0]] = value
    else {
        val child = (tree[path[0]] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        put(child, path.drop(1), value)
        tree[path[0]] = JsonObject(child)
    }
}
