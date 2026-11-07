package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.events.dto.AuctionsDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/AuctionsConverter.cs`. Reads the domain model, never changes it. */
object AuctionsConverter {
    fun toDto(auctions: Auctions): AuctionsDto = AuctionsDto(
        divineBarrier = Optional.Present(auctions.divineBarrier),
        hazardShield = Optional.Present(auctions.hazardShield),
        sniperShield = Optional.Present(auctions.sniperShield),
        dramonShield = Optional.Present(auctions.dramonShield),
        yinYangWand = Optional.Present(auctions.yinYangWand),
        hazardFang = Optional.Present(auctions.hazardFang),
    )
}
