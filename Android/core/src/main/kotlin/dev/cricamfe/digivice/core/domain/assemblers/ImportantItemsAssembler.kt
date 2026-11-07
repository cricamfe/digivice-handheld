package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource

/** Port of `Backend/Domain/Assemblers/ImportantItemsAssembler.cs`. */
object ImportantItemsAssembler {
    fun assemble(resource: ImportantItemsResource): ImportantItems = ImportantItems(
        treeBoots = resource.treeBoots.toInt() != 0,
        fishingPole = resource.fishingPole.toInt() != 0,
        asukaTrophy = resource.asukaTrophy.toInt() != 0,
        sunTrophy = resource.sunTrophy.toInt() != 0,
    )
}
