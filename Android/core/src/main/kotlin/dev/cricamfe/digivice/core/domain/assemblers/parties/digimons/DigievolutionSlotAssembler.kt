package dev.cricamfe.digivice.core.domain.assemblers.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Digievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource

/** Port of `Backend/Domain/Assemblers/Parties/Digimons/DigievolutionSlotAssembler.cs`. */
object DigievolutionSlotAssembler {
    fun assemble(
        resource: DigievolutionSlotResource,
        storedDigievolutions: List<StoredDigievolutionResource>,
    ): DigievolutionSlot {
        if (resource.digievolutionId <= 0) {
            return DigievolutionSlot(
                index = resource.index,
                digievolutionId = null,
                digievolution = null,
            )
        }

        val storedDigievolution = storedDigievolutions.firstOrNull { it.digievolutionId == resource.digievolutionId }

        return DigievolutionSlot(
            index = resource.index,
            digievolutionId = resource.digievolutionId,
            digievolution = Digievolution(
                level = storedDigievolution?.level ?: 1,
                dvexp = maxOf(0, storedDigievolution?.dvexp ?: 0),
            ),
        )
    }
}
