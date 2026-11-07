package dev.cricamfe.digivice.core.bridge

import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.EventSerializer
import kotlinx.serialization.json.JsonPrimitive

/**
 * The JavaScript that hands an event to the page: `window.__digivice.receive(eventType, payloadJson)`, where
 * `payloadJson` is the payload alone (the page parses it itself and takes the type from the first argument).
 * Both arguments are written as JavaScript string literals, so nothing in a payload can end the call early.
 */
object BridgeScript {
    fun receive(event: Event): String = receive(EventSerializer.typeName(event), EventSerializer.payloadJson(event))

    fun receive(eventType: String, payloadJson: String): String =
        "window.__digivice&&window.__digivice.receive(${stringLiteral(eventType)},${stringLiteral(payloadJson)})"

    /**
     * [value] as a double-quoted JavaScript string literal. JSON escapes the quote, the backslash and the control
     * characters; U+2028 and U+2029 pass through JSON unescaped but were line terminators in JavaScript string
     * literals before ES2019, so they are escaped too.
     */
    fun stringLiteral(value: String): String =
        JsonPrimitive(value).toString().replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
}
