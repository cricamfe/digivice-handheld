package dev.cricamfe.digivice.core.events.dto.journals.quests

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Journals/Quests/StepDTO.cs`. */
@Serializable
data class StepDto(
    @EncodeDefault val number: Int = 0,
    val isDone: Optional<Boolean> = Optional.Empty,
    val requisites: Optional<List<RequisiteDto>> = Optional.Empty,
) : IDto
