package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.battles.EnemyDto
import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/DigimonBattleDTO.cs`. */
@Serializable
data class DigimonBattleDto(
    val field: Optional<UByte> = Optional.Empty,
    val fieldAmount: Optional<Int> = Optional.Empty,
    val activeFighter: Optional<Int> = Optional.Empty,
    val stage: Optional<Int> = Optional.Empty,
    val enemy: Optional<EnemyDto?> = Optional.Empty,
) : IDto
