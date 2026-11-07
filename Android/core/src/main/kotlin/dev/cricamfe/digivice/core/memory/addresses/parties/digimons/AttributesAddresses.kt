package dev.cricamfe.digivice.core.memory.addresses.parties.digimons

import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/Digimons/AttributesAddresses.cs`. */
@Serializable
data class AttributesAddresses(
    @SerialName("Strength")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val strength: Int = 0,
    @SerialName("Defense")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val defense: Int = 0,
    @SerialName("Spirit")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val spirit: Int = 0,
    @SerialName("Wisdom")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val wisdom: Int = 0,
    @SerialName("Speed")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val speed: Int = 0,
    @SerialName("Charisma")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val charisma: Int = 0,
)
