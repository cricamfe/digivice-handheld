package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IAuctionsLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IAuctionsProvider
import dev.cricamfe.digivice.core.domain.assemblers.AuctionsAssembler
import dev.cricamfe.digivice.core.domain.models.Auctions

/** Port of `Backend/Application/Providers/AuctionsProvider.cs`. */
class AuctionsProvider(private val auctionsLoader: IAuctionsLoader) : IAuctionsProvider {
    override fun get(): Auctions = AuctionsAssembler.assemble(auctionsLoader.load())
}
