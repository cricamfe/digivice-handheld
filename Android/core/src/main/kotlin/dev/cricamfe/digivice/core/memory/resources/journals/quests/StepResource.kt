package dev.cricamfe.digivice.core.memory.resources.journals.quests

/** Port of `Backend/Memory/Resources/Journals/Quests/StepResource.cs`. */
data class StepResource(
    val number: Int = 0,
    val value: UByte = 0u,
    val requisites: List<RequisiteResource> = emptyList(),
)
