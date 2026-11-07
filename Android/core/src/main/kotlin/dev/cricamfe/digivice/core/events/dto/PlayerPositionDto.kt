package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/**
 * The payload of `PlayerPositionChanged`: where the player stands, in pixels of the map picture. It is not part of the
 * state's diff; the loop sends it between full cycles, only when the position changed. Not in the C# backend.
 */
@Serializable
data class PlayerPositionDto(val x: Int, val y: Int) : IDto
