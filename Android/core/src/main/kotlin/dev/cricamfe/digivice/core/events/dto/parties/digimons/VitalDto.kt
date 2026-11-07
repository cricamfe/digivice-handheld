package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/VitalDTO.cs`. */
@Serializable
data class VitalDto(
    val current: Optional<Int> = Optional.Empty,
    val max: Optional<Int> = Optional.Empty,
) : IDto
