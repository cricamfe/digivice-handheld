package dev.cricamfe.digivice.core.domain.models

/**
 * Port of `Backend/Domain/Models/Npc.cs`. The C# record needed a hand-written `Equals` to compare its list by
 * sequence; a Kotlin data class compares lists by content already.
 */
data class Npc(
    var battles: List<NpcBattle> = emptyList(),
)
