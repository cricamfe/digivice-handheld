package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/PartyDifferTests.cs`. */
class PartyDifferTests {
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

    private fun party() = Party(
        slots = listOf(
            DigimonSlot(index = 1, digimonId = 1, digimon = baseDigimon()),
            DigimonSlot(index = 2, digimonId = null, digimon = null),
            DigimonSlot(index = 3, digimonId = null, digimon = null),
        ),
    )

    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val result = PartyDiffer.diff(party(), party())

        assertAbsent(result.slots)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val result = PartyDiffer.diff(null, party())

        assertTrue(result.slots.hasValue)
        val slots = result.slots.value
        assertEquals(3, slots.size)
        assertEquals(1, slots[0].index)
        assertPresent(1, slots[0].digimonId)
        assertPresent(null, slots[1].digimonId)
        assertPresent(null, slots[2].digimonId)
    }

    @Test
    fun `diff returns only the changed slot when one slot changed`() {
        val new = party().also { it.slots[0].digimon!!.level = 15 }

        val result = PartyDiffer.diff(party(), new)

        val slot = result.slots.value.single()
        assertEquals(1, slot.index)
        val digimon = presentValue(slot.digimon)
        assertPresent(15, digimon.level)
        assertAbsent(digimon.experience)
    }

    @Test
    fun `diff returns the joined slot when a digimon joins an empty slot`() {
        val new = Party(
            slots = party().slots.toMutableList().also {
                it[1] = DigimonSlot(index = 2, digimonId = 2, digimon = baseDigimon())
            },
        )

        val result = PartyDiffer.diff(party(), new)

        val slot = result.slots.value.single()
        assertEquals(2, slot.index)
        assertPresent(2, slot.digimonId)
        presentValue(slot.digimon)
    }
}
