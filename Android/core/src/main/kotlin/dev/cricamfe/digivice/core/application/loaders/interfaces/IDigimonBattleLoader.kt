package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource

/** Port of `Backend/Application/Loaders/Interfaces/IDigimonBattleLoader.cs`. */
interface IDigimonBattleLoader {
    fun load(): DigimonBattleResource
}
