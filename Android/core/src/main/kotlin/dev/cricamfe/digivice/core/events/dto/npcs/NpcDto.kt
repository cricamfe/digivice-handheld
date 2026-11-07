package dev.cricamfe.digivice.core.events.dto.npcs

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Npcs/NpcDTO.cs`. */
@Serializable
data class NpcDto(
    val battles: Optional<List<NpcBattleDto>> = Optional.Empty,
) : IDto
