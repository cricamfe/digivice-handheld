package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.FakeQuestLoader
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class JournalLoaderTests {
    @Test
    fun `load should compose every quest category from quest loader`() {
        val mainQuest = QuestResource(id = "mainQuest")
        val sideQuests = listOf(QuestResource(id = "treeBoots"))
        val legendaryWeapons = listOf(QuestResource(id = "eternally"))
        val driAgents = listOf(QuestResource(id = "driAgentGuilmon"))
        val duelIsland = listOf(QuestResource(id = "asukaTrophy"))

        val journalResource = JournalLoader(
            FakeQuestLoader(mainQuest, sideQuests, legendaryWeapons, driAgents, duelIsland),
        ).load()

        assertSame(mainQuest, journalResource.mainQuest)
        assertSame(sideQuests, journalResource.sideQuests)
        assertSame(legendaryWeapons, journalResource.legendaryWeapons)
        assertSame(driAgents, journalResource.driAgents)
        assertSame(duelIsland, journalResource.duelIsland)
    }
}
