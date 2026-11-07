package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.PlayerAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IPlayerReader
import dev.cricamfe.digivice.core.memory.resources.PlayerResource

/**
 * Port of `Backend/Memory/Readers/PlayerReader.cs`.
 *
 * The position of the player is not at a fixed address: the field creates the player as an actor, a task that is
 * allocated on the heap. It is found as the game does (`findTask` in the matching decompilation of the European
 * version, ReGame-Labs/dw3_decomp, MIT): the first pointer of the task registry that points to a task with the actor
 * id whose kind (key2) is 0, the player. Its key1 is 2, or the id of the character a field lets lead instead (1, 0x6A,
 * 0x146 or 0x147). The position in pixels is `Actor.tile`. Without such an actor there is no position: (0, 0).
 *
 * Each task is read from wherever the heap put it, so a page the cycle has not read yet costs one more request.
 */
class PlayerReader(private val memoryReader: IMemoryReader) : IPlayerReader {
    /**
     * Where the last [read] found the player's actor, or null when it found none. The loop follows it with quick reads
     * between cycles. Only the loop's thread touches it.
     */
    var actorAddress: Long? = null
        private set

    /** Forgets the actor: nothing is known until the next [read]. */
    fun forgetActor() {
        actorAddress = null
    }

    override fun read(addresses: PlayerAddresses): PlayerResource {
        val actor = findPlayerActor(addresses)
        actorAddress = actor

        return PlayerResource(
            bits = memoryReader.readInt32(addresses.bits),
            mapId = memoryReader.readInt16(addresses.mapId),
            previousMapId = memoryReader.readInt16(addresses.previousMapId),
            seabedRoute = memoryReader.readByte(addresses.seabedRoute),
            mapVariant = memoryReader.readByte(addresses.mapVariant),
            positionX = actor?.let { memoryReader.readInt32(it + addresses.actorTileX) } ?: 0,
            positionY = actor?.let { memoryReader.readInt32(it + addresses.actorTileY) } ?: 0,
            extraChapter = memoryReader.readInt32(GAME_PROGRESS_ADDRESS) == 0x2D,
        )
    }

    /** The RAM address of the player's actor, or null when the registry has none. */
    private fun findPlayerActor(addresses: PlayerAddresses): Long? {
        val needed = maxOf(PlayerActor.HEADER_SIZE, maxOf(addresses.actorTileX, addresses.actorTileY) + Int.SIZE_BYTES)

        return TaskRegistry.tasks(memoryReader, addresses.taskRegistry, needed).firstOrNull { task ->
            memoryReader.readInt32(task) == PlayerActor.FIELD_TASK_ACTOR &&
                PlayerActor.isPlayer(
                    PlayerActor.FIELD_TASK_ACTOR,
                    memoryReader.readInt32(task + Int.SIZE_BYTES),
                    memoryReader.readInt32(task + 2 * Int.SIZE_BYTES),
                )
        }
    }
}
