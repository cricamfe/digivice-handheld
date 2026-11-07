package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional

/** Port of `Backend/Events/Converters/ImportantItemsConverter.cs`. Reads the domain model, never changes it. */
object ImportantItemsConverter {
    fun toDto(importantItems: ImportantItems): ImportantItemsDto = ImportantItemsDto(
        treeBoots = Optional.Present(importantItems.treeBoots),
        fishingPole = Optional.Present(importantItems.fishingPole),
        asukaTrophy = Optional.Present(importantItems.asukaTrophy),
        sunTrophy = Optional.Present(importantItems.sunTrophy),
    )
}
