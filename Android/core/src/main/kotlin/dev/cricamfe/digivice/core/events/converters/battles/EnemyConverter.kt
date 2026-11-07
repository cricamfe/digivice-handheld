package dev.cricamfe.digivice.core.events.converters.battles

import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.events.converters.parties.digimons.VitalConverter
import dev.cricamfe.digivice.core.events.dto.battles.EnemyDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Battles/EnemyConverter.cs`. Reads the domain model, never changes it. */
object EnemyConverter {
    fun toDto(enemy: Enemy): EnemyDto = EnemyDto(
        id = Optional.Present(enemy.id),
        groupId = Optional.Present(enemy.groupId),
        condition = Optional.Present(enemy.condition),
        strength = Optional.Present(enemy.strength),
        defense = Optional.Present(enemy.defense),
        speed = Optional.Present(enemy.speed),
        hp = Optional.Present(VitalConverter.toDto(enemy.hp)),
    )
}
