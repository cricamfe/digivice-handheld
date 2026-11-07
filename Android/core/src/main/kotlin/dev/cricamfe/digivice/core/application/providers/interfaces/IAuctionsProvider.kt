package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.Auctions

/** Port of `Backend/Application/Providers/Interfaces/IAuctionsProvider.cs`. */
interface IAuctionsProvider {
    fun get(): Auctions
}
