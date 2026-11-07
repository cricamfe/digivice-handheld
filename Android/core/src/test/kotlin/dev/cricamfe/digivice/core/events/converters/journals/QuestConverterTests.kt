package dev.cricamfe.digivice.core.events.converters.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Journals/QuestConverterTests.cs`. */
class QuestConverterTests {
    @Test
    fun `toDto maps quest steps and requisites`() {
        val quest = Quest(
            id = "QuestA",
            requisites = listOf(Requisite(id = "ReqA", isDone = true)),
            steps = listOf(
                Step(number = 2, isDone = true, requisites = listOf(Requisite(id = "ReqB", isDone = true))),
            ),
        )

        val dto = QuestConverter.toDto(quest)

        assertEquals("QuestA", dto.id)
        val requisite = dto.requisites.value.single()
        assertEquals("ReqA", requisite.id)
        assertTrue(requisite.isDone.value)
        val step = dto.steps.value.single()
        assertEquals(2, step.number)
        assertTrue(step.isDone.value)
        val nestedRequisite = step.requisites.value.single()
        assertEquals("ReqB", nestedRequisite.id)
        assertTrue(nestedRequisite.isDone.value)
    }
}
