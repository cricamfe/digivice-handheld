package dev.cricamfe.digivice.core.memory.resources.journals

import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource

/** Port of `Backend/Memory/Resources/Journals/QuestResource.cs`. */
data class QuestResource(
    val id: String = "",
    val requisites: List<RequisiteResource> = emptyList(),
    val steps: List<StepResource> = emptyList(),
)
