package dev.cricamfe.digivice.core.events.converters.parties

import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.events.converters.parties.digimons.AttributesConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.DigievolutionSlotConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.ElementsConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.EquipmentsConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.InBattleConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.StoredDigievolutionConverter
import dev.cricamfe.digivice.core.events.converters.parties.digimons.VitalConverter
import dev.cricamfe.digivice.core.events.dto.parties.DigimonDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/Parties/DigimonConverter.cs`. Reads the domain model, never changes it. */
object DigimonConverter {
    fun toDto(digimon: Digimon): DigimonDto = DigimonDto(
        level = Optional.Present(digimon.level),
        tp = Optional.Present(digimon.tp),
        blast = Optional.Present(digimon.blast),
        experience = Optional.Present(digimon.experience),
        hp = Optional.Present(VitalConverter.toDto(digimon.hp)),
        mp = Optional.Present(VitalConverter.toDto(digimon.mp)),
        inBattle = Optional.Present(InBattleConverter.toDto(digimon.inBattle)),
        attributes = Optional.Present(AttributesConverter.toDto(digimon.attributes)),
        elements = Optional.Present(ElementsConverter.toDto(digimon.elements)),
        equipments = Optional.Present(EquipmentsConverter.toDto(digimon.equipments)),
        digievolutions = Optional.Present(digimon.digievolutions.map(DigievolutionSlotConverter::toDto)),
        activeDigievolutionId = Optional.Present(digimon.activeDigievolutionId),
        storedDigievolutions = Optional.Present(digimon.storedDigievolutions.map(StoredDigievolutionConverter::toDto)),
    )
}
