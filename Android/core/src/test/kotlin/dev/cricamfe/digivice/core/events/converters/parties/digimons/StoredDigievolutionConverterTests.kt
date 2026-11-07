package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/StoredDigievolutionConverterTests.cs`. */
class StoredDigievolutionConverterTests {
    @Test
    fun `toDto maps digievolution id and level`() {
        val dto = StoredDigievolutionConverter.toDto(StoredDigievolution(digievolutionId = 386, level = 14))

        assertEquals(386, dto.digievolutionId.value)
        assertEquals(14, dto.level.value)
    }
}
