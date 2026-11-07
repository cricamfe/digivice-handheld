package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/CardBattleAddresses.cs`. */
@Serializable
data class CardBattleAddresses(
    @SerialName("Id")
    @Serializable(with = HexStringToLongConverter::class)
    val id: Long = 0L,
)
