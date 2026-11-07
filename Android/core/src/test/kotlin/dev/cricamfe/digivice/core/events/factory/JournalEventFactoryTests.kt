package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/JournalEventFactoryTests.cs`. */
class JournalEventFactoryTests {
    private fun state(journal: Journal) = State(journal = journal)

    private fun journal(stepIsDone: Boolean) = Journal(
        mainQuest = Quest(
            id = "MainQuest",
            steps = listOf(Step(number = 1, isDone = stepIsDone)),
            requisites = emptyList(),
        ),
        sideQuests = emptyList(),
    )

    @Test
    fun `create returns no events when the journal has no changes`() {
        assertTrue(JournalEventFactory.create(state(journal(false)), state(journal(false))).isEmpty())
    }

    @Test
    fun `create returns a journal changed event when the journal changes`() {
        val event = JournalEventFactory.create(state(journal(false)), state(journal(true))).single()

        assertEquals(EventType.JournalChanged, event.type)
        val dto = assertInstanceOf(JournalDto::class.java, event.payload)
        assertEquals("MainQuest", dto.mainQuest.value.id)
    }
}
