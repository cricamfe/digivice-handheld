package dev.cricamfe.digivice.core.domain.models

import dev.cricamfe.digivice.core.domain.models.journals.Quest

/** Port of `Backend/Domain/Models/Journal.cs` (list equality is structural, see [Npc]). */
data class Journal(
    var mainQuest: Quest = Quest(),
    var sideQuests: List<Quest> = emptyList(),
    var legendaryWeapons: List<Quest> = emptyList(),
    var driAgents: List<Quest> = emptyList(),
    var duelIsland: List<Quest> = emptyList(),
)
