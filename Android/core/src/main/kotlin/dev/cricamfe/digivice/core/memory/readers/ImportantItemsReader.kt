package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.ImportantItemsAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IImportantItemsReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource

/** Port of `Backend/Memory/Readers/ImportantItemsReader.cs`. */
class ImportantItemsReader(private val memoryReader: IMemoryReader) : IImportantItemsReader {
    override fun read(addresses: ImportantItemsAddresses): ImportantItemsResource {
        return ImportantItemsResource(
            treeBoots = memoryReader.readByte(addresses.treeBoots),
            fishingPole = memoryReader.readByte(addresses.fishingPole),
            asukaTrophy = memoryReader.readByte(addresses.asukaTrophy),
            sunTrophy = memoryReader.readByte(addresses.sunTrophy),
        )
    }
}
