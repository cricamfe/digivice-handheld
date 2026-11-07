package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IPlayerLoader
import dev.cricamfe.digivice.core.memory.resources.PlayerResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IPlayerReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/PlayerLoader.cs`. */
class PlayerLoader(
    private val addressesRepository: IAddressesRepository,
    private val playerReader: IPlayerReader,
) : IPlayerLoader {
    override fun load(): PlayerResource = playerReader.read(addressesRepository.getPlayerAddresses())
}
