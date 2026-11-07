package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.readers.AuctionsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuctionsLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate auctions addresses and reader`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B38AL] = 0x03u
        }

        val auctionsLoader = AuctionsLoader(addressesRepository, AuctionsReader(memoryReader))

        val resource = auctionsLoader.load()

        assertEquals(0x01.toUByte(), resource.divineBarrier)
        assertEquals(0x02.toUByte(), resource.hazardShield)
        assertEquals(0x00.toUByte(), resource.sniperShield)
        assertEquals(0x00.toUByte(), resource.dramonShield)
        assertEquals(0x00.toUByte(), resource.yinYangWand)
        assertEquals(0x00.toUByte(), resource.hazardFang)
    }
}
