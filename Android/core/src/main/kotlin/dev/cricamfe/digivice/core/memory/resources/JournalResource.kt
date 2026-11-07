package dev.cricamfe.digivice.core.memory.resources

import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Memory/Resources/JournalResource.cs`. */
data class JournalResource(
    val mainQuest: QuestResource = QuestResource(),
    val sideQuests: List<QuestResource> = emptyList(),
    val legendaryWeapons: List<QuestResource> = emptyList(),
    val driAgents: List<QuestResource> = emptyList(),
    val duelIsland: List<QuestResource> = emptyList(),
)
