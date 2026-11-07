package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IDigimonBattleLoader
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonBattleReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/DigimonBattleLoader.cs`. */
class DigimonBattleLoader(
    private val addressesRepository: IAddressesRepository,
    private val digimonBattleReader: IDigimonBattleReader,
) : IDigimonBattleLoader {
    override fun load(): DigimonBattleResource = digimonBattleReader.read(
        addressesRepository.getDigimonBattleAddresses(),
        addressesRepository.getEnemyAddresses(),
    )
}
