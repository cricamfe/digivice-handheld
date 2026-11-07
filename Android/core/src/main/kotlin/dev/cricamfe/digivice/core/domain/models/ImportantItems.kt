package dev.cricamfe.digivice.core.domain.models

/** Port of `Backend/Domain/Models/ImportantItems.cs`. */
data class ImportantItems(
    var treeBoots: Boolean = false,
    var fishingPole: Boolean = false,
    var asukaTrophy: Boolean = false,
    var sunTrophy: Boolean = false,
)
