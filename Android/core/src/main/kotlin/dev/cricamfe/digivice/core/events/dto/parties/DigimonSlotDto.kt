package dev.cricamfe.digivice.core.events.dto.parties

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/DigimonSlotDTO.cs`. */
@Serializable
data class DigimonSlotDto(
    @EncodeDefault val index: Int = 0,
    val digimonId: Optional<Int?> = Optional.Empty,
    val digimon: Optional<DigimonDto?> = Optional.Empty,
) : IDto
