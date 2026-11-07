package dev.cricamfe.digivice.core.memory.readers

/**
 * What the player's actor is in the game's task registry (the matching decompilation of the European version,
 * ReGame-Labs/dw3_decomp, MIT): a task with the actor id whose kind (key2) is 0, the player. Its key1 is 2, or the id of
 * the character a field lets lead instead. Shared by [PlayerReader], which finds it, and by the quick reads that follow
 * it between cycles.
 */
object PlayerActor {
    const val FIELD_TASK_ACTOR = 5
    const val PLAYER_KIND = 0

    /** `FIELD_CHARACTER_PLAYER`, then the characters a field may list to lead the party instead. */
    val LEADER_CHARACTERS = setOf(2, 1, 0x6A, 0x146, 0x147)

    /** The task header holds id, key1 and key2 in its first three words. */
    const val HEADER_SIZE = 12L

    fun isPlayer(id: Int, key1: Int, key2: Int): Boolean =
        id == FIELD_TASK_ACTOR && key2 == PLAYER_KIND && key1 in LEADER_CHARACTERS
}
