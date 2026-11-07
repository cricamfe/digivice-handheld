package dev.cricamfe.digivice.core.memory.addresses.parties

import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/DigimonAddress.cs`. */
@Serializable
data class DigimonAddress(
    @SerialName("Name")
    val name: String = "",
    @SerialName("MemoryBlockAddress")
    @Serializable(with = HexStringToLongConverter::class)
    val memoryBlockAddress: Long = 0L,
    @SerialName("BlastAddress")
    @Serializable(with = HexStringToLongConverter::class)
    val blastAddress: Long = 0L,
)
