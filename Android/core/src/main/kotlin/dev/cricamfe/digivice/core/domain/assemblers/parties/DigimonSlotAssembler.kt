package dev.cricamfe.digivice.core.domain.assemblers.parties

import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource

/** Port of `Backend/Domain/Assemblers/Parties/DigimonSlotAssembler.cs`. */
object DigimonSlotAssembler {
    fun assemble(resource: DigimonSlotResource): DigimonSlot {
        val digimonResource = resource.digimonResource
        return DigimonSlot(
            index = resource.index,
            digimonId = if (digimonResource != null) resource.digimonId else null,
            digimon = if (digimonResource != null) DigimonAssembler.assemble(digimonResource) else null,
        )
    }
}
