package dev.cricamfe.digivice.core.events.dto.parties

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.AttributesDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.DigievolutionSlotDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.ElementsDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.EquipmentsDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.InBattleDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.StoredDigievolutionDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Parties/DigimonDTO.cs`. */
@Serializable
data class DigimonDto(
    val level: Optional<Int> = Optional.Empty,
    val tp: Optional<Int> = Optional.Empty,
    val blast: Optional<Int> = Optional.Empty,
    val experience: Optional<Int> = Optional.Empty,
    val hp: Optional<VitalDto> = Optional.Empty,
    val mp: Optional<VitalDto> = Optional.Empty,
    val inBattle: Optional<InBattleDto> = Optional.Empty,
    val attributes: Optional<AttributesDto> = Optional.Empty,
    val elements: Optional<ElementsDto> = Optional.Empty,
    val equipments: Optional<EquipmentsDto> = Optional.Empty,
    val digievolutions: Optional<List<DigievolutionSlotDto>> = Optional.Empty,
    val activeDigievolutionId: Optional<Int?> = Optional.Empty,
    val storedDigievolutions: Optional<List<StoredDigievolutionDto>> = Optional.Empty,
) : IDto
