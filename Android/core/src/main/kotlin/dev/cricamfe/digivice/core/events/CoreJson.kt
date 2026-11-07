package dev.cricamfe.digivice.core.events

import kotlinx.serialization.json.Json

/**
 * The JSON settings of the wire format that the original C# backend sent, shared by everything in the core that
 * writes or reads DTOs.
 *
 * - Property names are the Kotlin property names, which are already the camelCase that the C# web defaults produced
 *   (`hp`, `tp`, `mapId`); enums go out as their name (`JsonStringEnumConverter`); unsigned types as plain numbers.
 * - `null` is written (`explicitNulls`, as `DefaultIgnoreCondition.Never` did).
 * - Default values are not written: the default of every `Optional` field is the absent state, so this is what leaves
 *   absent fields out, and the few plain fields that C# always wrote (`index`, `id`, ...) are `@EncodeDefault`.
 * - Never pretty-printed: it is a wire format.
 */
val coreJson: Json = Json {
    encodeDefaults = false
    explicitNulls = true
    prettyPrint = false
}
