package dev.cricamfe.digivice.core.memory.resources.parties

import dev.cricamfe.digivice.core.memory.resources.parties.digimons.AttributesResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.ElementsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.EquipmentsResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource

/** Port of `Backend/Memory/Resources/Parties/DigimonResource.cs`. */
data class DigimonResource(
    val experience: Int = 0,
    val level: Int = 0,
    val tp: Int = 0,
    val blast: Int = 0,
    val hp: VitalResource = VitalResource(),
    val mp: VitalResource = VitalResource(),
    val inBattle: InBattleResource = InBattleResource(),
    val attributes: AttributesResource = AttributesResource(),
    val elements: ElementsResource = ElementsResource(),
    val equipments: EquipmentsResource = EquipmentsResource(),
    val digievolutions: List<DigievolutionSlotResource> = emptyList(),
    val storedDigievolutions: List<StoredDigievolutionResource> = emptyList(),
    val activeDigievolutionId: Int = 0,
)
