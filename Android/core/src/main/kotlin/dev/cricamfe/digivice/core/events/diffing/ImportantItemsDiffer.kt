package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.events.converters.ImportantItemsConverter
import dev.cricamfe.digivice.core.events.diffing.extensions.changed
import dev.cricamfe.digivice.core.events.diffing.extensions.hasNoChanges
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto

/** Port of `Backend/Events/Diffing/ImportantItemsDiffer.cs`. Only reads the items. */
object ImportantItemsDiffer {
    fun diff(previousImportantItems: ImportantItems?, newImportantItems: ImportantItems): ImportantItemsDto {
        if (newImportantItems.hasNoChanges(previousImportantItems)) return ImportantItemsDto()
        if (previousImportantItems == null) return ImportantItemsConverter.toDto(newImportantItems)

        return ImportantItemsDto(
            treeBoots = changed(previousImportantItems.treeBoots, newImportantItems.treeBoots),
            fishingPole = changed(previousImportantItems.fishingPole, newImportantItems.fishingPole),
            asukaTrophy = changed(previousImportantItems.asukaTrophy, newImportantItems.asukaTrophy),
            sunTrophy = changed(previousImportantItems.sunTrophy, newImportantItems.sunTrophy),
        )
    }
}
