package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/DigievolutionDTO.cs`. */
@Serializable
data class DigievolutionDto(
    val level: Optional<Int> = Optional.Empty,
    val dvexp: Optional<Int> = Optional.Empty,
) : IDto
