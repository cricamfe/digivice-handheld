package dev.cricamfe.digivice.core.events.converters.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Journals/Quests/StepConverterTests.cs`. */
class StepConverterTests {
    @Test
    fun `toDto maps the step and its nested requisites`() {
        val step = Step(number = 5, isDone = true, requisites = listOf(Requisite(id = "ReqA", isDone = true)))

        val dto = StepConverter.toDto(step)

        assertEquals(5, dto.number)
        assertTrue(dto.isDone.value)
        val requisite = dto.requisites.value.single()
        assertEquals("ReqA", requisite.id)
        assertTrue(requisite.isDone.value)
    }
}
