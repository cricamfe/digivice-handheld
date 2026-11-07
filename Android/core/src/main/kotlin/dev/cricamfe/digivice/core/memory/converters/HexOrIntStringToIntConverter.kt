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
 * Port of `HexOrIntStringToIntConverter`: converts JSON values that may be a number (20), a hexadecimal string
 * ("0x48"), a decimal string ("20") or a negative one ("-4") into an [Int]. Used for the Digimon memory offsets,
 * which mix those formats. Null, empty or unparseable strings become 0 (the unparseable ones log a warning).
 * It writes a plain JSON number.
 */
object HexOrIntStringToIntConverter : KSerializer<Int> {
    private val logger: Logger = Logger.getLogger(HexOrIntStringToIntConverter::class.java.name)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexOrIntStringToInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val element = (decoder as? JsonDecoder ?: throw SerializationException("Only JSON is supported")).decodeJsonElement()
        if (element is JsonNull) return 0
        val primitive = element as? JsonPrimitive ?: throw SerializationException("Expected a JSON number or string, got $element")
        if (!primitive.isString) return primitive.content.toInt()

        val text = primitive.content
        if (text.isEmpty()) return 0

        return try {
            parseHexOrDecimalInt(text)
        } catch (_: NumberFormatException) {
            logger.warning { "HexOrIntStringToIntConverter failed to parse value '$text', falling back to 0" }
            0
        }
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }
}
