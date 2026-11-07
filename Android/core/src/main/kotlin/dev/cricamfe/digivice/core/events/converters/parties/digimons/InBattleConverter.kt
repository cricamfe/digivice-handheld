package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.events.dto.parties.digimons.InBattleDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/InBattleConverter.cs`. Reads the domain model, never changes it. */
object InBattleConverter {
    fun toDto(inBattle: InBattle): InBattleDto = InBattleDto(
        formId = Optional.Present(inBattle.formId),
        condition = Optional.Present(inBattle.condition),
        strength = Optional.Present(inBattle.strength),
        defense = Optional.Present(inBattle.defense),
        speed = Optional.Present(inBattle.speed),
        hp = Optional.Present(VitalConverter.toDto(inBattle.hp)),
        mp = Optional.Present(VitalConverter.toDto(inBattle.mp)),
    )
}
