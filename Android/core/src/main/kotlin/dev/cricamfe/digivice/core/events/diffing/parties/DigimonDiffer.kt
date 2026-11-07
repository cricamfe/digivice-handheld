package dev.cricamfe.digivice.core.events.diffing.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.events.converters.parties.DigimonConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.changedItems
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.diffing.extensions.nested
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.AttributesDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.DigievolutionSlotDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.ElementsDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.EquipmentsDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.InBattleDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.StoredDigievolutionDiffer
import dev.cricamfe.digivice.core.events.diffing.parties.digimons.VitalDiffer
import dev.cricamfe.digivice.core.events.dto.parties.DigimonDto

/** Port of `Backend/Events/Diffing/Parties/DigimonDiffer.cs`. Only reads the digimons. */
object DigimonDiffer {
    fun diff(previousDigimon: Digimon?, newDigimon: Digimon?): DigimonDto? {
        if (newDigimon.hasNoChanges(previousDigimon)) return null
        if (previousDigimon == null) return DigimonConverter.toDto(newDigimon)

        val dto = DigimonDto(
            level = changed(previousDigimon.level, newDigimon.level),
            tp = changed(previousDigimon.tp, newDigimon.tp),
            blast = changed(previousDigimon.blast, newDigimon.blast),
            experience = changed(previousDigimon.experience, newDigimon.experience),
            hp = nested(VitalDiffer.diff(previousDigimon.hp, newDigimon.hp)),
            mp = nested(VitalDiffer.diff(previousDigimon.mp, newDigimon.mp)),
            inBattle = nested(InBattleDiffer.diff(previousDigimon.inBattle, newDigimon.inBattle)),
            attributes = nested(AttributesDiffer.diff(previousDigimon.attributes, newDigimon.attributes)),
            elements = nested(ElementsDiffer.diff(previousDigimon.elements, newDigimon.elements)),
            equipments = nested(EquipmentsDiffer.diff(previousDigimon.equipments, newDigimon.equipments)),
            digievolutions = changedItems(
                newDigimon.digievolutions.mapNotNull { newSlot ->
                    DigievolutionSlotDiffer.diff(previousDigimon.digievolutions.firstOrNull { it.index == newSlot.index }, newSlot)
                },
            ),
            activeDigievolutionId = changed(previousDigimon.activeDigievolutionId, newDigimon.activeDigievolutionId),
            storedDigievolutions = changedItems(
                newDigimon.storedDigievolutions.mapNotNull { newStored ->
                    StoredDigievolutionDiffer.diff(
                        previousDigimon.storedDigievolutions.firstOrNull { it.digievolutionId == newStored.digievolutionId },
                        newStored,
                    )
                },
            ),
        )

        // The C# returned null when none of the thirteen fields changed: here, when no field of the dto was set.
        return if (dto == DigimonDto()) null else dto
    }
}
