package dev.cricamfe.digivice.core.events.converters.parties

import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.events.dto.parties.DigimonSlotDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/DigimonSlotConverter.cs`. Reads the domain model, never changes it. */
object DigimonSlotConverter {
    fun toDto(digimonSlot: DigimonSlot): DigimonSlotDto = DigimonSlotDto(
        index = digimonSlot.index,
        digimonId = Optional.Present(digimonSlot.digimonId),
        digimon = Optional.Present(digimonSlot.digimon?.let(DigimonConverter::toDto)),
    )
}
