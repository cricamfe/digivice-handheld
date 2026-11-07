package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/VitalConverter.cs`. Reads the domain model, never changes it. */
object VitalConverter {
    fun toDto(vital: Vital): VitalDto = VitalDto(
        current = Optional.Present(vital.current),
        max = Optional.Present(vital.max),
    )
}
