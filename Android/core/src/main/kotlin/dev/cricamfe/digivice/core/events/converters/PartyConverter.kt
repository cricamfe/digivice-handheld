package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.events.converters.parties.DigimonSlotConverter
import dev.cricamfe.digivice.core.events.dto.PartyDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/PartyConverter.cs`. Reads the domain model, never changes it. */
object PartyConverter {
    fun toDto(party: Party): PartyDto = PartyDto(
        slots = Optional.Present(party.slots.map(DigimonSlotConverter::toDto)),
    )
}
