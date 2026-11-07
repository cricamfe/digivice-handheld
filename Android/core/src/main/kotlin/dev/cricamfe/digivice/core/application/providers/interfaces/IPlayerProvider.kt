package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.Player

/** Port of `Backend/Application/Providers/Interfaces/IPlayerProvider.cs`. */
interface IPlayerProvider {
    fun get(): Player
}
