package dev.cricamfe.digivice.core.events.dto.journals.quests

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Journals/Quests/RequisiteDTO.cs`. */
@Serializable
data class RequisiteDto(
    @EncodeDefault val id: String = "",
    val isDone: Optional<Boolean> = Optional.Empty,
) : IDto
