package dev.cricamfe.digivice.core.domain.models.parties.digimons

/** Port of `Backend/Domain/Models/Parties/Digimons/Attributes.cs`. */
data class Attributes(
    var strength: Int = 0,
    var defense: Int = 0,
    var spirit: Int = 0,
    var wisdom: Int = 0,
    var speed: Int = 0,
    var charisma: Int = 0,
)
