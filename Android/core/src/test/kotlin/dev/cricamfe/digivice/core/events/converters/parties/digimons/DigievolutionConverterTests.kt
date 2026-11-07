package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/DigievolutionConverterTests.cs`. */
class DigievolutionConverterTests {
    @Test
    fun `toDto maps level and dvexp`() {
        val dto = DigievolutionConverter.toDto(Digievolution(level = 12, dvexp = 139))

        assertEquals(12, dto.level.value)
        assertEquals(139, dto.dvexp.value)
    }
}
