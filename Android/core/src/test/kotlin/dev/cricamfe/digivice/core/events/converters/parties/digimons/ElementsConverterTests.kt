package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/ElementsConverterTests.cs`. */
class ElementsConverterTests {
    @Test
    fun `toDto maps all element fields`() {
        val dto = ElementsConverter.toDto(
            Elements(fire = 1, water = 2, ice = 3, wind = 4, thunder = 5, machine = 6, dark = 7),
        )

        assertEquals(1, dto.fire.value)
        assertEquals(2, dto.water.value)
        assertEquals(3, dto.ice.value)
        assertEquals(4, dto.wind.value)
        assertEquals(5, dto.thunder.value)
        assertEquals(6, dto.machine.value)
        assertEquals(7, dto.dark.value)
    }
}
