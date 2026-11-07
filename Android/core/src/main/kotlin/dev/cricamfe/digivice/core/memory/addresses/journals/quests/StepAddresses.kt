package dev.cricamfe.digivice.core.memory.addresses.journals.quests

import dev.cricamfe.digivice.core.memory.converters.HexStringListToLongListConverter
import dev.cricamfe.digivice.core.memory.converters.HexStringToLongConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Journals/Quests/StepAddresses.cs`. */
@Serializable
data class StepAddresses(
    @SerialName("Number")
    val number: Int = 0,
    @SerialName("Address")
    @Serializable(with = HexStringToLongConverter::class)
    val address: Long = 0L,
    @SerialName("BitMasks")
    @Serializable(with = HexStringListToLongListConverter::class)
    val bitMasks: List<Long> = emptyList(),
    /** Other marks of the same step, for a step the game can reach in more than one way: any of them makes it done. */
    @SerialName("Alternatives")
    val alternatives: List<StepAlternative> = emptyList(),
    /** Set for a byte that counts up, like the story's stage: the step is done once the byte reaches this. */
    @SerialName("AtLeast")
    val atLeast: Int? = null,
    @SerialName("Requisites")
    val requisites: List<RequisiteAddresses> = emptyList(),
)

/** Another address that marks a step as done; see [StepAddresses.alternatives]. */
@Serializable
data class StepAlternative(
    @SerialName("Address")
    @Serializable(with = HexStringToLongConverter::class)
    val address: Long = 0L,
    @SerialName("BitMasks")
    @Serializable(with = HexStringListToLongListConverter::class)
    val bitMasks: List<Long> = emptyList(),
)
