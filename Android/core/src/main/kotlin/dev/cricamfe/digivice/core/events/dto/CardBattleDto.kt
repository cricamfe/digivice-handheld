package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/CardBattleDTO.cs`. */
@Serializable
data class CardBattleDto(
    val id: Optional<Int?> = Optional.Empty,
) : IDto
