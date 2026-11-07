package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.events.dto.AuctionsDto
import dev.cricamfe.digivice.core.events.dto.CardBattleDto
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.dto.FieldActorsDto
import dev.cricamfe.digivice.core.events.dto.FieldChangingDto
import dev.cricamfe.digivice.core.events.dto.QuestOffersDto
import dev.cricamfe.digivice.core.events.dto.PartnerTechniquesDto
import dev.cricamfe.digivice.core.events.dto.HealthDto
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.dto.PartyDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.PlayerPositionDto
import dev.cricamfe.digivice.core.events.dto.StateDto
import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.models.EventType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject

/**
 * Port of `Backend/Events/Models/Event.cs`: what changed ([type]) and the DTO that says it ([payload]).
 *
 * The C# `Timestamp` is not ported: nothing in the app reads it, and the goldens that fix the wire shape (a JSON object
 * `{ "type": <name>, "payload": <dto> }`) have none.
 */
@Serializable(with = EventSerializer::class)
data class Event(val type: EventType, val payload: IDto)

/**
 * Port of `DtoRuntimeTypeJsonConverter`: the payload is written as the DTO it really is (`PlayerDto`, `HealthDto`, ...),
 * with the settings of [coreJson] whatever the encoder's own are, so the fields a diff leaves out stay out.
 *
 * `IDto` is an open interface (every nested DTO has it too), so the types that travel as an event payload are listed
 * here instead of being looked up by reflection, which `:core` deliberately does not use. Only these twelve are payloads of
 * [EventType]; any other DTO is refused. Decoding is not supported, as in the backend: the app only emits events.
 */
object EventSerializer : KSerializer<Event> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Event") {
        element<EventType>("type")
        element("payload", JsonElement.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Event) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("An Event can only be written as JSON")
        jsonEncoder.encodeJsonElement(
            JsonObject(
                mapOf(
                    "type" to coreJson.encodeToJsonElement(EventType.serializer(), value.type),
                    "payload" to payloadElement(value),
                ),
            ),
        )
    }

    /** The `type` field of the envelope: the name of the event, without quotes. */
    fun typeName(event: Event): String = event.type.name

    /** The `payload` field of the envelope on its own, which is what a client that already has the type needs. */
    fun payloadJson(event: Event): String = payloadElement(event).toString()

    private fun payloadElement(event: Event): JsonElement =
        coreJson.encodeToJsonElement(payloadSerializer(event.payload), event.payload)

    override fun deserialize(decoder: Decoder): Event =
        throw SerializationException("Event payload deserialization is not supported; the app only emits events.")

    @Suppress("UNCHECKED_CAST")
    private fun payloadSerializer(payload: IDto): KSerializer<IDto> = when (payload) {
        is StateDto -> StateDto.serializer()
        is HealthDto -> HealthDto.serializer()
        is PlayerDto -> PlayerDto.serializer()
        is ImportantItemsDto -> ImportantItemsDto.serializer()
        is PartyDto -> PartyDto.serializer()
        is DigimonBattleDto -> DigimonBattleDto.serializer()
        is CardBattleDto -> CardBattleDto.serializer()
        is AuctionsDto -> AuctionsDto.serializer()
        is NpcsDto -> NpcsDto.serializer()
        is JournalDto -> JournalDto.serializer()
        is PlayerPositionDto -> PlayerPositionDto.serializer()
        is FieldActorsDto -> FieldActorsDto.serializer()
        is FieldChangingDto -> FieldChangingDto.serializer()
        is PartnerTechniquesDto -> PartnerTechniquesDto.serializer()
        is QuestOffersDto -> QuestOffersDto.serializer()
        else -> throw SerializationException("${payload::class.simpleName} is not an event payload")
    } as KSerializer<IDto>
}
