package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakePartyLoader
import dev.cricamfe.digivice.core.memory.resources.PartyResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PartyProviderTests {
    private companion object {
        const val EMPTY_SLOT_ID = 0xFF
    }

    @Test
    fun `get should load resource and apply party assembler`() {
        val partyResource = PartyResource(
            slotsResource = listOf(
                DigimonSlotResource(index = 1, digimonId = 1, digimonResource = DigimonResource(level = 12)),
                DigimonSlotResource(index = 2, digimonId = EMPTY_SLOT_ID, digimonResource = null),
                DigimonSlotResource(index = 3, digimonId = EMPTY_SLOT_ID, digimonResource = null),
            ),
        )
        val loader = FakePartyLoader(partyResource)

        val result = PartyProvider(loader).get()

        assertEquals(3, result.slots.size)
        assertEquals(1, result.slots[0].digimonId)
        assertEquals(12, result.slots[0].digimon!!.level)
        assertNull(result.slots[1].digimonId)
        assertNull(result.slots[2].digimonId)
        assertEquals(1, loader.calls)
    }
}
