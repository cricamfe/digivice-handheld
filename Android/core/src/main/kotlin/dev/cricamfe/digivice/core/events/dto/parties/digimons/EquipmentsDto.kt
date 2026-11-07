package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/EquipmentsDTO.cs`. */
@Serializable
data class EquipmentsDto(
    val head: Optional<Int?> = Optional.Empty,
    val body: Optional<Int?> = Optional.Empty,
    val right: Optional<Int?> = Optional.Empty,
    val left: Optional<Int?> = Optional.Empty,
    val accessory1: Optional<Int?> = Optional.Empty,
    val accessory2: Optional<Int?> = Optional.Empty,
) : IDto
