package dev.cricamfe.digivice.core.events.diffing.journals.quests

import dev.cricamfe.digivice.core.domain.models.journals.quests.Requisite
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Journals/Quests/RequisiteDifferTests.cs`. */
class RequisiteDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(RequisiteDiffer.diff(Requisite(id = "5", isDone = true), Requisite(id = "5", isDone = true)))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = RequisiteDiffer.diff(null, Requisite(id = "5", isDone = true))

        assertNotNull(result)
        assertEquals("5", result!!.id)
        assertPresent(true, result.isDone)
    }

    @Test
    fun `diff returns a delta when is done changed`() {
        val result = RequisiteDiffer.diff(Requisite(id = "5", isDone = false), Requisite(id = "5", isDone = true))

        assertNotNull(result)
        assertEquals("5", result!!.id)
        assertPresent(true, result.isDone)
    }
}
