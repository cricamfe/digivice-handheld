package dev.cricamfe.digivice.core.domain.assemblers.journals

import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestAssemblerTests {
    @Test
    fun `assemble should map all fields correctly`() {
        val resource = QuestResource(
            id = "12",
            requisites = listOf(RequisiteResource(id = "1", value = 1u)),
            steps = listOf(StepResource(number = 0, value = 1u, requisites = emptyList())),
        )

        val result = QuestAssembler.assemble(resource)

        assertEquals("12", result.id)
        assertEquals(1, result.requisites.size)
        assertEquals(1, result.steps.size)
        assertEquals(0, result.steps[0].number)
    }
}
