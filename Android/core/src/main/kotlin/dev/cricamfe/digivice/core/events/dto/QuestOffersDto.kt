package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import kotlinx.serialization.Serializable

/**
 * The payload of `QuestOffersChanged`: the quests the game would start now, by its own conditions. It is not part of the
 * state's diff; the loop sends it with the full cycle, only when it changed. Not in the C# backend.
 */
@Serializable
data class QuestOffersDto(val quests: List<String>) : IDto
