package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IAuctionsLoader
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IAuctionsReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/AuctionsLoader.cs`. */
class AuctionsLoader(
    private val addressesRepository: IAddressesRepository,
    private val auctionsReader: IAuctionsReader,
) : IAuctionsLoader {
    override fun load(): AuctionsResource = auctionsReader.read(addressesRepository.getAuctionsAddresses())
}
