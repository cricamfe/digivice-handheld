package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeAuctionsLoader
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuctionsProviderTests {
    @Test
    fun `get should load resource and apply auctions assembler`() {
        val resource = AuctionsResource(
            divineBarrier = 0x01u,
            hazardShield = 0x00u,
            sniperShield = 0x04u,
            dramonShield = 0x00u,
            yinYangWand = 0x10u,
            hazardFang = 0x20u,
        )
        val loader = FakeAuctionsLoader(resource)

        val result = AuctionsProvider(loader).get()

        assertTrue(result.divineBarrier)
        assertFalse(result.hazardShield)
        assertTrue(result.sniperShield)
        assertFalse(result.dramonShield)
        assertTrue(result.yinYangWand)
        assertTrue(result.hazardFang)
        assertEquals(1, loader.calls)
    }
}
