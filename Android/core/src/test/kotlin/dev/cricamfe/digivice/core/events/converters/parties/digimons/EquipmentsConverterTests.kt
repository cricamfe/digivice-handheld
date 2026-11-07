package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/EquipmentsConverterTests.cs`. */
class EquipmentsConverterTests {
    @Test
    fun `toDto maps all equipment fields`() {
        val dto = EquipmentsConverter.toDto(
            Equipments(head = 1, body = 2, right = 3, left = 4, accessory1 = 5, accessory2 = 6),
        )

        assertEquals(1, dto.head.value)
        assertEquals(2, dto.body.value)
        assertEquals(3, dto.right.value)
        assertEquals(4, dto.left.value)
        assertEquals(5, dto.accessory1.value)
        assertEquals(6, dto.accessory2.value)
    }

    @Test
    fun `toDto preserves null equipment slots`() {
        val dto = EquipmentsConverter.toDto(Equipments(head = 101))

        assertEquals(101, dto.head.value)
        assertTrue(dto.body.hasValue)
        assertNull(dto.body.value)
        assertTrue(dto.accessory2.hasValue)
        assertNull(dto.accessory2.value)
    }
}
