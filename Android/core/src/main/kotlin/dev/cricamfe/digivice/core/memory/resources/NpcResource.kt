package dev.cricamfe.digivice.core.memory.resources

/** Port of `Backend/Memory/Resources/NpcResource.cs`. */
data class NpcResource(
    val battles: List<NpcBattleResource> = emptyList(),
)
