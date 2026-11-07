package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.converters.parties.digimons.VitalConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/VitalDiffer.cs`. Only reads the vitals. */
object VitalDiffer {
    fun diff(previousVital: Vital?, newVital: Vital): VitalDto? {
        if (newVital.hasNoChanges(previousVital)) return null
        if (previousVital == null) return VitalConverter.toDto(newVital)

        return VitalDto(
            current = changed(previousVital.current, newVital.current),
            max = changed(previousVital.max, newVital.max),
        )
    }
}
