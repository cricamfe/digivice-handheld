package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource

/** Port of `Backend/Domain/Assemblers/AuctionsAssembler.cs`. */
object AuctionsAssembler {
    fun assemble(resource: AuctionsResource): Auctions = Auctions(
        divineBarrier = resource.divineBarrier.toInt() != 0,
        hazardShield = resource.hazardShield.toInt() != 0,
        sniperShield = resource.sniperShield.toInt() != 0,
        dramonShield = resource.dramonShield.toInt() != 0,
        yinYangWand = resource.yinYangWand.toInt() != 0,
        hazardFang = resource.hazardFang.toInt() != 0,
    )
}
