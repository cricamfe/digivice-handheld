package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/ImportantItemsAddresses.cs`. */
@Serializable
data class ImportantItemsAddresses(
    @SerialName("TreeBoots")
    @Serializable(with = HexStringToLongConverter::class)
    val treeBoots: Long = 0L,
    @SerialName("FishingPole")
    @Serializable(with = HexStringToLongConverter::class)
    val fishingPole: Long = 0L,
    @SerialName("AsukaTrophy")
    @Serializable(with = HexStringToLongConverter::class)
    val asukaTrophy: Long = 0L,
    @SerialName("SunTrophy")
    @Serializable(with = HexStringToLongConverter::class)
    val sunTrophy: Long = 0L,
)
