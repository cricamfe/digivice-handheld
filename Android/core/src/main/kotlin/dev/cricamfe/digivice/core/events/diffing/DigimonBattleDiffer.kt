package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.events.converters.DigimonBattleConverter
import dev.cricamfe.digivice.core.events.diffing.battles.EnemyDiffer
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.dto.battles.EnemyDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/DigimonBattleDiffer.cs`. Only reads the battles. */
object DigimonBattleDiffer {
    fun diff(previousDigimonBattle: DigimonBattle?, newDigimonBattle: DigimonBattle): DigimonBattleDto {
        if (newDigimonBattle.hasNoChanges(previousDigimonBattle)) return DigimonBattleDto()
        if (previousDigimonBattle == null) return DigimonBattleConverter.toDto(newDigimonBattle)

        return DigimonBattleDto(
            field = changed(previousDigimonBattle.field, newDigimonBattle.field),
            fieldAmount = changed(previousDigimonBattle.fieldAmount, newDigimonBattle.fieldAmount),
            activeFighter = changed(previousDigimonBattle.activeFighter, newDigimonBattle.activeFighter),
            stage = changed(previousDigimonBattle.stage, newDigimonBattle.stage),
            enemy = diffEnemy(previousDigimonBattle, newDigimonBattle),
        )
    }

    /** Absent when there is no change, `Present(null)` when the enemy is gone, the delta (or the whole enemy) otherwise. */
    private fun diffEnemy(previousDigimonBattle: DigimonBattle, newDigimonBattle: DigimonBattle): Optional<EnemyDto?> {
        val newEnemy = newDigimonBattle.enemy
            ?: return if (previousDigimonBattle.enemy == null) Optional.Empty else Optional.Present(null)

        val enemyDelta = EnemyDiffer.diff(previousDigimonBattle.enemy, newEnemy) ?: return Optional.Empty
        return Optional.Present(enemyDelta)
    }
}
