package dev.cricamfe.digivice.core.memory.addresses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Port of `Backend/Memory/Addresses/NpcAddresses.cs`. */
@Serializable
data class NpcAddresses(
    @SerialName("Battles")
    val battles: Map<String, NpcBattleAddresses> = emptyMap(),
)
