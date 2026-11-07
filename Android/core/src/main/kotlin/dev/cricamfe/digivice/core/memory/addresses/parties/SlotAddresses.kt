package dev.cricamfe.digivice.core.memory.addresses.parties

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/SlotAddresses.cs`. */
@Serializable
data class SlotAddresses(
    @SerialName("Index")
    val index: Int = 0,
    @SerialName("Address")
    @Serializable(with = HexStringToLongConverter::class)
    val address: Long = 0L,
)
