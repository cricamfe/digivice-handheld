package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.parties.DigimonSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/PartyDTO.cs`. */
@Serializable
data class PartyDto(
    val slots: Optional<List<DigimonSlotDto>> = Optional.Empty,
) : IDto
