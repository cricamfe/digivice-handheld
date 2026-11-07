package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.journals.QuestDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/JournalDTO.cs`. */
@Serializable
data class JournalDto(
    val mainQuest: Optional<QuestDto> = Optional.Empty,
    val sideQuests: Optional<List<QuestDto>> = Optional.Empty,
    val legendaryWeapons: Optional<List<QuestDto>> = Optional.Empty,
    val driAgents: Optional<List<QuestDto>> = Optional.Empty,
    val duelIsland: Optional<List<QuestDto>> = Optional.Empty,
) : IDto
