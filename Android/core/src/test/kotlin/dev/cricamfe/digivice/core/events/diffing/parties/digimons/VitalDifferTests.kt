package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/VitalDifferTests.cs`. */
class VitalDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(VitalDiffer.diff(Vital(current = 10, max = 20), Vital(current = 10, max = 20)))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = VitalDiffer.diff(null, Vital(current = 10, max = 20))

        assertNotNull(result)
        assertPresent(10, result!!.current)
        assertPresent(20, result.max)
    }

    @Test
    fun `diff returns only the changed fields when there are partial changes`() {
        val result = VitalDiffer.diff(Vital(current = 10, max = 20), Vital(current = 15, max = 20))!!

        assertPresent(15, result.current)
        assertAbsent(result.max)
    }
}
