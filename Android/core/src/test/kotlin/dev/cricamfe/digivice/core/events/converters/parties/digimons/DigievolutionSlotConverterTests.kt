package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/Digimons/DigievolutionSlotConverterTests.cs`. */
class DigievolutionSlotConverterTests {
    @Test
    fun `toDto maps the slot and the nested digievolution`() {
        val slot = DigievolutionSlot(index = 2, digievolutionId = 10, digievolution = Digievolution(level = 4))

        val dto = DigievolutionSlotConverter.toDto(slot)

        assertEquals(2, dto.index)
        assertEquals(10, dto.digievolutionId.value)
        assertTrue(dto.digievolution.hasValue)
        assertNotNull(dto.digievolution.value)
        assertEquals(4, dto.digievolution.value!!.level.value)
    }

    @Test
    fun `toDto preserves a null digievolution`() {
        val slot = DigievolutionSlot(index = 3, digievolutionId = null, digievolution = null)

        val dto = DigievolutionSlotConverter.toDto(slot)

        assertEquals(3, dto.index)
        assertTrue(dto.digievolutionId.hasValue)
        assertNull(dto.digievolutionId.value)
        assertTrue(dto.digievolution.hasValue)
        assertNull(dto.digievolution.value)
    }
}
