package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.PlayerResource

/** Port of `Backend/Application/Loaders/Interfaces/IPlayerLoader.cs`. */
interface IPlayerLoader {
    fun load(): PlayerResource
}
