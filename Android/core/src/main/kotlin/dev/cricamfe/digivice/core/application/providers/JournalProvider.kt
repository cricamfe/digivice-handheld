package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.loaders.interfaces.IJournalLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IJournalProvider
import dev.cricamfe.digivice.core.domain.assemblers.JournalAssembler
import dev.cricamfe.digivice.core.domain.models.Journal

/** Port of `Backend/Application/Providers/JournalProvider.cs`. */
class JournalProvider(private val journalLoader: IJournalLoader) : IJournalProvider {
    override fun get(): Journal = JournalAssembler.assemble(journalLoader.load())
}
