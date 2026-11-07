package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.events.converters.PartyConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changedItems
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.parties.DigimonSlotDiffer
import dev.cricamfe.digivice.core.events.dto.PartyDto

/** Port of `Backend/Events/Diffing/PartyDiffer.cs`. Only reads the parties. */
object PartyDiffer {
    fun diff(previousParty: Party?, newParty: Party): PartyDto {
        if (newParty.hasNoChanges(previousParty)) return PartyDto()
        if (previousParty == null) return PartyConverter.toDto(newParty)

        return PartyDto(
            slots = changedItems(
                newParty.slots.mapNotNull { newSlot ->
                    DigimonSlotDiffer.diff(previousParty.slots.firstOrNull { it.index == newSlot.index }, newSlot)
                },
            ),
        )
    }
}
