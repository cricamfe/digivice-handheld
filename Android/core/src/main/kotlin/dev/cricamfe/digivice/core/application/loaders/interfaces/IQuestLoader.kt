package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Application/Loaders/Interfaces/IQuestLoader.cs`. */
interface IQuestLoader {
    fun loadMainQuest(): QuestResource
    fun loadSideQuests(): List<QuestResource>
    fun loadLegendaryWeapons(): List<QuestResource>
    fun loadDriAgents(): List<QuestResource>
    fun loadDuelIsland(): List<QuestResource>
}
