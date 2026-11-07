package dev.cricamfe.digivice.core.events.diffing.journals

import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Journals/QuestDifferTests.cs`. */
class QuestDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = Quest(id = "12", requisites = emptyList(), steps = emptyList())
        val new = Quest(id = "12", requisites = emptyList(), steps = emptyList())

        assertNull(QuestDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Quest(id = "12", requisites = emptyList(), steps = listOf(Step(number = 0, isDone = true, requisites = emptyList())))

        val result = QuestDiffer.diff(null, new)

        assertNotNull(result)
        assertEquals("12", result!!.id)
        assertEquals(1, result.steps.value.size)
    }

    @Test
    fun `diff returns a delta when a step changed`() {
        val previous = Quest(id = "12", requisites = emptyList(), steps = listOf(Step(number = 0, isDone = false, requisites = emptyList())))
        val new = Quest(id = "12", requisites = emptyList(), steps = listOf(Step(number = 0, isDone = true, requisites = emptyList())))

        val result = QuestDiffer.diff(previous, new)!!

        assertEquals("12", result.id)
        assertEquals(1, result.steps.value.size)
        assertPresent(true, result.steps.value[0].isDone)
    }

    @Test
    fun `diff returns a delta when a requisite changed`() {
        val previous = Quest(id = "12", requisites = listOf(Requisite(id = "1", isDone = false)), steps = emptyList())
        val new = Quest(id = "12", requisites = listOf(Requisite(id = "1", isDone = true)), steps = emptyList())

        val result = QuestDiffer.diff(previous, new)!!

        assertEquals("12", result.id)
        assertEquals(1, result.requisites.value.size)
        assertPresent(true, result.requisites.value[0].isDone)
        assertAbsent(result.steps)
    }

    @Test
    fun `diff returns multiple deltas when a requisite and a step changed`() {
        val previous = Quest(
            id = "12",
            requisites = listOf(Requisite(id = "1", isDone = false)),
            steps = listOf(Step(number = 0, isDone = false, requisites = emptyList())),
        )
        val new = Quest(
            id = "12",
            requisites = listOf(Requisite(id = "1", isDone = true)),
            steps = listOf(Step(number = 0, isDone = true, requisites = emptyList())),
        )

        val result = QuestDiffer.diff(previous, new)!!

        assertEquals("12", result.id)
        assertEquals(1, result.requisites.value.size)
        assertPresent(true, result.requisites.value[0].isDone)
        assertEquals(1, result.steps.value.size)
        assertPresent(true, result.steps.value[0].isDone)
    }

    // Not in the C# tests: requisites and steps are matched by id and number, so only the changed ones go out.
    @Test
    fun `diff returns only the requisite and the step that changed when a quest has several`() {
        val previous = Quest(
            id = "12",
            requisites = listOf(Requisite(id = "1", isDone = true), Requisite(id = "2", isDone = false)),
            steps = listOf(Step(number = 1, isDone = true), Step(number = 2, isDone = false)),
        )
        val new = Quest(
            id = "12",
            requisites = listOf(Requisite(id = "1", isDone = true), Requisite(id = "2", isDone = true)),
            steps = listOf(Step(number = 1, isDone = true), Step(number = 2, isDone = true)),
        )

        val result = QuestDiffer.diff(previous, new)!!

        assertEquals("2", result.requisites.value.single().id)
        assertEquals(2, result.steps.value.single().number)
    }
}
