package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.events.dto.parties.digimons.ElementsDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/ElementsConverter.cs`. Reads the domain model, never changes it. */
object ElementsConverter {
    fun toDto(elements: Elements): ElementsDto = ElementsDto(
        fire = Optional.Present(elements.fire),
        water = Optional.Present(elements.water),
        ice = Optional.Present(elements.ice),
        wind = Optional.Present(elements.wind),
        thunder = Optional.Present(elements.thunder),
        machine = Optional.Present(elements.machine),
        dark = Optional.Present(elements.dark),
    )
}
