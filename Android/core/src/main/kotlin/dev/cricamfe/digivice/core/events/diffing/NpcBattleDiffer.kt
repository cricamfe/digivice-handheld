package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.events.converters.NpcsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.npcs.NpcBattleDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/NpcBattleDiffer.cs`. Only reads the battles. */
object NpcBattleDiffer {
    fun diff(previousBattle: NpcBattle?, newBattle: NpcBattle): NpcBattleDto? {
        if (newBattle.hasNoChanges(previousBattle)) return null
        if (previousBattle == null) return NpcsConverter.toBattleDto(newBattle)
        if (previousBattle.won == newBattle.won) return null

        return NpcBattleDto(id = newBattle.id, won = Optional.Present(newBattle.won))
    }
}
