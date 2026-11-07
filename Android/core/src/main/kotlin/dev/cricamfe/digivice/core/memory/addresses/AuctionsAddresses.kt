package dev.cricamfe.digivice.core.memory.addresses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/AuctionsAddresses.cs`. */
@Serializable
data class AuctionsAddresses(
    @SerialName("DivineBarrier")
    val divineBarrier: AuctionAddresses = AuctionAddresses(),
    @SerialName("HazardShield")
    val hazardShield: AuctionAddresses = AuctionAddresses(),
    @SerialName("SniperShield")
    val sniperShield: AuctionAddresses = AuctionAddresses(),
    @SerialName("DramonShield")
    val dramonShield: AuctionAddresses = AuctionAddresses(),
    @SerialName("YinYangWand")
    val yinYangWand: AuctionAddresses = AuctionAddresses(),
    @SerialName("HazardFang")
    val hazardFang: AuctionAddresses = AuctionAddresses(),
)
