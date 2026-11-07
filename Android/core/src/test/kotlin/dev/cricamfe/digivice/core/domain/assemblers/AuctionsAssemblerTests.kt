package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.memory.resources.AuctionsResource
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuctionsAssemblerTests {
    @Test
    fun `assemble should map non zero flags to true`() {
        val resource = AuctionsResource(
            divineBarrier = 0x01u,
            hazardShield = 0x00u,
            sniperShield = 0x04u,
            dramonShield = 0x00u,
            yinYangWand = 0x10u,
            hazardFang = 0x20u,
        )

        val result = AuctionsAssembler.assemble(resource)

        assertTrue(result.divineBarrier)
        assertFalse(result.hazardShield)
        assertTrue(result.sniperShield)
        assertFalse(result.dramonShield)
        assertTrue(result.yinYangWand)
        assertTrue(result.hazardFang)
    }
}
