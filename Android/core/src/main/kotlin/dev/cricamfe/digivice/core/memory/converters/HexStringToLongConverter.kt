package dev.cricamfe.digivice.core.memory.converters

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.util.logging.Logger

/**
 * Port of `HexStringToLongConverter`: a JSON string holding a hexadecimal ("0x48") or decimal ("20", "-4") number
 * becomes a [Long]. Null, empty or unparseable values become 0 (the unparseable ones log a warning). It writes
 * "0x" followed by upper-case hex.
 *
 * Unlike the C# converter, which throws on a bare JSON number, a bare number is accepted here as if it were quoted.
 */
object HexStringToLongConverter : KSerializer<Long> {
    private val logger: Logger = Logger.getLogger(HexStringToLongConverter::class.java.name)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexStringToLong", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as? JsonDecoder ?: throw SerializationException("Only JSON is supported")).decodeJsonElement()
        if (element is JsonNull) return 0
        val text = (element as? JsonPrimitive)?.content ?: throw SerializationException("Expected a JSON string, got $element")
        if (text.isEmpty()) return 0

        return try {
            parseHexOrDecimalLong(text)
        } catch (_: NumberFormatException) {
            logger.warning { "HexStringToLongConverter failed to parse value '$text', falling back to 0" }
            0
        }
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeString("0x" + java.lang.Long.toHexString(value).uppercase())
    }
}
