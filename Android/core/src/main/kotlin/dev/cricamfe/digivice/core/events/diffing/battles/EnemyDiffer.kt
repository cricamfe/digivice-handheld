package dev.cricamfe.digivice.core.events.diffing.battles

import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.events.converters.battles.EnemyConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.extensions.nested
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.VitalDiffer
import dev.cricamfe.digivice.core.events.dto.battles.EnemyDto

/** Port of `Backend/Events/Diffing/Battles/EnemyDiffer.cs`. Only reads the enemies. */
object EnemyDiffer {
    fun diff(previousEnemy: Enemy?, newEnemy: Enemy): EnemyDto? {
        if (newEnemy.hasNoChanges(previousEnemy)) return null
        if (previousEnemy == null) return EnemyConverter.toDto(newEnemy)

        val hpDelta = VitalDiffer.diff(previousEnemy.hp, newEnemy.hp)
        val dto = EnemyDto(
            id = changed(previousEnemy.id, newEnemy.id),
            groupId = changed(previousEnemy.groupId, newEnemy.groupId),
            condition = changed(previousEnemy.condition, newEnemy.condition),
            strength = changed(previousEnemy.strength, newEnemy.strength),
            defense = changed(previousEnemy.defense, newEnemy.defense),
            speed = changed(previousEnemy.speed, newEnemy.speed),
            hp = nested(hpDelta),
        )

        // The C# returned null when none of the seven fields changed: here, when no field of the dto was set.
        return if (dto == EnemyDto()) null else dto
    }
}
