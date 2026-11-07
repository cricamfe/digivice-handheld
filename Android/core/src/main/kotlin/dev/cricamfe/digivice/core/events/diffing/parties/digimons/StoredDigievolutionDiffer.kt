package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.events.converters.parties.digimons.StoredDigievolutionConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.StoredDigievolutionDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Diffing/Parties/Digimons/StoredDigievolutionDiffer.cs`. Only reads the stored digievolutions. */
object StoredDigievolutionDiffer {
    fun diff(previousStoredDigievolution: StoredDigievolution?, newStoredDigievolution: StoredDigievolution): StoredDigievolutionDto? {
        if (newStoredDigievolution.hasNoChanges(previousStoredDigievolution)) return null
        if (previousStoredDigievolution == null) return StoredDigievolutionConverter.toDto(newStoredDigievolution)

        // The id is the key it was matched by, so it always goes out; the level only when it changed.
        return StoredDigievolutionDto(
            digievolutionId = Optional.Present(newStoredDigievolution.digievolutionId),
            level = changed(previousStoredDigievolution.level, newStoredDigievolution.level),
        )
    }
}
