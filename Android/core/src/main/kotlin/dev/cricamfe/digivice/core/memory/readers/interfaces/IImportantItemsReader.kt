package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.ImportantItemsAddresses
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource

/** Port of `Backend/Memory/Readers/Interfaces/IImportantItemsReader.cs`. */
interface IImportantItemsReader {
    fun read(addresses: ImportantItemsAddresses): ImportantItemsResource
}
