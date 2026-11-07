package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/AuctionsDTO.cs`. */
@Serializable
data class AuctionsDto(
    val divineBarrier: Optional<Boolean> = Optional.Empty,
    val hazardShield: Optional<Boolean> = Optional.Empty,
    val sniperShield: Optional<Boolean> = Optional.Empty,
    val dramonShield: Optional<Boolean> = Optional.Empty,
    val yinYangWand: Optional<Boolean> = Optional.Empty,
    val hazardFang: Optional<Boolean> = Optional.Empty,
) : IDto
