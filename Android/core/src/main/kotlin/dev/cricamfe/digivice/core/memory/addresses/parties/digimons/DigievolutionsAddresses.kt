package dev.cricamfe.digivice.core.memory.addresses.parties.digimons

import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.converters.HexOrIntStringToIntConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Parties/Digimons/DigievolutionsAddresses.cs`. */
@Serializable
data class DigievolutionsAddresses(
    @SerialName("ActiveDigievolution")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val activeDigievolution: Int = 0,
    @SerialName("Slots")
    val slots: List<SlotAddresses> = emptyList(),
    @SerialName("UnlockedDigievolutionsStart")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val unlockedDigievolutionsStart: Int = 0,
    @SerialName("UnlockedDigievolutionEntryStride")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val unlockedDigievolutionEntryStride: Int = 0,
    @SerialName("MaxUnlockedDigievolutions")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val maxUnlockedDigievolutions: Int = 0,
    @SerialName("Id")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val id: Int = 0,
    @SerialName("Level")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val level: Int = 0,
    @SerialName("Dvexp")
    @Serializable(with = HexOrIntStringToIntConverter::class)
    val dvexp: Int = 0,
)
