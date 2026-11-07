package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/DigievolutionSlotDifferTests.cs`. */
class DigievolutionSlotDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5))
        val new = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5))

        assertNull(DigievolutionSlotDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5))

        val result = DigievolutionSlotDiffer.diff(null, new)

        assertNotNull(result)
        assertEquals(1, result!!.index)
        assertPresent(4, result.digievolutionId)
        assertPresent(5, presentValue(result.digievolution).level)
    }

    @Test
    fun `diff returns the full dto when the digievolution id changed`() {
        val previous = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5))
        val new = DigievolutionSlot(index = 1, digievolutionId = 5, digievolution = Digievolution(level = 5))

        val result = DigievolutionSlotDiffer.diff(previous, new)!!

        assertEquals(1, result.index)
        assertPresent(5, result.digievolutionId)
        val digievolution = presentValue(result.digievolution)
        assertPresent(5, digievolution.level)
        assertTrue(digievolution.dvexp.hasValue)
    }

    @Test
    fun `diff returns the full dto when an empty slot is filled`() {
        val previous = DigievolutionSlot(index = 2, digievolutionId = null, digievolution = null)
        val new = DigievolutionSlot(index = 2, digievolutionId = 12, digievolution = Digievolution(level = 1, dvexp = 0))

        val result = DigievolutionSlotDiffer.diff(previous, new)!!

        assertEquals(2, result.index)
        assertPresent(12, result.digievolutionId)
        val digievolution = presentValue(result.digievolution)
        assertPresent(1, digievolution.level)
        assertPresent(0, digievolution.dvexp)
    }

    @Test
    fun `diff returns a delta when only the nested level changed`() {
        val previous = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5))
        val new = DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 6))

        val result = DigievolutionSlotDiffer.diff(previous, new)!!

        assertEquals(1, result.index)
        assertAbsent(result.digievolutionId)
        assertPresent(6, presentValue(result.digievolution).level)
    }

    @Test
    fun `diff returns explicit nulls when a filled slot becomes empty`() {
        val previous = DigievolutionSlot(index = 3, digievolutionId = 12, digievolution = Digievolution(level = 2, dvexp = 15))
        val new = DigievolutionSlot(index = 3, digievolutionId = null, digievolution = null)

        val result = DigievolutionSlotDiffer.diff(previous, new)!!

        assertEquals(3, result.index)
        assertPresent(null, result.digievolutionId)
        assertPresent(null, result.digievolution)
    }
}
