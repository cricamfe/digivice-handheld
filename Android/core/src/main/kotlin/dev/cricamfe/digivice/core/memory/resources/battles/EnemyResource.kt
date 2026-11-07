package dev.cricamfe.digivice.core.memory.resources.battles

import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Resources/Battles/EnemyResource.cs`. */
data class EnemyResource(
    val id: Int = 0,
    val groupId: Int = 0,
    val condition: Int = 0,
    val strength: Int = 0,
    val defense: Int = 0,
    val speed: Int = 0,
    val hp: VitalResource = VitalResource(),
)
