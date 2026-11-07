package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/NpcBattleAddresses.cs`. */
@Serializable
data class NpcBattleAddresses(
    @SerialName("Address")
    @Serializable(with = HexStringToLongConverter::class)
    val address: Long = 0L,
    @SerialName("BitMask")
    @Serializable(with = HexStringToLongConverter::class)
    val bitMask: Long = 0L,
)
