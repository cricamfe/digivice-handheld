package dev.cricamfe.digivice.core.domain.assemblers.journals.quests

import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class StepAssemblerTests {
    @Test
    fun `assemble should map all fields correctly`() {
        val resource = StepResource(
            number = 3,
            value = 1u,
            requisites = listOf(RequisiteResource(id = "1", value = 1u)),
        )

        val result = StepAssembler.assemble(resource)

        assertEquals(3, result.number)
        assertTrue(result.isDone)
        assertEquals(1, result.requisites.size)
        assertEquals("1", result.requisites[0].id)
    }

    @ParameterizedTest
    @CsvSource("0,false", "1,true", "128,true")
    fun `assemble should map is done from non zero value`(value: Int, expectedIsDone: Boolean) {
        val result = StepAssembler.assemble(StepResource(number = 1, value = value.toUByte(), requisites = emptyList()))

        assertEquals(expectedIsDone, result.isDone)
    }
}
