package dev.cricamfe.digivice.core.domain.assemblers.journals.quests

import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RequisiteAssemblerTests {
    @Test
    fun `assemble should map all fields correctly`() {
        val result = RequisiteAssembler.assemble(RequisiteResource(id = "5", value = 1u))

        assertEquals("5", result.id)
        assertTrue(result.isDone)
    }

    @ParameterizedTest
    @CsvSource("0,false", "1,true", "128,true")
    fun `assemble should map is done from non zero value`(value: Int, expectedIsDone: Boolean) {
        val result = RequisiteAssembler.assemble(RequisiteResource(id = "5", value = value.toUByte()))

        assertEquals(expectedIsDone, result.isDone)
    }
}
