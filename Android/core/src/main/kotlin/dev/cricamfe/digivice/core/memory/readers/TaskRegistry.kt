package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader

/**
 * The game's task registry (`TASK_REGISTRY` in the matching decompilation of the European version, ReGame-Labs/dw3_decomp,
 * MIT): 100 pointers, NULL for a free entry, to the tasks that other code finds by id. [PlayerReader] and
 * [FieldActorsReader] walk it the way `findTask` does.
 */
object TaskRegistry {
    const val SIZE = 100

    /** The task header: id, key1, key2 and state are its first four words. */
    const val ID = 0L
    const val KEY1 = 4L
    const val KEY2 = 8L
    const val STATE = 0x0CL
    const val HEADER_SIZE = 16L

    /** `TASK_RUN`: the normal state, the one in which an actor is drawn. */
    const val STATE_RUN = 1

    /** A pointer of the game points into the RAM through KSEG0. */
    private const val KSEG0 = 0x80000000L
    private const val RAM_SIZE = 0x200000L

    /**
     * The RAM addresses of the registered tasks, in the order of the registry, read as the sequence is consumed (so that
     * stopping early stops reading). A free entry is skipped, and so is a pointer that is not a word aligned address of
     * the RAM with room for [needed] bytes: it is never read.
     */
    fun tasks(memoryReader: IMemoryReader, registry: Long, needed: Long): Sequence<Long> = sequence {
        for (i in 0 until SIZE) {
            val pointer = memoryReader.readInt32(registry + i * Int.SIZE_BYTES).toLong() and 0xFFFFFFFFL
            val task = pointer - KSEG0
            if (pointer == 0L || task < 0 || task % Int.SIZE_BYTES != 0L || task > RAM_SIZE - needed) continue
            yield(task)
        }
    }
}
