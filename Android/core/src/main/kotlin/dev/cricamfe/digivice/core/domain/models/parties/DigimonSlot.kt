package dev.cricamfe.digivice.core.domain.models.parties

/** Port of `Backend/Domain/Models/Parties/DigimonSlot.cs`. */
data class DigimonSlot(
    var index: Int = 0,
    var digimonId: Int? = null,
    var digimon: Digimon? = null,
)
