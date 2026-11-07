package dev.cricamfe.digivice.core.events.diffing.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/DigimonDifferTests.cs`. */
class DigimonDifferTests {
    private fun baseDigimon() = Digimon(
        level = 10,
        tp = 5,
        blast = 100,
        experience = 1000,
        activeDigievolutionId = 3,
        hp = Vital(current = 100, max = 100),
        mp = Vital(current = 50, max = 50),
        inBattle = InBattle(condition = 0, hp = Vital(current = 0, max = 0), mp = Vital(current = 0, max = 0)),
        attributes = Attributes(strength = 5, defense = 5, spirit = 5, wisdom = 5, speed = 5, charisma = 5),
        elements = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1),
        equipments = Equipments(),
        digievolutions = emptyList(),
    )

    @Test
    fun `diff returns null when there are no changes`() {
        assertNull(DigimonDiffer.diff(baseDigimon(), baseDigimon()))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = baseDigimon().also { it.level = 25 }

        val result = DigimonDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(25, result!!.level)
    }

    @Test
    fun `diff returns null when the new digimon is null`() {
        assertNull(DigimonDiffer.diff(baseDigimon(), null))
    }

    @Test
    fun `diff returns the changed fields when there are partial changes`() {
        val new = baseDigimon().also {
            it.level = 26
            it.hp.current = 150
        }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(26, result.level)
        assertAbsent(result.experience)
        assertPresent(150, result.hp.value.current)
        assertAbsent(result.mp)
    }

    @Test
    fun `diff returns the stored digievolutions delta when a stored level changes`() {
        val previous = baseDigimon().also { it.storedDigievolutions = listOf(StoredDigievolution(digievolutionId = 386, level = 14)) }
        val new = baseDigimon().also { it.storedDigievolutions = listOf(StoredDigievolution(digievolutionId = 386, level = 15)) }

        val result = DigimonDiffer.diff(previous, new)!!

        assertAbsent(result.level)
        val storedDelta = result.storedDigievolutions.value.single()
        assertPresent(386, storedDelta.digievolutionId)
        assertPresent(15, storedDelta.level)
    }

    @Test
    fun `diff returns the stored digievolutions delta when a new digievolution is unlocked`() {
        val previous = baseDigimon().also { it.storedDigievolutions = listOf(StoredDigievolution(digievolutionId = 386, level = 14)) }
        val new = baseDigimon().also {
            it.storedDigievolutions = listOf(
                StoredDigievolution(digievolutionId = 386, level = 14),
                StoredDigievolution(digievolutionId = 260, level = 1),
            )
        }

        val result = DigimonDiffer.diff(previous, new)!!

        val storedDelta = result.storedDigievolutions.value.single()
        assertPresent(260, storedDelta.digievolutionId)
        assertPresent(1, storedDelta.level)
    }

    @Test
    fun `diff returns the blast delta when only the blast changes`() {
        val new = baseDigimon().also { it.blast = 500 }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(500, result.blast)
        assertAbsent(result.level)
    }

    @Test
    fun `diff returns the in battle condition delta when only the in battle condition changes`() {
        val new = baseDigimon().also { it.inBattle.condition = 0x04 }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(0x04, result.inBattle.value.condition)
        assertAbsent(result.level)
    }

    @Test
    fun `diff returns an explicit null active digievolution id when the digievolution is deactivated`() {
        val new = baseDigimon().also { it.activeDigievolutionId = null }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(null, result.activeDigievolutionId)
        assertAbsent(result.level)
    }

    @Test
    fun `diff returns the active digievolution id when a digievolution is activated`() {
        val previous = baseDigimon().also { it.activeDigievolutionId = null }
        val new = baseDigimon().also { it.activeDigievolutionId = 12 }

        val result = DigimonDiffer.diff(previous, new)!!

        assertPresent(12, result.activeDigievolutionId)
    }

    @Test
    fun `diff returns the tp delta when only the tp changes`() {
        val new = baseDigimon().also { it.tp = 9 }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(9, result.tp)
        assertAbsent(result.level)
        assertAbsent(result.experience)
    }

    @Test
    fun `diff returns nested deltas when the attributes, elements and equipments change`() {
        val new = baseDigimon().also {
            it.attributes.strength = 6
            it.elements.fire = 2
            it.equipments.head = 101
        }

        val result = DigimonDiffer.diff(baseDigimon(), new)!!

        assertPresent(6, result.attributes.value.strength)
        assertAbsent(result.attributes.value.defense)
        assertPresent(2, result.elements.value.fire)
        assertAbsent(result.elements.value.water)
        assertPresent(101, result.equipments.value.head)
        assertAbsent(result.equipments.value.body)
        assertAbsent(result.level)
    }

    @Test
    fun `diff returns only the changed digievolution slot when an empty slot is filled`() {
        val previous = baseDigimon().also {
            it.digievolutions = listOf(
                DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5)),
                DigievolutionSlot(index = 2, digievolutionId = null, digievolution = null),
                DigievolutionSlot(index = 3, digievolutionId = null, digievolution = null),
            )
        }
        val new = baseDigimon().also {
            it.digievolutions = listOf(
                DigievolutionSlot(index = 1, digievolutionId = 4, digievolution = Digievolution(level = 5)),
                DigievolutionSlot(index = 2, digievolutionId = 12, digievolution = Digievolution(level = 1)),
                DigievolutionSlot(index = 3, digievolutionId = null, digievolution = null),
            )
        }

        val result = DigimonDiffer.diff(previous, new)!!

        val slotDelta = result.digievolutions.value.single()
        assertEquals(2, slotDelta.index)
        assertPresent(12, slotDelta.digievolutionId)
        assertPresent(1, presentValue(slotDelta.digievolution).level)
    }
}
