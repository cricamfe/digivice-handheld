package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonAddress
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonStatusAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource

/** Port of `Backend/Memory/Readers/Interfaces/IDigimonReader.cs`. */
interface IDigimonReader {
    fun read(
        digimonAddress: DigimonAddress,
        digimonStatusAddresses: DigimonStatusAddresses,
        inBattleAddresses: InBattleAddresses,
        partySlotIndex: Int,
    ): DigimonResource?
}
