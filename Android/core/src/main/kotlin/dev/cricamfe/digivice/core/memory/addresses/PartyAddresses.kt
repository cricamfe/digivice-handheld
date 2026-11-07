package dev.cricamfe.digivice.core.memory.addresses

import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/PartyAddresses.cs`. */
@Serializable
data class PartyAddresses(
    @SerialName("Slots")
    val slots: List<SlotAddresses> = emptyList(),
    @SerialName("EmptySlotId")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val emptySlotId: Int = 0,
)
