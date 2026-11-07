package dev.cricamfe.digivice.core.memory.resources.parties

/** Port of `Backend/Memory/Resources/Parties/DigimonSlotResource.cs`. */
data class DigimonSlotResource(
    val index: Int = 0,
    val digimonId: Int = 0,
    val digimonResource: DigimonResource? = null,
)
