package dev.cricamfe.digivice.core.memory.addresses.journals.quests

import dev.cricamfe.digivice.core.memory.converters.HexStringListToLongListConverter
import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Journals/Quests/RequisiteAddresses.cs`. */
@Serializable
data class RequisiteAddresses(
    @SerialName("Id")
    val id: String = "",
    @SerialName("Address")
    @Serializable(with = HexStringToLongConverter::class)
    val address: Long = 0L,
    @SerialName("BitMasks")
    @Serializable(with = HexStringListToLongListConverter::class)
    val bitMasks: List<Long> = emptyList(),
)
