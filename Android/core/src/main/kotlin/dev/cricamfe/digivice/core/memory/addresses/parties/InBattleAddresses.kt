package dev.cricamfe.digivice.core.memory.addresses.parties

import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.VitalAddresses
import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/InBattleAddresses.cs`. */
@Serializable
data class InBattleAddresses(
    @SerialName("AllySlotBase")
    @Serializable(with = HexStringToLongConverter::class)
    val allySlotBase: Long = 0L,
    @SerialName("SlotStride")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val slotStride: Int = 0,
    /** The Digimon the fighter is now (`BattleFighter.id`): its own, or the form it digivolved to. */
    @SerialName("Id")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val id: Int = 0,
    @SerialName("Condition")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val condition: Int = 0,
    @SerialName("Strength")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val strength: Int = 0,
    @SerialName("Defense")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val defense: Int = 0,
    @SerialName("Speed")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val speed: Int = 0,
    @SerialName("HP")
    val hp: VitalAddresses = VitalAddresses(),
    @SerialName("MP")
    val mp: VitalAddresses = VitalAddresses(),
)
