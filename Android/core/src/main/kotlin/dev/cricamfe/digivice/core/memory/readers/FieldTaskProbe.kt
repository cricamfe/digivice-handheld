package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.domain.assemblers.PlayerAssembler
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The field's main task (`FieldTask`, `FIELDSTG_updateField` in the matching decompilation of the European version,
 * ReGame-Labs/dw3_decomp, MIT), which says when the game changes field. It is in `TASK_INIT` from when the mode starts while
 * the field loads and `TASK_RUN` once it shows; `TASK_DONE` closes it (the view shrinks, the cover fades in, the next mode
 * is requested and the task ends). Before that, `leaveDelay` frames may pass with the field still on the screen. A warp
 * goes from `TASK_RUN`, in the substate that plays its effect and kills the characters.
 *
 * The field is not seen yet when it runs: the area name banner (`AreaBanner`, `FIELDSTG_updateBanner`) still covers it, for
 * 60 frames more after the field tells it to go. It then opens its clip, from `TASK_DONE` substate 2 on, and ends.
 *
 * A full cycle finds it ([read]), and the loop follows it between cycles with one request ([parse]). Only the loop's
 * thread uses it.
 */
class FieldTaskProbe(
    private val memoryReader: IMemoryReader,
    private val addressesRepository: IAddressesRepository,
) {
    data class Reading(
        val state: Int,
        val substate: Int,
        val nextMode: Int,
        val leaveDelay: Int,
        /** The area name banner covers the screen. A quick read does not look at it: the field has been running for a while. */
        val bannerCovers: Boolean = false,
    ) {
        /** The field is on the screen and staying there. */
        val isRunning: Boolean
            get() = when (state) {
                TaskRegistry.STATE_RUN -> substate != SUBSTATE_WARP && !bannerCovers
                STATE_DONE -> leaveDelay > 0
                else -> false
            }

        /** The map the game asked to go to, once it has. */
        val leavingFor: String?
            get() = if (state == STATE_DONE && nextMode != 0) PlayerAssembler.formatMapId(nextMode.toShort()) else null
    }

    /** Where the last [read] found the task, or null when it found none. */
    var address: Long? = null
        private set

    fun forget() {
        address = null
    }

    /** Finds the task in the registry and reads it. */
    fun read(): Reading? {
        val registry = addressesRepository.getPlayerAddresses().taskRegistry
        val task = TaskRegistry.tasks(memoryReader, registry, SIZE.toLong())
            .firstOrNull { memoryReader.readInt32(it + TaskRegistry.ID) == FIELD_TASK_FIELD }
        address = task
        return task?.let {
            Reading(
                state = memoryReader.readInt32(it + TaskRegistry.STATE),
                substate = memoryReader.readInt32(it + SUBSTATE),
                nextMode = memoryReader.readInt32(it + NEXT_MODE),
                leaveDelay = memoryReader.readInt32(it + LEAVE_DELAY),
                bannerCovers = bannerCovers(registry),
            )
        }
    }

    private fun bannerCovers(registry: Long): Boolean {
        val banner = TaskRegistry.tasks(memoryReader, registry, SIZE.toLong())
            .firstOrNull { memoryReader.readInt32(it + TaskRegistry.ID) == FIELD_TASK_BANNER } ?: return false
        return when (memoryReader.readInt32(banner + TaskRegistry.STATE)) {
            STATE_DONE -> memoryReader.readInt32(banner + SUBSTATE) < BANNER_SUBSTATE_OPENS
            STATE_KILL -> false
            else -> true
        }
    }

    /** The reading in [bytes], the first [SIZE] bytes of the task, or null when they are not the task any more. */
    fun parse(bytes: ByteArray): Reading? {
        if (bytes.size < SIZE) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buffer.getInt(TaskRegistry.ID.toInt()) != FIELD_TASK_FIELD) return null
        return Reading(
            state = buffer.getInt(TaskRegistry.STATE.toInt()),
            substate = buffer.getInt(SUBSTATE.toInt()),
            nextMode = buffer.getInt(NEXT_MODE.toInt()),
            leaveDelay = buffer.getInt(LEAVE_DELAY.toInt()),
        )
    }

    companion object {
        const val FIELD_TASK_FIELD = 7
        const val FIELD_TASK_BANNER = 9
        const val STATE_DONE = 2
        const val STATE_KILL = 3
        const val BANNER_SUBSTATE_OPENS = 2
        const val SUBSTATE_WARP = 3
        const val SUBSTATE = 0x10L
        const val NEXT_MODE = 0x5CL
        const val LEAVE_DELAY = 0x68L
        const val SIZE = 0x6C

        /** The modes of the fields are 0x2xx and 0x3xx. */
        fun isFieldMode(mapId: String): Boolean = mapId.startsWith("02") || mapId.startsWith("03")
    }
}
