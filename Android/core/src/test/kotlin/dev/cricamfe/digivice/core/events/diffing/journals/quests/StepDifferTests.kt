package dev.cricamfe.digivice.core.events.diffing.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Journals/Quests/StepDifferTests.cs`. */
class StepDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = Step(number = 1, isDone = true, requisites = emptyList())
        val new = Step(number = 1, isDone = true, requisites = emptyList())

        assertNull(StepDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Step(number = 2, isDone = true, requisites = listOf(Requisite(id = "1", isDone = true)))

        val result = StepDiffer.diff(null, new)

        assertNotNull(result)
        assertEquals(2, result!!.number)
        assertPresent(true, result.isDone)
        assertEquals(1, result.requisites.value.size)
        assertEquals("1", result.requisites.value[0].id)
    }

    @Test
    fun `diff returns a delta when is done changed`() {
        val previous = Step(number = 1, isDone = false, requisites = emptyList())
        val new = Step(number = 1, isDone = true, requisites = emptyList())

        val result = StepDiffer.diff(previous, new)!!

        assertEquals(1, result.number)
        assertPresent(true, result.isDone)
        assertAbsent(result.requisites)
    }

    @Test
    fun `diff returns a delta when a nested requisite changed`() {
        val previous = Step(number = 1, isDone = false, requisites = listOf(Requisite(id = "1", isDone = false)))
        val new = Step(number = 1, isDone = false, requisites = listOf(Requisite(id = "1", isDone = true)))

        val result = StepDiffer.diff(previous, new)!!

        assertEquals(1, result.number)
        assertAbsent(result.isDone)
        assertEquals(1, result.requisites.value.size)
        assertPresent(true, result.requisites.value[0].isDone)
    }

    @Test
    fun `diff returns multiple deltas when is done and a nested requisite changed`() {
        val previous = Step(number = 1, isDone = false, requisites = listOf(Requisite(id = "1", isDone = false)))
        val new = Step(number = 1, isDone = true, requisites = listOf(Requisite(id = "1", isDone = true)))

        val result = StepDiffer.diff(previous, new)!!

        assertEquals(1, result.number)
        assertPresent(true, result.isDone)
        assertEquals(1, result.requisites.value.size)
        assertPresent(true, result.requisites.value[0].isDone)
    }

    // Not in the C# tests: requisites are matched by id, so only the changed one goes out.
    @Test
    fun `diff returns only the requisite that changed when a step has several`() {
        val previous = Step(
            number = 1,
            isDone = false,
            requisites = listOf(Requisite(id = "1", isDone = true), Requisite(id = "2", isDone = false)),
        )
        val new = Step(
            number = 1,
            isDone = false,
            requisites = listOf(Requisite(id = "1", isDone = true), Requisite(id = "2", isDone = true)),
        )

        val result = StepDiffer.diff(previous, new)!!

        assertAbsent(result.isDone)
        val requisite = result.requisites.value.single()
        assertEquals("2", requisite.id)
        assertPresent(true, requisite.isDone)
    }
}
