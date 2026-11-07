package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IPlayerLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IPlayerProvider
import dev.cricamfe.digivice.core.domain.assemblers.PlayerAssembler
import dev.cricamfe.digivice.core.domain.models.Player

/** Port of `Backend/Application/Providers/PlayerProvider.cs`. */
class PlayerProvider(private val playerLoader: IPlayerLoader) : IPlayerProvider {
    override fun get(): Player = PlayerAssembler.assemble(playerLoader.load())
}
