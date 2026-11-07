package dev.cricamfe.digivice.core.events.diffing.parties

import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.events.converters.parties.DigimonSlotConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.DigimonSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/Parties/DigimonSlotDiffer.cs`. Only reads the slots. */
object DigimonSlotDiffer {
    fun diff(previousDigimonSlot: DigimonSlot?, newDigimonSlot: DigimonSlot): DigimonSlotDto? {
        if (newDigimonSlot.hasNoChanges(previousDigimonSlot)) return null
        if (previousDigimonSlot == null) return DigimonSlotConverter.toDto(newDigimonSlot)

        if (previousDigimonSlot.digimonId != newDigimonSlot.digimonId) {
            // The slot was emptied (explicit nulls) or holds another digimon (the whole slot): never a partial delta.
            return if (newDigimonSlot.digimon == null) {
                DigimonSlotDto(
                    index = newDigimonSlot.index,
                    digimonId = Optional.Present(null),
                    digimon = Optional.Present(null),
                )
            } else {
                DigimonSlotConverter.toDto(newDigimonSlot)
            }
        }

        val digimonDelta = DigimonDiffer.diff(previousDigimonSlot.digimon, newDigimonSlot.digimon) ?: return null

        return DigimonSlotDto(
            index = newDigimonSlot.index,
            digimonId = Optional.Present(newDigimonSlot.digimonId),
            digimon = Optional.Present(digimonDelta),
        )
    }
}
