package dev.cricamfe.digivice.core.application.providers.interfaces

import dev.cricamfe.digivice.core.domain.models.Journal

/** Port of `Backend/Application/Providers/Interfaces/IJournalProvider.cs`. */
interface IJournalProvider {
    fun get(): Journal
}
