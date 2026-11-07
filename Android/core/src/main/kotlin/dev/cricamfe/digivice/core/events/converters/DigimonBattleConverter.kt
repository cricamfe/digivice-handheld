package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.events.converters.battles.EnemyConverter
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/DigimonBattleConverter.cs`. Reads the domain model, never changes it. */
object DigimonBattleConverter {
    fun toDto(digimonBattle: DigimonBattle): DigimonBattleDto = DigimonBattleDto(
        field = Optional.Present(digimonBattle.field),
        fieldAmount = Optional.Present(digimonBattle.fieldAmount),
        activeFighter = Optional.Present(digimonBattle.activeFighter),
        stage = Optional.Present(digimonBattle.stage),
        enemy = Optional.Present(digimonBattle.enemy?.let(EnemyConverter::toDto)),
    )
}
