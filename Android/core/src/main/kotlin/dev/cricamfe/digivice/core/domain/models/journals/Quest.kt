package dev.cricamfe.digivice.core.domain.models.journals

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step

/** Port of `Backend/Domain/Models/Journals/Quest.cs` (list equality is structural, see [dev.cricamfe.digivice.core.domain.models.Npc]). */
data class Quest(
    var id: String = "",
    var requisites: List<Requisite> = emptyList(),
    var steps: List<Step> = emptyList(),
)
