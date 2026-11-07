package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/PartyConverterTests.cs`. */
class PartyConverterTests {
    @Test
    fun `toDto maps party slots`() {
        val party = Party(slots = listOf(DigimonSlot(index = 1, digimonId = 7, digimon = Digimon(level = 33))))

        val dto = PartyConverter.toDto(party)

        assertTrue(dto.slots.hasValue)
        val slot = dto.slots.value.single()
        assertEquals(1, slot.index)
        assertTrue(slot.digimonId.hasValue)
        assertEquals(7, slot.digimonId.value)
        assertTrue(slot.digimon.hasValue)
        assertNotNull(slot.digimon.value)
        assertTrue(slot.digimon.value!!.level.hasValue)
        assertEquals(33, slot.digimon.value!!.level.value)
    }
}
