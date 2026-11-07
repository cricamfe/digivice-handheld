package dev.cricamfe.digivice.core.bridge

import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.events.Event
import dev.cricamfe.digivice.core.events.EventSerializer
import dev.cricamfe.digivice.core.events.coreJson
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import dev.cricamfe.digivice.core.events.factory.StateEventFactory
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import dev.cricamfe.digivice.core.memory.TestEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BridgeScriptTest {
    private val parser = Json

    @Test
    fun `the payload goes out alone, without the envelope`() {
        val event = Event(EventType.HealthChanged, HealthDto(HealthStatus.Error, "retroarch_unreachable", null))

        assertEquals(EventSerializer.typeName(event), "HealthChanged")
        assertEquals(
            """{"status":"Error","errorCode":"retroarch_unreachable","errorDetail":null}""",
            EventSerializer.payloadJson(event),
        )
    }

    @Test
    fun `type and payload are exactly the two fields of the envelope`() {
        val events = listOf(
            Event(EventType.PlayerChanged, PlayerDto(bits = Optional.Present(200))),
            Event(EventType.HealthChanged, HealthDto(HealthStatus.Healthy)),
        ) + realEvents()

        assertTrue(events.size > 2, "the real events are part of the check")
        for (event in events) {
            val envelope = parser.parseToJsonElement(coreJson.encodeToString(EventSerializer, event)).jsonObject
            assertEquals(setOf("type", "payload"), envelope.keys)
            assertEquals(envelope["type"]!!.jsonPrimitive.content, EventSerializer.typeName(event))
            assertEquals(envelope["payload"], parser.parseToJsonElement(EventSerializer.payloadJson(event)))
        }
    }

    /** The whole first state of a real RAM snapshot, and what changes to the next one: big payloads, many types. */
    private fun realEvents(): List<Event> {
        val names = TestEnvironment.snapshotNames()
        val states = names.take(3).map {
            CoreModule(
                TestEnvironment.snapshot(it),
                TestEnvironment.definitions::readText,
                TestEnvironment.definitions::listFiles,
            ).stateComposer.compose()
        }
        return StateEventFactory.create(null, states[0]) +
            states.zipWithNext().flatMap { (a, b) -> StateEventFactory.create(a, b) }
    }

    @Test
    fun `receive calls the page with the type and the payload as string literals`() {
        val script = BridgeScript.receive("HealthChanged", """{"status":"Healthy"}""")

        assertEquals(
            """window.__digivice&&window.__digivice.receive("HealthChanged","{\"status\":\"Healthy\"}")""",
            script,
        )
    }

    @Test
    fun `the literal survives every character that could break it`() {
        val nasty = "quote\" backslash\\ slash/ newline\n return\r tab\t nul\u0000 bell\u0007 " +
            "ls\u2028 ps\u2029 </script> 'single' `tick` \${x} emoji\uD83D\uDE00 accent\u00E9 \uD800"

        val literal = BridgeScript.stringLiteral(nasty)

        assertFalse('\n' in literal || '\r' in literal || '\u2028' in literal || '\u2029' in literal, "a raw terminator is left")
        assertTrue(literal.startsWith("\"") && literal.endsWith("\""))
        // JSON string syntax is a subset of JavaScript's once U+2028/2029 are escaped, so a JSON parser stands in for JS here.
        assertEquals(JsonPrimitive(nasty), parser.parseToJsonElement(literal.replace("\\u2028", "\u2028").replace("\\u2029", "\u2029")))
    }

    @Test
    fun `line and paragraph separators are written as escapes`() {
        assertEquals("\"a\\u2028b\\u2029c\"", BridgeScript.stringLiteral("a\u2028b\u2029c"))
    }

    @Test
    fun `a payload that is itself json with strings and separators stays one argument`() {
        val payload = coreJson.encodeToString(
            JsonObject.serializer(),
            JsonObject(mapOf("name" to JsonPrimitive("a\"b\u2028c\\d"))),
        )

        val script = BridgeScript.receive("PlayerChanged", payload)

        val prefix = "window.__digivice&&window.__digivice.receive(\"PlayerChanged\","
        assertTrue(script.startsWith(prefix))
        assertTrue(script.endsWith(")"))
        val argument = script.removePrefix(prefix).removeSuffix(")")
        val decoded = parser.parseToJsonElement(argument.replace("\\u2028", "\u2028")).jsonPrimitive.content
        assertEquals(payload, decoded)
    }
}
