package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/JournalConverterTests.cs`. */
class JournalConverterTests {
    @Test
    fun `toDto maps main quest and side quests`() {
        val journal = Journal(
            mainQuest = Quest(id = "MainQuest", steps = listOf(Step(number = 1, isDone = true)), requisites = emptyList()),
            sideQuests = listOf(
                Quest(id = "folderBag", steps = listOf(Step(number = 1, isDone = false)), requisites = emptyList()),
            ),
        )

        val dto = JournalConverter.toDto(journal)

        assertTrue(dto.mainQuest.hasValue)
        assertEquals("MainQuest", dto.mainQuest.value.id)
        assertTrue(dto.mainQuest.value.steps.hasValue)
        assertEquals(1, dto.mainQuest.value.steps.value[0].number)
        assertTrue(dto.mainQuest.value.steps.value[0].isDone.value)
        assertTrue(dto.sideQuests.hasValue)
        val sideQuest = dto.sideQuests.value.single()
        assertEquals("folderBag", sideQuest.id)
        assertTrue(sideQuest.steps.hasValue)
        assertFalse(sideQuest.steps.value[0].isDone.value)
    }

    @Test
    fun `toDto keeps empty lists present`() {
        val dto = JournalConverter.toDto(Journal())

        assertEquals(emptyList<Any>(), dto.legendaryWeapons.value)
        assertEquals(emptyList<Any>(), dto.driAgents.value)
        assertEquals(emptyList<Any>(), dto.duelIsland.value)
    }
}
