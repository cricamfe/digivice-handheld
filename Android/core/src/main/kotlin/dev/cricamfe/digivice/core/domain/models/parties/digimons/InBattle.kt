package dev.cricamfe.digivice.core.domain.models.parties.digimons

/** Port of `Backend/Domain/Models/Parties/Digimons/InBattle.cs`. */
data class InBattle(
    /** The Digimon the fighter is now in a battle: its own id, or the form it digivolved to. */
    var formId: Int = 0,
    var condition: Int = 0,
    var strength: Int = 0,
    var defense: Int = 0,
    var speed: Int = 0,
    var hp: Vital = Vital(),
    var mp: Vital = Vital(),
)
