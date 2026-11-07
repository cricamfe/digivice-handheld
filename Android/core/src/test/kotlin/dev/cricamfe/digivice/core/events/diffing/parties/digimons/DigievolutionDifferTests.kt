package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/DigievolutionDifferTests.cs`. */
class DigievolutionDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(DigievolutionDiffer.diff(Digievolution(level = 15, dvexp = 100), Digievolution(level = 15, dvexp = 100)))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = DigievolutionDiffer.diff(null, Digievolution(level = 25, dvexp = 300))

        assertNotNull(result)
        assertPresent(25, result!!.level)
        assertPresent(300, result.dvexp)
    }

    @Test
    fun `diff returns the changed level when the level changed`() {
        val result = DigievolutionDiffer.diff(Digievolution(level = 15, dvexp = 100), Digievolution(level = 16, dvexp = 100))!!

        assertPresent(16, result.level)
        assertAbsent(result.dvexp)
    }

    @Test
    fun `diff returns the changed dvexp when only the dvexp changed`() {
        val result = DigievolutionDiffer.diff(Digievolution(level = 15, dvexp = 100), Digievolution(level = 15, dvexp = 144))!!

        assertAbsent(result.level)
        assertPresent(144, result.dvexp)
    }

    @Test
    fun `diff returns null when the new digievolution is null`() {
        assertNull(DigievolutionDiffer.diff(Digievolution(level = 15, dvexp = 100), null))
    }
}
