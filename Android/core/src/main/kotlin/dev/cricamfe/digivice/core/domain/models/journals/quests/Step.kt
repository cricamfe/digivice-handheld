package dev.cricamfe.digivice.core.domain.models.journals.quests

/** Port of `Backend/Domain/Models/Journals/Quests/Step.cs`. */
data class Step(
    var number: Int = 0,
    var isDone: Boolean = false,
    var requisites: List<Requisite> = emptyList(),
)
