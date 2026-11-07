package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.events.dto.parties.digimons.DigievolutionDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/DigievolutionConverter.cs`. Reads the domain model, never changes it. */
object DigievolutionConverter {
    fun toDto(digievolution: Digievolution): DigievolutionDto = DigievolutionDto(
        level = Optional.Present(digievolution.level),
        dvexp = Optional.Present(digievolution.dvexp),
    )
}
