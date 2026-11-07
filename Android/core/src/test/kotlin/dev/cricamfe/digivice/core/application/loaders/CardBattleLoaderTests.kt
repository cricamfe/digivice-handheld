package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.CardBattleReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CardBattleLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate card battle addresses and reader`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply { int32[0x0004B404L] = 11 }

        val cardBattleLoader = CardBattleLoader(addressesRepository, CardBattleReader(memoryReader))

        val cardBattleResource = cardBattleLoader.load()

        assertEquals(11, cardBattleResource.id)
    }

    @Test
    fun `load should throw memory read exception when memory reader cannot read card battle data`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            int32Failures[0x0004B404L] = MemoryReadException(0x0004B404, "Memory session is not connected.")
        }

        val cardBattleLoader = CardBattleLoader(addressesRepository, CardBattleReader(memoryReader))

        assertThrows(MemoryReadException::class.java) { cardBattleLoader.load() }
    }
}
