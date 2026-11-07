package dev.cricamfe.digivice.core.domain.models.parties.digimons

/** Port of `Backend/Domain/Models/Parties/Digimons/DigievolutionSlot.cs`. */
data class DigievolutionSlot(
    var index: Int = 0,
    var digievolutionId: Int? = null,
    var digievolution: Digievolution? = null,
)
