package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.ImportantItems

/** Port of `Backend/Application/Providers/Interfaces/IImportantItemsProvider.cs`. */
interface IImportantItemsProvider {
    fun get(): ImportantItems
}
