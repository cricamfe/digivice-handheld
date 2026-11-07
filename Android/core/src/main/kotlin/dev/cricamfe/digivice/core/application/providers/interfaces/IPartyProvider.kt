package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.Party

/** Port of `Backend/Application/Providers/Interfaces/IPartyProvider.cs`. */
interface IPartyProvider {
    fun get(): Party
}
