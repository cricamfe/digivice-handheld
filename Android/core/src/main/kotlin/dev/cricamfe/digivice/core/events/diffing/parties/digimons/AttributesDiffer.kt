package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.events.converters.parties.digimons.AttributesConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.AttributesDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/AttributesDiffer.cs`. Only reads the attributes. */
object AttributesDiffer {
    fun diff(previousAttributes: Attributes?, newAttributes: Attributes): AttributesDto? {
        if (newAttributes.hasNoChanges(previousAttributes)) return null
        if (previousAttributes == null) return AttributesConverter.toDto(newAttributes)

        return AttributesDto(
            strength = changed(previousAttributes.strength, newAttributes.strength),
            defense = changed(previousAttributes.defense, newAttributes.defense),
            spirit = changed(previousAttributes.spirit, newAttributes.spirit),
            wisdom = changed(previousAttributes.wisdom, newAttributes.wisdom),
            speed = changed(previousAttributes.speed, newAttributes.speed),
            charisma = changed(previousAttributes.charisma, newAttributes.charisma),
        )
    }
}
