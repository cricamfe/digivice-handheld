package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.events.dto.parties.digimons.DigievolutionSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/DigievolutionSlotConverter.cs`. Reads the domain model, never changes it. */
object DigievolutionSlotConverter {
    fun toDto(digievolutionSlot: DigievolutionSlot): DigievolutionSlotDto = DigievolutionSlotDto(
        index = digievolutionSlot.index,
        digievolutionId = Optional.Present(digievolutionSlot.digievolutionId),
        digievolution = Optional.Present(digievolutionSlot.digievolution?.let(DigievolutionConverter::toDto)),
    )
}
