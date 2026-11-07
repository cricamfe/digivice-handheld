package dev.cricamfe.digivice.core.domain.assemblers.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource

/** Port of `Backend/Domain/Assemblers/Parties/Digimons/StoredDigievolutionAssembler.cs`. */
object StoredDigievolutionAssembler {
    fun assemble(resource: StoredDigievolutionResource): StoredDigievolution = StoredDigievolution(
        digievolutionId = resource.digievolutionId,
        level = resource.level,
    )
}
