package dev.cricamfe.digivice.core.domain.models

/** Port of `Backend/Domain/Models/Auctions.cs`. */
data class Auctions(
    var divineBarrier: Boolean = false,
    var hazardShield: Boolean = false,
    var sniperShield: Boolean = false,
    var dramonShield: Boolean = false,
    var yinYangWand: Boolean = false,
    var hazardFang: Boolean = false,
)
