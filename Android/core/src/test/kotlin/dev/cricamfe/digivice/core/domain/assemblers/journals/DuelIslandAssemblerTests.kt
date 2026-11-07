package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuelIslandAssemblerTests {
    private fun steps(vararg values: Int) = values.mapIndexed { index, value ->
        StepResource(number = index + 1, value = value.toUByte(), requisites = emptyList())
    }

    @Test
    fun `assemble should normalize when trophy is obtained`() {
        val resource = QuestResource(id = "asukaTrophy", requisites = emptyList(), steps = steps(0, 0, 0, 0, 0, 1))

        val result = DuelIslandAssembler.assemble(resource)

        assertTrue(result.steps[0].isDone)
        assertTrue(result.steps[1].isDone)
        assertTrue(result.steps[2].isDone)
        assertTrue(result.steps[3].isDone)
        assertTrue(result.steps[4].isDone)
        assertTrue(result.steps[5].isDone)
    }

    @Test
    fun `assemble should not normalize when trophy is not obtained`() {
        val resource = QuestResource(
            id = "asukaTrophy",
            requisites = emptyList(),
            steps = steps(0x80, 0x01, 0, 0, 0, 0),
        )

        val result = DuelIslandAssembler.assemble(resource)

        assertTrue(result.steps[0].isDone)
        assertTrue(result.steps[1].isDone)
        assertFalse(result.steps[2].isDone)
        assertFalse(result.steps[5].isDone)
    }

    @Test
    fun `assemble should normalize asuka rematch when last step is done`() {
        val resource = QuestResource(
            id = "asukaRematch",
            requisites = listOf(RequisiteResource(id = "asukaTrophy", value = 1u)),
            steps = steps(0, 0, 0, 0, 0, 1),
        )

        val result = DuelIslandAssembler.assemble(resource)

        assertTrue(result.steps[0].isDone)
        assertTrue(result.steps[1].isDone)
        assertTrue(result.steps[2].isDone)
        assertTrue(result.steps[3].isDone)
        assertTrue(result.steps[4].isDone)
        assertTrue(result.steps[5].isDone)
    }

    @Test
    fun `assemble should suppress steps when requisite is not met`() {
        val resource = QuestResource(
            id = "asukaRematch",
            requisites = listOf(RequisiteResource(id = "asukaTrophy", value = 0u)),
            steps = steps(0x80, 0x01, 0x02, 0x04, 0x08, 0),
        )

        val result = DuelIslandAssembler.assemble(resource)

        assertFalse(result.steps[0].isDone)
        assertFalse(result.steps[1].isDone)
        assertFalse(result.steps[2].isDone)
        assertFalse(result.steps[3].isDone)
        assertFalse(result.steps[4].isDone)
        assertFalse(result.steps[5].isDone)
    }

    @Test
    fun `assemble should suppress steps even when trophy step would cascade`() {
        val resource = QuestResource(
            id = "asukaRematch",
            requisites = listOf(RequisiteResource(id = "asukaTrophy", value = 0u)),
            steps = steps(0, 0, 0, 0, 0, 1),
        )

        val result = DuelIslandAssembler.assemble(resource)

        assertFalse(result.steps[0].isDone)
        assertFalse(result.steps[1].isDone)
        assertFalse(result.steps[2].isDone)
        assertFalse(result.steps[3].isDone)
        assertFalse(result.steps[4].isDone)
        assertFalse(result.steps[5].isDone)
    }
}
