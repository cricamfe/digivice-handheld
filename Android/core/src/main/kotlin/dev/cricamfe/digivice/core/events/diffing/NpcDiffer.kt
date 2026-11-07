package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.events.converters.NpcsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.npcs.NpcDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/NpcDiffer.cs`. Only reads the npcs. */
object NpcDiffer {
    fun diff(previousNpc: Npc?, newNpc: Npc): NpcDto? {
        if (newNpc.hasNoChanges(previousNpc)) return null
        if (previousNpc == null) return NpcsConverter.toNpcDto(newNpc)

        val battlesDelta = newNpc.battles.mapNotNull { newBattle ->
            NpcBattleDiffer.diff(previousNpc.battles.firstOrNull { it.id == newBattle.id }, newBattle)
        }
        if (battlesDelta.isEmpty()) return null

        return NpcDto(battles = Optional.Present(battlesDelta))
    }
}
