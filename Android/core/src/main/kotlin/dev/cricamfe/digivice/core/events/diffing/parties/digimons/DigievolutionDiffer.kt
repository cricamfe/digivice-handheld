package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.events.converters.parties.digimons.DigievolutionConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.DigievolutionDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/DigievolutionDiffer.cs`. Only reads the digievolutions. */
object DigievolutionDiffer {
    fun diff(previousDigievolution: Digievolution?, newDigievolution: Digievolution?): DigievolutionDto? {
        if (newDigievolution.hasNoChanges(previousDigievolution)) return null
        if (previousDigievolution == null) return DigievolutionConverter.toDto(newDigievolution)

        return DigievolutionDto(
            level = changed(previousDigievolution.level, newDigievolution.level),
            dvexp = changed(previousDigievolution.dvexp, newDigievolution.dvexp),
        )
    }
}
