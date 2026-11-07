package dev.cricamfe.digivice.core.memory.converters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.logging.Logger

/**
 * Port of `HexStringListToLongListConverter`: a JSON array of hexadecimal ("0x08") or decimal strings becomes a
 * `List<Long>` (bit masks). Empty entries are skipped, unparseable ones become 0 with a warning, and a value that is
 * not an array becomes an empty list. It writes the masks back as "0x" followed by upper-case hex.
 */
object HexStringListToLongListConverter : KSerializer<List<Long>> {
    private val logger: Logger = Logger.getLogger(HexStringListToLongListConverter::class.java.name)
    private val stringListSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = stringListSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<Long> {
        val element = (decoder as? JsonDecoder ?: throw SerializationException("Only JSON is supported")).decodeJsonElement()
        val bitMasks = mutableListOf<Long>()
        if (element !is JsonArray) return bitMasks

        for (item in element) {
            if (item is JsonNull) continue
            val hexString = (item as? JsonPrimitive)?.content ?: throw SerializationException("Expected a JSON string, got $item")
            if (hexString.isEmpty()) continue

            try {
                bitMasks.add(parseHexOrDecimalLong(hexString))
            } catch (_: NumberFormatException) {
                logger.warning { "HexStringListToLongListConverter failed to parse value '$hexString', falling back to 0" }
                bitMasks.add(0)
            }
        }

        return bitMasks
    }

    override fun serialize(encoder: Encoder, value: List<Long>) {
        encoder.encodeSerializableValue(
            stringListSerializer,
            value.map { "0x" + java.lang.Long.toHexString(it).uppercase() },
        )
    }
}
