package dev.cricamfe.digivice.core.memory.resources.parties

import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Resources/Parties/InBattleResource.cs`. */
data class InBattleResource(
    val formId: Int = 0,
    val condition: Int = 0,
    val strength: Int = 0,
    val defense: Int = 0,
    val speed: Int = 0,
    val hp: VitalResource = VitalResource(),
    val mp: VitalResource = VitalResource(),
)
