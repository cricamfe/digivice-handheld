package dev.cricamfe.digivice.core.events.dto.shared

import dev.cricamfe.digivice.core.events.converters.OptionalSerializer
import kotlinx.serialization.Serializable

/**
 * Port of `Backend/Events/DTO/Shared/Optional.cs`: a DTO field that may be absent, as opposed to present with a value
 * that is itself `null`. The wire format needs the three states apart:
 *
 * - [Empty]: the field is left out of the JSON (nothing changed, in a diff).
 * - `Present(null)`: the field is written as `null` (it changed to "nothing", or it is a nullable field of a snapshot).
 * - `Present(value)`: the field is written as the value, `0` and `false` included.
 *
 * How [Empty] is left out is the job of `coreJson` (it does not encode default values, and [Empty] is the default of
 * every optional field), see [OptionalSerializer].
 */
@Serializable(with = OptionalSerializer::class)
sealed interface Optional<out T> {
    val hasValue: Boolean

    /** The value; reading it on [Empty] throws, where the C# struct returned `default`. */
    val value: T

    data object Empty : Optional<Nothing> {
        override val hasValue: Boolean get() = false
        override val value: Nothing get() = throw NoSuchElementException("The optional has no value")
    }

    data class Present<out T>(override val value: T) : Optional<T> {
        override val hasValue: Boolean get() = true
    }
}

/** The value, or null when the optional is [Optional.Empty] (also null for a present `null`: use [Optional.hasValue]). */
fun <T> Optional<T>.valueOrNull(): T? = if (this is Optional.Present) value else null
