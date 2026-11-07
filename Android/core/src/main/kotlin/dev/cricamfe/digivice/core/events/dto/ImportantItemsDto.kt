package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/ImportantItemsDTO.cs`. */
@Serializable
data class ImportantItemsDto(
    val treeBoots: Optional<Boolean> = Optional.Empty,
    val fishingPole: Optional<Boolean> = Optional.Empty,
    val asukaTrophy: Optional<Boolean> = Optional.Empty,
    val sunTrophy: Optional<Boolean> = Optional.Empty,
) : IDto
