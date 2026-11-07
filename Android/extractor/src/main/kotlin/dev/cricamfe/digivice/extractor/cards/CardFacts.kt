package dev.cricamfe.digivice.extractor.cards

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private val colors = listOf("white", "blue", "green", "red", "black", "brown")

/** The catalogue uses the card ID as its art key. Names are extracted separately from the language tables. */
internal fun buildCardFacts(cards: CardImages): String = buildJsonObject {
    for (id in cards.ids) {
        val header = cards.header(id)
        val digimon = header.kind == 16
        putJsonObject(id.toString()) {
            put("type", if (digimon) colors[header.color - 1] else "action")
            if (digimon) putJsonObject("points") {
                put("ap", header.ap)
                put("hp", header.hp)
            }
        }
    }
}.toString()
