package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.memory.resources.JournalResource
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JournalAssemblerTests {
    @Test
    fun `assemble should apply completion cascade to main quest`() {
        val resource = JournalResource(mainQuest = createQuestWithOnlyLastStepDone("mainQuest"))

        val result = JournalAssembler.assemble(resource)

        assertTrue(result.mainQuest.steps.all { it.isDone })
    }

    @Test
    fun `assemble should not apply completion cascade to side quests`() {
        val resource = JournalResource(sideQuests = listOf(createQuestWithOnlyLastStepDone("treeBoots")))

        val result = JournalAssembler.assemble(resource)

        assertOnlyLastStepDone(result.sideQuests.single())
    }

    @Test
    fun `assemble should not apply completion cascade to legendary weapons`() {
        val resource = JournalResource(legendaryWeapons = listOf(createQuestWithOnlyLastStepDone("eternally")))

        val result = JournalAssembler.assemble(resource)

        assertOnlyLastStepDone(result.legendaryWeapons.single())
    }

    @Test
    fun `assemble should not apply completion cascade to dri agents`() {
        val resource = JournalResource(driAgents = listOf(createQuestWithOnlyLastStepDone("driAgentGuilmon")))

        val result = JournalAssembler.assemble(resource)

        assertOnlyLastStepDone(result.driAgents.single())
    }

    @Test
    fun `assemble should apply duel island rules to duel island`() {
        val suppressedQuest = QuestResource(
            id = "asukaRematch",
            requisites = listOf(RequisiteResource(id = "asukaTrophy", value = 0u)),
            steps = listOf(
                StepResource(number = 1, value = 0x80u, requisites = emptyList()),
                StepResource(number = 2, value = 0u, requisites = emptyList()),
            ),
        )
        val resource = JournalResource(
            duelIsland = listOf(createQuestWithOnlyLastStepDone("asukaTrophy"), suppressedQuest),
        )

        val result = JournalAssembler.assemble(resource)

        assertEquals(2, result.duelIsland.size)
        assertTrue(result.duelIsland[0].steps.all { it.isDone })
        assertTrue(result.duelIsland[1].steps.none { it.isDone })
    }

    private fun createQuestWithOnlyLastStepDone(id: String) = QuestResource(
        id = id,
        requisites = emptyList(),
        steps = listOf(
            StepResource(number = 1, value = 0u, requisites = emptyList()),
            StepResource(number = 2, value = 0u, requisites = emptyList()),
            StepResource(number = 3, value = 1u, requisites = emptyList()),
        ),
    )

    private fun assertOnlyLastStepDone(quest: Quest) {
        assertFalse(quest.steps[0].isDone)
        assertFalse(quest.steps[1].isDone)
        assertTrue(quest.steps[2].isDone)
    }
}
