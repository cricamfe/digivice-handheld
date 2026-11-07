package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.events.converters.parties.digimons.InBattleConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.extensions.nested
import dev.cricamfe.digivice.core.events.dto.parties.digimons.InBattleDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/InBattleDiffer.cs`. Only reads the in-battle stats. */
object InBattleDiffer {
    fun diff(previousInBattle: InBattle?, newInBattle: InBattle): InBattleDto? {
        if (newInBattle.hasNoChanges(previousInBattle)) return null
        if (previousInBattle == null) return InBattleConverter.toDto(newInBattle)

        val dto = InBattleDto(
            formId = changed(previousInBattle.formId, newInBattle.formId),
            condition = changed(previousInBattle.condition, newInBattle.condition),
            strength = changed(previousInBattle.strength, newInBattle.strength),
            defense = changed(previousInBattle.defense, newInBattle.defense),
            speed = changed(previousInBattle.speed, newInBattle.speed),
            hp = nested(VitalDiffer.diff(previousInBattle.hp, newInBattle.hp)),
            mp = nested(VitalDiffer.diff(previousInBattle.mp, newInBattle.mp)),
        )

        // The C# returned null when none of the seven fields changed: here, when no field of the dto was set.
        return if (dto == InBattleDto()) null else dto
    }
}
