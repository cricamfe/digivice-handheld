package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.events.converters.parties.digimons.DigievolutionSlotConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.DigievolutionSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/Parties/Digimons/DigievolutionSlotDiffer.cs`. Only reads the slots. */
object DigievolutionSlotDiffer {
    fun diff(previousDigievolutionSlot: DigievolutionSlot?, newDigievolutionSlot: DigievolutionSlot): DigievolutionSlotDto? {
        if (newDigievolutionSlot.hasNoChanges(previousDigievolutionSlot)) return null
        if (previousDigievolutionSlot == null) return DigievolutionSlotConverter.toDto(newDigievolutionSlot)

        // Another digievolution in the slot (or the slot emptied): the whole slot, never a partial delta.
        if (previousDigievolutionSlot.digievolutionId != newDigievolutionSlot.digievolutionId) {
            return DigievolutionSlotConverter.toDto(newDigievolutionSlot)
        }

        val digievolutionDelta =
            DigievolutionDiffer.diff(previousDigievolutionSlot.digievolution, newDigievolutionSlot.digievolution) ?: return null

        return DigievolutionSlotDto(index = newDigievolutionSlot.index, digievolution = Optional.Present(digievolutionDelta))
    }
}
