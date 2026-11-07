package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/VitalConverterTests.cs`. */
class VitalConverterTests {
    @Test
    fun `toDto maps all vital fields`() {
        val dto = VitalConverter.toDto(Vital(current = 1, max = 2))

        assertEquals(1, dto.current.value)
        assertEquals(2, dto.max.value)
    }
}
