package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.JournalResource

/** Port of `Backend/Application/Loaders/Interfaces/IJournalLoader.cs`. */
interface IJournalLoader {
    fun load(): JournalResource
}
