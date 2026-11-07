package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IJournalLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IQuestLoader
import dev.cricamfe.digivice.core.memory.resources.JournalResource

/** Port of `Backend/Application/Loaders/JournalLoader.cs`. */
class JournalLoader(private val questLoader: IQuestLoader) : IJournalLoader {
    override fun load(): JournalResource = JournalResource(
        mainQuest = questLoader.loadMainQuest(),
        sideQuests = questLoader.loadSideQuests(),
        legendaryWeapons = questLoader.loadLegendaryWeapons(),
        driAgents = questLoader.loadDriAgents(),
        duelIsland = questLoader.loadDuelIsland(),
    )
}
