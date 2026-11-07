package dev.cricamfe.digivice.core.domain.models

import dev.cricamfe.digivice.core.domain.models.battles.Enemy

/** Port of `Backend/Domain/Models/DigimonBattle.cs`; C# `byte` is [UByte]. */
data class DigimonBattle(
    var field: UByte = 0u,
    /** How much the field boosts its element, in 128ths. */
    var fieldAmount: Int = 0,
    /** The player's fighter in the field, 0 to 2. */
    var activeFighter: Int = 0,
    /** The fight stage the battle started on: the base one, a script can show others for a while. */
    var stage: Int = 0,
    var enemy: Enemy? = null,
)
