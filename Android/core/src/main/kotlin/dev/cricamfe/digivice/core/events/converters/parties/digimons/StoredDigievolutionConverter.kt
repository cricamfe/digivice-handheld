package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.events.dto.parties.digimons.StoredDigievolutionDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/StoredDigievolutionConverter.cs`. Reads the domain model, never changes it. */
object StoredDigievolutionConverter {
    fun toDto(storedDigievolution: StoredDigievolution): StoredDigievolutionDto = StoredDigievolutionDto(
        digievolutionId = Optional.Present(storedDigievolution.digievolutionId),
        level = Optional.Present(storedDigievolution.level),
    )
}
