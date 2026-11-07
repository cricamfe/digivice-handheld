package dev.cricamfe.digivice.core.memory.resources

import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource

/** Port of `Backend/Memory/Resources/PartyResource.cs`. */
data class PartyResource(
    val slotsResource: List<DigimonSlotResource> = emptyList(),
)
