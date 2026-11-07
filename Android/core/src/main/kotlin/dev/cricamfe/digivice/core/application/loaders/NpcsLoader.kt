package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.INpcsLoader
import dev.cricamfe.digivice.core.memory.resources.NpcsResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.INpcsReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/NpcsLoader.cs`. */
class NpcsLoader(
    private val addressesRepository: IAddressesRepository,
    private val npcsReader: INpcsReader,
) : INpcsLoader {
    override fun load(): NpcsResource = npcsReader.read(addressesRepository.getNpcsAddresses())
}
