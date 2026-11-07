package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IDigimonLoader
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/DigimonLoader.cs`. */
class DigimonLoader(
    private val addressesRepository: IAddressesRepository,
    private val digimonReader: IDigimonReader,
) : IDigimonLoader {
    override fun load(digimonId: Int, partySlotIndex: Int): DigimonResource? {
        val digimonAddress = addressesRepository.getDigimonAddressById(digimonId) ?: return null

        val digimonStatusAddresses = addressesRepository.getDigimonStatusAddresses()
        val inBattleAddresses = addressesRepository.getInBattleAddresses()
        return digimonReader.read(
            digimonAddress,
            digimonStatusAddresses,
            inBattleAddresses,
            partySlotIndex,
        )
    }
}
