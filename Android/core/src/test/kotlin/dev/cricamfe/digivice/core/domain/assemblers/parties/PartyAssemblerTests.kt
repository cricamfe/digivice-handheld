package dev.cricamfe.digivice.core.domain.assemblers.parties

import dev.cricamfe.digivice.core.domain.assemblers.PartyAssembler
import dev.cricamfe.digivice.core.memory.resources.PartyResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.AttributesResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.ElementsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.EquipmentsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PartyAssemblerTests {
    private companion object {
        const val EMPTY_SLOT_ID = 0xFF
    }

    @Test
    fun `assemble should map occupied and empty slots`() {
        val resource = PartyResource(
            slotsResource = listOf(
                DigimonSlotResource(
                    index = 1,
                    digimonId = 1,
                    digimonResource = DigimonResource(
                        experience = 100,
                        level = 2,
                        hp = VitalResource(),
                        mp = VitalResource(),
                        attributes = AttributesResource(),
                        elements = ElementsResource(),
                        equipments = EquipmentsResource(),
                        digievolutions = emptyList(),
                    ),
                ),
                DigimonSlotResource(index = 2, digimonId = EMPTY_SLOT_ID, digimonResource = null),
                DigimonSlotResource(index = 3, digimonId = EMPTY_SLOT_ID, digimonResource = null),
            ),
        )

        val result = PartyAssembler.assemble(resource)

        assertEquals(3, result.slots.size)

        assertEquals(1, result.slots[0].index)
        assertEquals(1, result.slots[0].digimonId)
        assertNotNull(result.slots[0].digimon)
        assertEquals(2, result.slots[0].digimon!!.level)

        assertEquals(2, result.slots[1].index)
        assertNull(result.slots[1].digimonId)
        assertNull(result.slots[1].digimon)

        assertEquals(3, result.slots[2].index)
        assertNull(result.slots[2].digimonId)
        assertNull(result.slots[2].digimon)
    }
}
