package dev.cricamfe.digivice.core.events.dto.extensions

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto

/**
 * Port of `Backend/Events/DTO/Extensions/DTOExtensions.cs`: a DTO is empty when it equals a freshly built one, that
 * is, when none of its optional fields is present (a present `0`, `false` or `null` makes it not empty).
 *
 * The C# `new T()` becomes the argument [empty]: the caller passes the fresh DTO (`dto.isNotEmpty(PlayerDto())`), so
 * no reflection is needed and nothing depends on constructors that R8 could remove. In the event factories of the
 * Diffing port, [empty] is the empty instance of the same DTO type that the differ produced.
 */
fun <T : IDto> T.isEmpty(empty: T): Boolean = this == empty

fun <T : IDto> T.isNotEmpty(empty: T): Boolean = !isEmpty(empty)
