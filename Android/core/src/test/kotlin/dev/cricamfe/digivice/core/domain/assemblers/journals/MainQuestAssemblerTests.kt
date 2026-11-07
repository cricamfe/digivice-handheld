package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainQuestAssemblerTests {
    private fun createMainQuest(vararg stepValues: Int) = QuestResource(
        id = "mainQuest",
        requisites = emptyList(),
        steps = stepValues.mapIndexed { index, value ->
            StepResource(number = index + 1, value = value.toUByte(), requisites = emptyList())
        },
    )

    @Test
    fun `assemble should apply completion cascade`() {
        val result = MainQuestAssembler.assemble(createMainQuest(0, 0, 1, 0))

        assertTrue(result.steps[0].isDone)
        assertTrue(result.steps[1].isDone)
        assertTrue(result.steps[2].isDone)
        assertFalse(result.steps[3].isDone)
    }

    @Test
    fun `assemble should cascade from last step when only last step is done`() {
        val result = MainQuestAssembler.assemble(createMainQuest(0, 0, 0, 0x80))

        assertTrue(result.steps.all { it.isDone })
    }

    @Test
    fun `assemble should keep all steps not done when no step is done`() {
        val result = MainQuestAssembler.assemble(createMainQuest(0, 0, 0))

        assertTrue(result.steps.none { it.isDone })
    }

    @Test
    fun `assemble should keep all steps done when every step is done`() {
        val result = MainQuestAssembler.assemble(createMainQuest(1, 1, 1))

        assertTrue(result.steps.all { it.isDone })
    }

    @Test
    fun `assemble should handle empty steps`() {
        val result = MainQuestAssembler.assemble(createMainQuest())

        assertEquals("mainQuest", result.id)
        assertTrue(result.steps.isEmpty())
    }
}
