package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.events.dto.FieldActorDto
import dev.cricamfe.digivice.core.events.dto.FieldActorsDto
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.PlayerPositionDto
import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.models.HealthStatus
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * The wire shape of an [Event] as the goldens fix it (`{ "type": <name>, "payload": <runtime DTO> }`, no timestamp),
 * and that the HealthDto payload is what `Frontend/src/events/dto/health.dto.ts` declares.
 */
class EventTests {
    private fun encode(event: Event) = coreJson.encodeToString(EventSerializer, event)

    @Test
    fun `the event type goes out as its name and the payload as its runtime dto`() {
        val event = Event(EventType.PlayerChanged, PlayerDto(bits = Optional.Present(200)))

        assertJsonEquals("""{"type":"PlayerChanged","payload":{"bits":200}}""", encode(event))
    }

    @Test
    fun `a position event carries x and y alone`() {
        val event = Event(EventType.PlayerPositionChanged, PlayerPositionDto(776, -224))

        assertJsonEquals("""{"type":"PlayerPositionChanged","payload":{"x":776,"y":-224}}""", encode(event))
    }

    @Test
    fun `the characters of the map go out as one list`() {
        val event = Event(EventType.FieldActorsChanged, FieldActorsDto(listOf(FieldActorDto(157, 1072, 665, 1), FieldActorDto(178, 1448, 236, 7))))

        assertJsonEquals(
            """{"type":"FieldActorsChanged","payload":{"actors":[{"key1":157,"x":1072,"y":665,"dir":1},{"key1":178,"x":1448,"y":236,"dir":7}]}}""",
            encode(event),
        )
        assertJsonEquals("""{"type":"FieldActorsChanged","payload":{"actors":[]}}""", encode(Event(EventType.FieldActorsChanged, FieldActorsDto(emptyList()))))
    }

    @Test
    fun `a health payload has the three fields of the frontend dto, the errors as null`() {
        val event = Event(EventType.HealthChanged, HealthDto(HealthStatus.Healthy))

        assertJsonEquals(
            """{"type":"HealthChanged","payload":{"status":"Healthy","errorCode":null,"errorDetail":null}}""",
            encode(event),
        )
    }

    @Test
    fun `a health error payload carries the code and the detail`() {
        val event = Event(EventType.HealthChanged, HealthDto(HealthStatus.Error, "memory_read_failed", "Failed to read"))

        assertJsonEquals(
            """{"type":"HealthChanged","payload":{"status":"Error","errorCode":"memory_read_failed","errorDetail":"Failed to read"}}""",
            encode(event),
        )
    }

    @Test
    fun `a list of events is an array of type and payload objects in order`() {
        val events = listOf(
            Event(EventType.PlayerChanged, PlayerDto(bits = Optional.Present(1))),
            Event(EventType.HealthChanged, HealthDto(HealthStatus.Loading)),
        )

        val json = coreJson.encodeToString(ListSerializer(EventSerializer), events)

        assertJsonEquals(
            """[{"type":"PlayerChanged","payload":{"bits":1}},""" +
                """{"type":"HealthChanged","payload":{"status":"Loading","errorCode":null,"errorDetail":null}}]""",
            json,
        )
    }

    @Test
    fun `the event serializer is the default serializer of Event`() {
        val event = Event(EventType.PlayerChanged, PlayerDto(bits = Optional.Present(5)))

        assertEquals(encode(event), coreJson.encodeToString(Event.serializer(), event))
    }

    @Test
    fun `a payload that is not an event payload dto is refused when serialized`() {
        assertThrows(SerializationException::class.java) { encode(Event(EventType.PlayerChanged, VitalDto())) }
        assertThrows(SerializationException::class.java) { encode(Event(EventType.PlayerChanged, object : IDto {})) }
    }
}
