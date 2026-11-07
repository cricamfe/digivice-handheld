package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/**
 * The characters the field is drawing besides the party: what the game shows as people on the map, read from the task
 * registry (see [TaskRegistry]; facts from ReGame-Labs/dw3_decomp, MIT).
 *
 * An actor is a registered task with the actor id. `FIELDSTG_createActor` gives the player kind (key2) 0, a partner 2, 4
 * or 8, and a character of the map 1 (`FIELDSTG_updateField`, from the stage's `FieldActorEntry`); key1 is the
 * character. `FIELDSTG_updateActor` draws it only while its state is `TASK_RUN`, it has an animation file, and its tile
 * is not (0, 0); one that fails any of the three is not on the screen and is not listed. The tile is in pixels of the
 * map picture, [dir] is the direction it faces (0 to 7; 5 to 7 are drawn mirrored).
 */
class FieldActorsReader(
    private val memoryReader: IMemoryReader,
    private val addressesRepository: IAddressesRepository,
) {
    data class Actor(val key1: Int, val x: Int, val y: Int, val dir: Int)

    /** The characters of the map that are drawn, in the order of the registry. At most [TaskRegistry.SIZE] entries are read. */
    fun read(): List<Actor> {
        val addresses = addressesRepository.getPlayerAddresses()
        val needed = maxOf(
            TaskRegistry.HEADER_SIZE,
            addresses.actorTileX + Int.SIZE_BYTES,
            addresses.actorTileY + Int.SIZE_BYTES,
            addresses.actorDir + Int.SIZE_BYTES,
            addresses.actorAnimFile + Int.SIZE_BYTES,
        )

        val actors = ArrayList<Actor>()
        for (task in TaskRegistry.tasks(memoryReader, addresses.taskRegistry, needed)) {
            if (memoryReader.readInt32(task + TaskRegistry.ID) != PlayerActor.FIELD_TASK_ACTOR) continue
            if (memoryReader.readInt32(task + TaskRegistry.KEY2) != MAP_CHARACTER_KIND) continue
            if (memoryReader.readInt32(task + TaskRegistry.STATE) != TaskRegistry.STATE_RUN) continue
            if (memoryReader.readInt32(task + addresses.actorAnimFile) == 0) continue

            val x = memoryReader.readInt32(task + addresses.actorTileX)
            val y = memoryReader.readInt32(task + addresses.actorTileY)
            if (x + y == 0) continue
            actors += Actor(
                key1 = memoryReader.readInt32(task + TaskRegistry.KEY1),
                x = x,
                y = y,
                dir = memoryReader.readInt32(task + addresses.actorDir),
            )
        }
        return actors
    }

    private companion object {
        /** The kind `FIELDSTG_updateField` gives the characters of the map. */
        const val MAP_CHARACTER_KIND = 1
    }
}
