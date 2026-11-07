package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.parties.DigimonResource

/** Port of `Backend/Application/Loaders/Interfaces/IDigimonLoader.cs`. */
interface IDigimonLoader {
    fun load(digimonId: Int, partySlotIndex: Int): DigimonResource?
}
