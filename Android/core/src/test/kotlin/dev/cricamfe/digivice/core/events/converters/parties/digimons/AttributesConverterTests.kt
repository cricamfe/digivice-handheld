package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/AttributesConverterTests.cs`. */
class AttributesConverterTests {
    @Test
    fun `toDto maps all attributes fields`() {
        val dto = AttributesConverter.toDto(
            Attributes(strength = 1, defense = 2, spirit = 3, wisdom = 4, speed = 5, charisma = 6),
        )

        assertEquals(1, dto.strength.value)
        assertEquals(2, dto.defense.value)
        assertEquals(3, dto.spirit.value)
        assertEquals(4, dto.wisdom.value)
        assertEquals(5, dto.speed.value)
        assertEquals(6, dto.charisma.value)
    }
}
