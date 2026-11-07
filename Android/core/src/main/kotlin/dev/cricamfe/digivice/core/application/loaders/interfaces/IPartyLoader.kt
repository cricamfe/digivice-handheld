package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.PartyResource

/** Port of `Backend/Application/Loaders/Interfaces/IPartyLoader.cs`. */
interface IPartyLoader {
    fun load(): PartyResource
}
