package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.INpcsLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.INpcsProvider
import dev.cricamfe.digivice.core.domain.assemblers.NpcsAssembler
import dev.cricamfe.digivice.core.domain.models.Npcs

/** Port of `Backend/Application/Providers/NpcsProvider.cs`. */
class NpcsProvider(private val npcsLoader: INpcsLoader) : INpcsProvider {
    override fun get(): Npcs = NpcsAssembler.assemble(npcsLoader.load())
}
