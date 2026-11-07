package dev.cricamfe.digivice.core.events.dto.journals

import dev.cricamfe.digivice.core.events.dto.interfaces.IDto
import dev.cricamfe.digivice.core.events.dto.journals.quests.RequisiteDto
import dev.cricamfe.digivice.core.events.dto.journals.quests.StepDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/** Port of `Backend/Events/DTO/Journals/QuestDTO.cs`. */
@Serializable
data class QuestDto(
    @EncodeDefault val id: String = "",
    val requisites: Optional<List<RequisiteDto>> = Optional.Empty,
    val steps: Optional<List<StepDto>> = Optional.Empty,
) : IDto
