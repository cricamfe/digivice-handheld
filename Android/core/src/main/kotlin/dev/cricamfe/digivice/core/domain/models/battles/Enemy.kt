package dev.cricamfe.digivice.core.domain.models.battles

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital

/** Port of `Backend/Domain/Models/Battles/Enemy.cs`. */
data class Enemy(
    var id: Int = 0,
    var groupId: Int = 0,
    var condition: Int = 0,
    var strength: Int = 0,
    var defense: Int = 0,
    var speed: Int = 0,
    var hp: Vital = Vital(),
)
