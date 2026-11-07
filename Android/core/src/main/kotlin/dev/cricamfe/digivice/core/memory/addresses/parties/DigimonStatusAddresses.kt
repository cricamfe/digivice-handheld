package dev.cricamfe.digivice.core.memory.addresses.parties

import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.AttributesAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.ElementsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.EquipmentsAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.VitalAddresses
import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/DigimonStatusAddresses.cs`. */
@Serializable
data class DigimonStatusAddresses(
    @SerialName("Experience")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val experience: Int = 0,
    @SerialName("Level")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val level: Int = 0,
    @SerialName("TP")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val tp: Int = 0,
    @SerialName("HP")
    val hp: VitalAddresses = VitalAddresses(),
    @SerialName("MP")
    val mp: VitalAddresses = VitalAddresses(),
    @SerialName("Attributes")
    val attributes: AttributesAddresses = AttributesAddresses(),
    @SerialName("Elements")
    val elements: ElementsAddresses = ElementsAddresses(),
    @SerialName("Equipments")
    val equipments: EquipmentsAddresses = EquipmentsAddresses(),
    @SerialName("Digievolutions")
    val digievolutions: DigievolutionsAddresses = DigievolutionsAddresses(),
)
