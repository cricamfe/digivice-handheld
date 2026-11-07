package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.events.dto.parties.digimons.AttributesDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/AttributesConverter.cs`. Reads the domain model, never changes it. */
object AttributesConverter {
    fun toDto(attributes: Attributes): AttributesDto = AttributesDto(
        strength = Optional.Present(attributes.strength),
        defense = Optional.Present(attributes.defense),
        spirit = Optional.Present(attributes.spirit),
        wisdom = Optional.Present(attributes.wisdom),
        speed = Optional.Present(attributes.speed),
        charisma = Optional.Present(attributes.charisma),
    )
}
