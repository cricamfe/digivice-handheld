package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.events.converters.parties.digimons.ElementsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.ElementsDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/ElementsDiffer.cs`. Only reads the elements. */
object ElementsDiffer {
    fun diff(previousElements: Elements?, newElements: Elements): ElementsDto? {
        if (newElements.hasNoChanges(previousElements)) return null
        if (previousElements == null) return ElementsConverter.toDto(newElements)

        return ElementsDto(
            fire = changed(previousElements.fire, newElements.fire),
            water = changed(previousElements.water, newElements.water),
            ice = changed(previousElements.ice, newElements.ice),
            wind = changed(previousElements.wind, newElements.wind),
            thunder = changed(previousElements.thunder, newElements.thunder),
            machine = changed(previousElements.machine, newElements.machine),
            dark = changed(previousElements.dark, newElements.dark),
        )
    }
}
