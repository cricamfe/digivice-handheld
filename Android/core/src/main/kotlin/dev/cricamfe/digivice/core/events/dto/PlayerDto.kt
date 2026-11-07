package dev.cricamfe.digivice.core.events.dto

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/PlayerDTO.cs`. */
@Serializable
data class PlayerDto(
    val bits: Optional<Int> = Optional.Empty,
    val mapId: Optional<String> = Optional.Empty,
    val previousMapId: Optional<String> = Optional.Empty,
    val seabedRoute: Optional<UByte?> = Optional.Empty,
    val mapVariant: Optional<UByte?> = Optional.Empty,
    val positionX: Optional<Int> = Optional.Empty,
    val positionY: Optional<Int> = Optional.Empty,
    val extraChapter: Optional<Boolean> = Optional.Empty,
) : IDto
