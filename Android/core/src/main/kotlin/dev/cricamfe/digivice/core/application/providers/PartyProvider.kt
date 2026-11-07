package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IPartyLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IPartyProvider
import dev.cricamfe.digivice.core.domain.assemblers.PartyAssembler
import dev.cricamfe.digivice.core.domain.models.Party

/** Port of `Backend/Application/Providers/PartyProvider.cs`. */
class PartyProvider(private val partyLoader: IPartyLoader) : IPartyProvider {
    override fun get(): Party = PartyAssembler.assemble(partyLoader.load())
}
