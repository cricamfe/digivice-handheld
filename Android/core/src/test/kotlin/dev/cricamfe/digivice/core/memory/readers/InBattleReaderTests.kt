package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class InBattleReaderTests {
    private companion object {
        const val ALLY_SLOT_BASE = 0x000A4470L
        const val SLOT_STRIDE = 0x20
        const val STRENGTH_OFFSET = 0x10
    }

    @ParameterizedTest
    @CsvSource("1,0", "2,32", "3,64")
    fun `read should resolve ally slot from party slot index`(partySlotIndex: Int, expectedSlotOffset: Int) {
        val addresses = InBattleAddresses(
            allySlotBase = ALLY_SLOT_BASE,
            slotStride = SLOT_STRIDE,
            strength = STRENGTH_OFFSET,
        )
        val memoryReader = FakeMemoryReader().apply {
            int16[ALLY_SLOT_BASE + expectedSlotOffset + STRENGTH_OFFSET] = 252
        }

        val result = InBattleReader(memoryReader).read(addresses, partySlotIndex)

        assertEquals(252, result.strength)
    }

    @ParameterizedTest
    @CsvSource("1,0", "2,32", "3,64")
    fun `read should take the form the fighter is in from the first word of its slot`(partySlotIndex: Int, expectedSlotOffset: Int) {
        val addresses = InBattleAddresses(allySlotBase = ALLY_SLOT_BASE, slotStride = SLOT_STRIDE, id = 0)
        val memoryReader = FakeMemoryReader().apply {
            int16[ALLY_SLOT_BASE + expectedSlotOffset] = 389            // Kyukimon, where the partner's own id is 383
        }

        val result = InBattleReader(memoryReader).read(addresses, partySlotIndex)

        assertEquals(389, result.formId)
    }
}
