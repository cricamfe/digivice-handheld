package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/AttributesDTO.cs`. */
@Serializable
data class AttributesDto(
    val strength: Optional<Int> = Optional.Empty,
    val defense: Optional<Int> = Optional.Empty,
    val spirit: Optional<Int> = Optional.Empty,
    val wisdom: Optional<Int> = Optional.Empty,
    val speed: Optional<Int> = Optional.Empty,
    val charisma: Optional<Int> = Optional.Empty,
) : IDto
