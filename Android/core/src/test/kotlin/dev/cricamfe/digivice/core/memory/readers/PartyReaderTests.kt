package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeDigimonSlotReader
import dev.cricamfe.digivice.core.memory.addresses.PartyAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PartyReaderTests {
    @Test
    fun `read should map party resource correctly`() {
        val slot1 = SlotAddresses(index = 0, address = 0x1000)
        val slot2 = SlotAddresses(index = 1, address = 0x2000)
        val addresses = PartyAddresses(emptySlotId = 255, slots = listOf(slot1, slot2))
        val expectedResource1 = DigimonSlotResource(index = 0, digimonId = 1)
        val expectedResource2 = DigimonSlotResource(index = 1, digimonId = 2)
        val slotReader = FakeDigimonSlotReader(mapOf(slot1 to expectedResource1, slot2 to expectedResource2))

        val result = PartyReader(slotReader).read(addresses)

        assertEquals(2, result.slotsResource.size)
        assertEquals(expectedResource1, result.slotsResource[0])
        assertEquals(expectedResource2, result.slotsResource[1])
    }
}
