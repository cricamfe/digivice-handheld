package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Parties/Digimons/EquipmentsDifferTests.cs`. */
class EquipmentsDifferTests {
    @Test
    fun `diff returns null when there are no changes`() {
        val previous = Equipments(head = 1, body = 1, right = 1, left = 1, accessory1 = 1, accessory2 = 1)
        val new = Equipments(head = 1, body = 1, right = 1, left = 1, accessory1 = 1, accessory2 = 1)

        assertNull(EquipmentsDiffer.diff(previous, new))
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Equipments(head = 101, body = 102, right = 103, left = 104, accessory1 = 105, accessory2 = 106)

        val result = EquipmentsDiffer.diff(null, new)

        assertNotNull(result)
        assertPresent(101, result!!.head)
        assertPresent(102, result.body)
        assertPresent(103, result.right)
        assertPresent(104, result.left)
        assertPresent(105, result.accessory1)
        assertPresent(106, result.accessory2)
    }

    @Test
    fun `diff returns only the changed fields when there are partial changes`() {
        val previous = Equipments(head = 1, body = 1, right = 1, left = 1, accessory1 = 1, accessory2 = 1)
        val new = Equipments(head = 1, body = 200, right = 1, left = 1, accessory1 = 205, accessory2 = 1)

        val result = EquipmentsDiffer.diff(previous, new)!!

        assertAbsent(result.head)
        assertPresent(200, result.body)
        assertAbsent(result.right)
        assertAbsent(result.left)
        assertPresent(205, result.accessory1)
        assertAbsent(result.accessory2)
    }

    @Test
    fun `diff returns an explicit null when an equipment is removed`() {
        val previous = Equipments(head = 101, body = 1, right = 1, left = 1, accessory1 = 1, accessory2 = 1)
        val new = Equipments(head = null, body = 1, right = 1, left = 1, accessory1 = 1, accessory2 = 1)

        val result = EquipmentsDiffer.diff(previous, new)!!

        assertPresent(null, result.head)
        assertAbsent(result.body)
    }
}
