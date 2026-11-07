package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.events.converters.AuctionsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.AuctionsDto

/** Port of `Backend/Events/Diffing/AuctionsDiffer.cs`. Only reads the auctions. */
object AuctionsDiffer {
    fun diff(previousAuctions: Auctions?, newAuctions: Auctions): AuctionsDto {
        if (newAuctions.hasNoChanges(previousAuctions)) return AuctionsDto()
        if (previousAuctions == null) return AuctionsConverter.toDto(newAuctions)

        return AuctionsDto(
            divineBarrier = changed(previousAuctions.divineBarrier, newAuctions.divineBarrier),
            hazardShield = changed(previousAuctions.hazardShield, newAuctions.hazardShield),
            sniperShield = changed(previousAuctions.sniperShield, newAuctions.sniperShield),
            dramonShield = changed(previousAuctions.dramonShield, newAuctions.dramonShield),
            yinYangWand = changed(previousAuctions.yinYangWand, newAuctions.yinYangWand),
            hazardFang = changed(previousAuctions.hazardFang, newAuctions.hazardFang),
        )
    }
}
