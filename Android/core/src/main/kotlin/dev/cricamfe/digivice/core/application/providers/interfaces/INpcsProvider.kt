package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.Npcs

/** Port of `Backend/Application/Providers/Interfaces/INpcsProvider.cs`. */
interface INpcsProvider {
    fun get(): Npcs
}
