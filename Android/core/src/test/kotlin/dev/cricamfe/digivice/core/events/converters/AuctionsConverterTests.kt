package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Auctions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/AuctionsConverterTests.cs`. */
class AuctionsConverterTests {
    @Test
    fun `toDto maps all auction fields`() {
        val auctions = Auctions(
            divineBarrier = true,
            hazardShield = false,
            sniperShield = true,
            dramonShield = false,
            yinYangWand = true,
            hazardFang = true,
        )

        val dto = AuctionsConverter.toDto(auctions)

        assertTrue(dto.divineBarrier.hasValue)
        assertTrue(dto.divineBarrier.value)
        assertTrue(dto.hazardShield.hasValue)
        assertFalse(dto.hazardShield.value)
        assertTrue(dto.sniperShield.hasValue)
        assertTrue(dto.sniperShield.value)
        assertTrue(dto.dramonShield.hasValue)
        assertFalse(dto.dramonShield.value)
        assertTrue(dto.yinYangWand.hasValue)
        assertTrue(dto.yinYangWand.value)
        assertTrue(dto.hazardFang.hasValue)
        assertTrue(dto.hazardFang.value)
    }
}
