package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeJournalLoader
import dev.cricamfe.digivice.core.memory.resources.JournalResource
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JournalProviderTests {
    @Test
    fun `get should load resource and apply journal assembler`() {
        val journalResource = JournalResource(
            mainQuest = QuestResource(
                id = "mainQuest",
                steps = listOf(StepResource(number = 1, value = 0u), StepResource(number = 2, value = 1u)),
            ),
            sideQuests = listOf(QuestResource(id = "treeBoots")),
        )
        val loader = FakeJournalLoader(journalResource)

        val result = JournalProvider(loader).get()

        assertEquals("mainQuest", result.mainQuest.id)
        assertTrue(result.mainQuest.steps.all { it.isDone })
        assertEquals("treeBoots", result.sideQuests.single().id)
        assertEquals(1, loader.calls)
    }
}
