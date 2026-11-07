package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource

/** Port of `Backend/Application/Loaders/Interfaces/IImportantItemsLoader.cs`. */
interface IImportantItemsLoader {
    fun load(): ImportantItemsResource
}
