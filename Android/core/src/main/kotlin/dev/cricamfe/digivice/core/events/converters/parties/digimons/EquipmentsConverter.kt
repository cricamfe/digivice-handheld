package dev.cricamfe.digivice.core.events.converters.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.events.dto.parties.digimons.EquipmentsDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/Digimons/EquipmentsConverter.cs`. Reads the domain model, never changes it. */
object EquipmentsConverter {
    fun toDto(equipments: Equipments): EquipmentsDto = EquipmentsDto(
        head = Optional.Present(equipments.head),
        body = Optional.Present(equipments.body),
        right = Optional.Present(equipments.right),
        left = Optional.Present(equipments.left),
        accessory1 = Optional.Present(equipments.accessory1),
        accessory2 = Optional.Present(equipments.accessory2),
    )
}
