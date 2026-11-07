package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.CardBattleAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardBattleReaderTests {
    @Test
    fun `read should map card battle resource correctly`() {
        val addresses = CardBattleAddresses(id = 0x0004B404)
        val memoryReader = FakeMemoryReader().apply { int32[0x0004B404] = 11 }

        val result = CardBattleReader(memoryReader).read(addresses)

        assertEquals(11, result.id)
    }
}
