package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/**
 * The payload of `FieldActorsChanged`: the characters the field draws besides the party, as the whole list each time
 * (empty out of the field). It is not part of the state's diff; the loop sends it with the full cycle, only when it
 * changed. Not in the C# backend.
 */
@Serializable
data class FieldActorsDto(val actors: List<FieldActorDto>) : IDto

/** A character of the map: [key1] says who, [x] and [y] are pixels of the map picture, [dir] is 0 to 7. */
@Serializable
data class FieldActorDto(val key1: Int, val x: Int, val y: Int, val dir: Int)
