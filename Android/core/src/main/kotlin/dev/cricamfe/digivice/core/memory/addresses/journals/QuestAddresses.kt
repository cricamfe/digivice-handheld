package dev.cricamfe.digivice.core.memory.addresses.journals

import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/Journals/QuestAddresses.cs`. */
@Serializable
data class QuestAddresses(
    @SerialName("Id")
    val id: String = "",
    @SerialName("Requisites")
    val requisites: List<RequisiteAddresses> = emptyList(),
    @SerialName("Steps")
    val steps: List<StepAddresses> = emptyList(),
)
