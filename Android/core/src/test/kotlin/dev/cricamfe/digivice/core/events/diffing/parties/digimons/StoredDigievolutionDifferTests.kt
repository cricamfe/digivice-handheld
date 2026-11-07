package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/StoredDigievolutionDifferTests.cs`. */
class StoredDigievolutionDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = StoredDigievolution(digievolutionId = 386, level = 14)
        val new = StoredDigievolution(digievolutionId = 386, level = 14)

        assertNull(StoredDigievolutionDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = StoredDigievolutionDiffer.diff(null, StoredDigievolution(digievolutionId = 260, level = 1))

        assertNotNull(result)
        assertPresent(260, result!!.digievolutionId)
        assertPresent(1, result.level)
    }

    @Test
    fun `diff returns the changed level with the digievolution id when the level changed`() {
        val previous = StoredDigievolution(digievolutionId = 386, level = 14)
        val new = StoredDigievolution(digievolutionId = 386, level = 15)

        val result = StoredDigievolutionDiffer.diff(previous, new)!!

        assertPresent(386, result.digievolutionId)
        assertPresent(15, result.level)
    }
}
