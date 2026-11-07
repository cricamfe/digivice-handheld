package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IDigimonLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IPartyLoader
import dev.cricamfe.digivice.core.memory.resources.PartyResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IPartyReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/PartyLoader.cs`. */
class PartyLoader(
    private val addressesRepository: IAddressesRepository,
    private val partyReader: IPartyReader,
    private val digimonLoader: IDigimonLoader,
) : IPartyLoader {
    override fun load(): PartyResource {
        val partyAddresses = addressesRepository.getPartyAddresses()
        val partyResource = partyReader.read(partyAddresses)

        // The C# fills `slotResource.DigimonResource` in place; the Kotlin resources are immutable, so the slots are
        // rebuilt with the loaded digimon (empty slots, and ids without an address, stay as read).
        return partyResource.copy(
            slotsResource = partyResource.slotsResource.map { slotResource ->
                if (slotResource.digimonId != partyAddresses.emptySlotId) {
                    slotResource.copy(digimonResource = digimonLoader.load(slotResource.digimonId, slotResource.index))
                } else {
                    slotResource
                }
            },
        )
    }
}
