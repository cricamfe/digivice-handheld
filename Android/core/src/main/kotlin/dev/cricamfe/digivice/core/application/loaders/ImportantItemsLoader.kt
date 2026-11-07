package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IImportantItemsLoader
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IImportantItemsReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/ImportantItemsLoader.cs`. */
class ImportantItemsLoader(
    private val addressesRepository: IAddressesRepository,
    private val importantItemsReader: IImportantItemsReader,
) : IImportantItemsLoader {
    override fun load(): ImportantItemsResource = importantItemsReader.read(addressesRepository.getImportantItemsAddresses())
}
