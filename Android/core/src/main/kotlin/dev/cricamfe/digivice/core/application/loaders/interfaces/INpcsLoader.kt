package dev.cricamfe.digivice.core.application.loaders.interfaces

import dev.cricamfe.digivice.core.memory.resources.NpcsResource

/** Port of `Backend/Application/Loaders/Interfaces/INpcsLoader.cs`. */
interface INpcsLoader {
    fun load(): NpcsResource
}
