package dev.cricamfe.digivice.core.events.diffing.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/DigimonSlotDifferTests.cs`. */
class DigimonSlotDifferTests {
    private fun baseDigimon() = Digimon(
        level = 10,
        experience = 1000,
        activeDigievolutionId = 3,
        hp = Vital(current = 100, max = 100),
        mp = Vital(current = 50, max = 50),
        attributes = Attributes(strength = 5, defense = 5, spirit = 5, wisdom = 5, speed = 5, charisma = 5),
        elements = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1),
        equipments = Equipments(),
        digievolutions = emptyList(),
    )

    @Test
    fun `diff returns null when there are no changes`() {
        val previous = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())
        val new = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())

        assertNull(DigimonSlotDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())

        val result = DigimonSlotDiffer.diff(null, new)

        assertNotNull(result)
        assertEquals(1, result!!.index)
        assertPresent(3, result.digimonId)
        presentValue(result.digimon)
    }

    @Test
    fun `diff returns a delta when the digimon id changed to null`() {
        val previous = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())
        val new = DigimonSlot(index = 1, digimonId = null, digimon = null)

        val result = DigimonSlotDiffer.diff(previous, new)!!

        assertEquals(1, result.index)
        assertPresent(null, result.digimonId)
        assertPresent(null, result.digimon)
    }

    @Test
    fun `diff returns a delta when the nested digimon changed`() {
        val previous = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())
        val new = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon().also { it.level = 12 })

        val result = DigimonSlotDiffer.diff(previous, new)!!

        assertEquals(1, result.index)
        assertPresent(3, result.digimonId)
        assertPresent(12, presentValue(result.digimon).level)
    }

    @Test
    fun `diff returns null when both slots are empty`() {
        val previous = DigimonSlot(index = 2, digimonId = null, digimon = null)
        val new = DigimonSlot(index = 2, digimonId = null, digimon = null)

        assertNull(DigimonSlotDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when kotemon joins an empty slot`() {
        val previous = DigimonSlot(index = 2, digimonId = null, digimon = null)
        val new = DigimonSlot(index = 2, digimonId = 0, digimon = baseDigimon())

        val result = DigimonSlotDiffer.diff(previous, new)!!

        assertEquals(2, result.index)
        assertPresent(0, result.digimonId)
        assertPresent(10, presentValue(result.digimon).level)
    }

    @Test
    fun `diff returns the full dto when the digimon id changed to another active id`() {
        val previous = DigimonSlot(index = 1, digimonId = 3, digimon = baseDigimon())
        val new = DigimonSlot(index = 1, digimonId = 5, digimon = baseDigimon().also { it.level = 15 })

        val result = DigimonSlotDiffer.diff(previous, new)!!

        assertEquals(1, result.index)
        assertPresent(5, result.digimonId)
        assertPresent(15, presentValue(result.digimon).level)
    }
}
