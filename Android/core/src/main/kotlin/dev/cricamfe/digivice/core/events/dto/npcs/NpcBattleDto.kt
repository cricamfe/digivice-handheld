package dev.cricamfe.digivice.core.events.dto.npcs

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Npcs/NpcBattleDTO.cs`. */
@Serializable
data class NpcBattleDto(
    @EncodeDefault val id: String = "",
    val won: Optional<Boolean> = Optional.Empty,
) : IDto
