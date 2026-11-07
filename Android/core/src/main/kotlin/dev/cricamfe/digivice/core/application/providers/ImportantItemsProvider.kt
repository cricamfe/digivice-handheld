package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IImportantItemsLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IImportantItemsProvider
import dev.cricamfe.digivice.core.domain.assemblers.ImportantItemsAssembler
import dev.cricamfe.digivice.core.domain.models.ImportantItems

/** Port of `Backend/Application/Providers/ImportantItemsProvider.cs`. */
class ImportantItemsProvider(private val importantItemsLoader: IImportantItemsLoader) : IImportantItemsProvider {
    override fun get(): ImportantItems = ImportantItemsAssembler.assemble(importantItemsLoader.load())
}
