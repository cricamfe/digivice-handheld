package dev.cricamfe.digivice.core.events.dto.battles

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Battles/EnemyDTO.cs`. */
@Serializable
data class EnemyDto(
    val id: Optional<Int> = Optional.Empty,
    val groupId: Optional<Int> = Optional.Empty,
    val condition: Optional<Int> = Optional.Empty,
    val strength: Optional<Int> = Optional.Empty,
    val defense: Optional<Int> = Optional.Empty,
    val speed: Optional<Int> = Optional.Empty,
    val hp: Optional<VitalDto> = Optional.Empty,
) : IDto
