package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.assemblers.parties.DigimonSlotAssembler
import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.memory.resources.PartyResource

/** Port of `Backend/Domain/Assemblers/PartyAssembler.cs`. */
object PartyAssembler {
    fun assemble(resource: PartyResource): Party = Party(
        slots = resource.slotsResource.map(DigimonSlotAssembler::assemble),
    )
}
