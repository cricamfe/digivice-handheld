package dev.cricamfe.digivice.core.memory.resources

import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource

/** Port of `Backend/Memory/Resources/DigimonBattleResource.cs`. */
data class DigimonBattleResource(
    val field: UByte = 0u,
    val fieldAmount: Int = 0,
    val activeFighter: Int = 0,
    val stage: Int = 0,
    val enemy: EnemyResource = EnemyResource(),
)
