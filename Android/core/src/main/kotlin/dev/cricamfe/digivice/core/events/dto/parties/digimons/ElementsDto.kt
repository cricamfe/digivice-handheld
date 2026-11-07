package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/ElementsDTO.cs`. */
@Serializable
data class ElementsDto(
    val fire: Optional<Int> = Optional.Empty,
    val water: Optional<Int> = Optional.Empty,
    val ice: Optional<Int> = Optional.Empty,
    val wind: Optional<Int> = Optional.Empty,
    val thunder: Optional<Int> = Optional.Empty,
    val machine: Optional<Int> = Optional.Empty,
    val dark: Optional<Int> = Optional.Empty,
) : IDto
