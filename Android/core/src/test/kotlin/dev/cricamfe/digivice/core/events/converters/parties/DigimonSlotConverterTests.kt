package dev.cricamfe.digivice.core.events.converters.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/Parties/DigimonSlotConverterTests.cs`. */
class DigimonSlotConverterTests {
    @Test
    fun `toDto maps the slot and the nested digimon`() {
        val slot = DigimonSlot(index = 2, digimonId = 7, digimon = Digimon(level = 30))

        val dto = DigimonSlotConverter.toDto(slot)

        assertEquals(2, dto.index)
        assertTrue(dto.digimonId.hasValue)
        assertEquals(7, dto.digimonId.value)
        assertTrue(dto.digimon.hasValue)
        assertNotNull(dto.digimon.value)
        assertEquals(30, dto.digimon.value!!.level.value)
    }

    @Test
    fun `toDto preserves a null digimon`() {
        val slot = DigimonSlot(index = 3, digimonId = 255, digimon = null)

        val dto = DigimonSlotConverter.toDto(slot)

        assertEquals(3, dto.index)
        assertTrue(dto.digimonId.hasValue)
        assertEquals(255, dto.digimonId.value)
        assertTrue(dto.digimon.hasValue)
        assertNull(dto.digimon.value)
    }
}
