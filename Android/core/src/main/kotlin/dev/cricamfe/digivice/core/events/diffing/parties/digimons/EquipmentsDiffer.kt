package dev.cricamfe.digivice.core.events.diffing.parties.digimons

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.events.converters.parties.digimons.EquipmentsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.parties.digimons.EquipmentsDto

/** Port of `Backend/Events/Diffing/Parties/Digimons/EquipmentsDiffer.cs`. Only reads the equipments. */
object EquipmentsDiffer {
    fun diff(previousEquipments: Equipments?, newEquipments: Equipments): EquipmentsDto? {
        if (newEquipments.hasNoChanges(previousEquipments)) return null
        if (previousEquipments == null) return EquipmentsConverter.toDto(newEquipments)

        return EquipmentsDto(
            head = changed(previousEquipments.head, newEquipments.head),
            body = changed(previousEquipments.body, newEquipments.body),
            right = changed(previousEquipments.right, newEquipments.right),
            left = changed(previousEquipments.left, newEquipments.left),
            accessory1 = changed(previousEquipments.accessory1, newEquipments.accessory1),
            accessory2 = changed(previousEquipments.accessory2, newEquipments.accessory2),
        )
    }
}
