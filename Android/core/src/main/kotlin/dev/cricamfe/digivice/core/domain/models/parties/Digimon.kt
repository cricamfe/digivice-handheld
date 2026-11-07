package dev.cricamfe.digivice.core.domain.models.parties

import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.DigievolutionSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.InBattle
import dev.cricamfe.digivice.core.domain.models.parties.digimons.StoredDigievolution
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital

/** Port of `Backend/Domain/Models/Parties/Digimon.cs`. */
data class Digimon(
    var level: Int = 0,
    var tp: Int = 0,
    var blast: Int = 0,
    var experience: Int = 0,
    var hp: Vital = Vital(),
    var mp: Vital = Vital(),
    var inBattle: InBattle = InBattle(),
    var attributes: Attributes = Attributes(),
    var elements: Elements = Elements(),
    var equipments: Equipments = Equipments(),
    var digievolutions: List<DigievolutionSlot> = emptyList(),
    var storedDigievolutions: List<StoredDigievolution> = emptyList(),
    var activeDigievolutionId: Int? = null,
)
