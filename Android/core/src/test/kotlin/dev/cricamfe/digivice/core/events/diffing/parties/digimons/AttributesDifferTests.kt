package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/AttributesDifferTests.cs`. */
class AttributesDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = Attributes(strength = 10, defense = 10, spirit = 10, wisdom = 10, speed = 10, charisma = 10)
        val new = Attributes(strength = 10, defense = 10, spirit = 10, wisdom = 10, speed = 10, charisma = 10)

        assertNull(AttributesDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Attributes(strength = 10, defense = 11, spirit = 12, wisdom = 13, speed = 14, charisma = 15)

        val result = AttributesDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(10, result!!.strength)
        assertPresent(11, result.defense)
        assertPresent(12, result.spirit)
        assertPresent(13, result.wisdom)
        assertPresent(14, result.speed)
        assertPresent(15, result.charisma)
    }

    @Test
    fun `diff returns only the changed fields when there are partial changes`() {
        val previous = Attributes(strength = 10, defense = 10, spirit = 10, wisdom = 10, speed = 10, charisma = 10)
        val new = Attributes(strength = 10, defense = 12, spirit = 10, wisdom = 10, speed = 15, charisma = 10)

        val result = AttributesDiffer.diff(previous, new)!!

        assertAbsent(result.strength)
        assertPresent(12, result.defense)
        assertAbsent(result.spirit)
        assertAbsent(result.wisdom)
        assertPresent(15, result.speed)
        assertAbsent(result.charisma)
    }
}
