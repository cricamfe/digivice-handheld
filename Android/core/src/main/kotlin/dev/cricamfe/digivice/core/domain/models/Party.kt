package dev.cricamfe.digivice.core.domain.models

import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot

/** Port of `Backend/Domain/Models/Party.cs` (list equality is structural, see [Npc]). */
data class Party(
    var slots: List<DigimonSlot> = emptyList(),
)
