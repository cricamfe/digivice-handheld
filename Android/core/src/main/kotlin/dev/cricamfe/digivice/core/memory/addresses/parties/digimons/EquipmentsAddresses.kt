package dev.cricamfe.digivice.core.memory.addresses.parties.digimons

import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/Digimons/EquipmentsAddresses.cs`. */
@Serializable
data class EquipmentsAddresses(
    @SerialName("Head")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val head: Int = 0,
    @SerialName("Body")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val body: Int = 0,
    @SerialName("Right")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val right: Int = 0,
    @SerialName("Left")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val left: Int = 0,
    @SerialName("Accessory1")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val accessory1: Int = 0,
    @SerialName("Accessory2")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val accessory2: Int = 0,
)
