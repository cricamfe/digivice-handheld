package dev.cricamfe.digivice.core.memory.addresses.parties.digimons

import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/Digimons/ElementsAddresses.cs`. */
@Serializable
data class ElementsAddresses(
    @SerialName("Fire")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val fire: Int = 0,
    @SerialName("Water")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val water: Int = 0,
    @SerialName("Ice")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val ice: Int = 0,
    @SerialName("Wind")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val wind: Int = 0,
    @SerialName("Thunder")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val thunder: Int = 0,
    @SerialName("Machine")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val machine: Int = 0,
    @SerialName("Dark")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val dark: Int = 0,
)
