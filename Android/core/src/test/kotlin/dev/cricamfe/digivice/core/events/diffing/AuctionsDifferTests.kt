package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/AuctionsDifferTests.cs`. */
class AuctionsDifferTests {
    private fun baseAuctions() = Auctions(
        divineBarrier = false,
        hazardShield = false,
        sniperShield = false,
        dramonShield = false,
        yinYangWand = false,
        hazardFang = false,
    )

    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val result = AuctionsDiffer.diff(baseAuctions(), baseAuctions())

        assertAbsent(result.divineBarrier)
        assertAbsent(result.hazardShield)
        assertAbsent(result.sniperShield)
        assertAbsent(result.dramonShield)
        assertAbsent(result.yinYangWand)
        assertAbsent(result.hazardFang)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val current = baseAuctions().also { it.divineBarrier = true }

        val result = AuctionsDiffer.diff(null, current)

        assertPresent(true, result.divineBarrier)
        assertPresent(false, result.hazardShield)
    }

    @Test
    fun `diff returns the divine barrier delta when only the divine barrier changed`() {
        val current = baseAuctions().also { it.divineBarrier = true }

        val result = AuctionsDiffer.diff(baseAuctions(), current)

        assertPresent(true, result.divineBarrier)
        assertAbsent(result.hazardShield)
    }

    @Test
    fun `diff returns the hazard fang delta when only the hazard fang changed`() {
        val current = baseAuctions().also { it.hazardFang = true }

        val result = AuctionsDiffer.diff(baseAuctions(), current)

        assertPresent(true, result.hazardFang)
        assertAbsent(result.yinYangWand)
        assertAbsent(result.divineBarrier)
    }
}
