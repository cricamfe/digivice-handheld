package dev.cricamfe.digivice.core.events.converters.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Journals/Quests/RequisiteConverterTests.cs`. */
class RequisiteConverterTests {
    @Test
    fun `toDto maps all requisite fields`() {
        val dto = RequisiteConverter.toDto(Requisite(id = "ReqA", isDone = true))

        assertEquals("ReqA", dto.id)
        assertTrue(dto.isDone.value)
    }
}
