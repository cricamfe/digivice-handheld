package dev.cricamfe.digivice.core.events.dto.parties.digimons

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/Digimons/DigievolutionSlotDTO.cs`. */
@Serializable
data class DigievolutionSlotDto(
    @EncodeDefault val index: Int = 0,
    val digievolutionId: Optional<Int?> = Optional.Empty,
    val digievolution: Optional<DigievolutionDto?> = Optional.Empty,
) : IDto
