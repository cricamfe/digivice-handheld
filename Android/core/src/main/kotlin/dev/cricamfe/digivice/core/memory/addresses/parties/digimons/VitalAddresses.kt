package dev.cricamfe.digivice.core.memory.addresses.parties.digimons

import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/Digimons/VitalAddresses.cs`. */
@Serializable
data class VitalAddresses(
    @SerialName("Current")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val current: Int = 0,
    @SerialName("Max")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val max: Int = 0,
)
