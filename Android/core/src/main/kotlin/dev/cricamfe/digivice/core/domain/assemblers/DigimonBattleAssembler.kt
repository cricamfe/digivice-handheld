package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource

/** Port of `Backend/Domain/Assemblers/DigimonBattleAssembler.cs`. */
object DigimonBattleAssembler {
    fun assemble(resource: DigimonBattleResource): DigimonBattle = DigimonBattle(
        field = resource.field,
        fieldAmount = resource.fieldAmount,
        activeFighter = resource.activeFighter,
        stage = resource.stage,
        enemy = if (resource.enemy.id <= 0) null else Enemy(
            id = resource.enemy.id,
            groupId = resource.enemy.groupId,
            condition = resource.enemy.condition,
            strength = resource.enemy.strength,
            defense = resource.enemy.defense,
            speed = resource.enemy.speed,
            hp = Vital(
                current = resource.enemy.hp.current,
                max = resource.enemy.hp.max,
            ),
        ),
    )
}
