package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Port of `OptionalJsonConverter<T>` (and of its factory, which Kotlin does not need: the compiler plugin builds this
 * serializer for every `Optional<T>` property, giving it the serializer of `T`).
 *
 * A present value is written as the value itself, with no wrapper. An empty optional cannot be written: it has to be
 * left out of its object, which the property cannot do by itself. `coreJson` takes care of it by not encoding default
 * values (the default of every optional field is [Optional.Empty]); in C# the `WhenWritingDefault` attribute did it.
 * Reaching this serializer with an empty value means some other `Json` instance encoded the DTOs, so it fails loudly
 * instead of writing a half object.
 */
class OptionalSerializer<T>(private val valueSerializer: KSerializer<T>) : KSerializer<Optional<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Optional<T>) {
        when (value) {
            is Optional.Present -> encoder.encodeSerializableValue(valueSerializer, value.value)
            Optional.Empty -> throw SerializationException(
                "An empty Optional has no JSON form; it is left out of its object. Encode with coreJson " +
                    "(encodeDefaults = false), not with a Json that encodes default values.",
            )
        }
    }

    /** A JSON `null` is a present null (the target type decides whether `null` is allowed), never an empty optional. */
    override fun deserialize(decoder: Decoder): Optional<T> =
        Optional.Present(decoder.decodeSerializableValue(valueSerializer))
}
