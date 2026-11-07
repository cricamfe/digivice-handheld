package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.AuctionsResource

/** Port of `Backend/Application/Loaders/Interfaces/IAuctionsLoader.cs`. */
interface IAuctionsLoader {
    fun load(): AuctionsResource
}
