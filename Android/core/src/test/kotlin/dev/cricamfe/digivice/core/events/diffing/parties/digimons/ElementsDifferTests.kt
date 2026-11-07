package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/ElementsDifferTests.cs`. */
class ElementsDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1)
        val new = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1)

        assertNull(ElementsDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Elements(fire = 1, water = 2, ice = 3, wind = 4, thunder = 5, machine = 6, dark = 7)

        val result = ElementsDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(1, result!!.fire)
        assertPresent(2, result.water)
        assertPresent(3, result.ice)
        assertPresent(4, result.wind)
        assertPresent(5, result.thunder)
        assertPresent(6, result.machine)
        assertPresent(7, result.dark)
    }

    @Test
    fun `diff returns only the changed fields when there are partial changes`() {
        val previous = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1)
        val new = Elements(fire = 1, water = 3, ice = 1, wind = 1, thunder = 1, machine = 5, dark = 1)

        val result = ElementsDiffer.diff(previous, new)!!

        assertAbsent(result.fire)
        assertPresent(3, result.water)
        assertAbsent(result.ice)
        assertAbsent(result.wind)
        assertAbsent(result.thunder)
        assertPresent(5, result.machine)
        assertAbsent(result.dark)
    }
}
