package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.readers.FieldTaskProbe
import dev.cricamfe.digivice.core.memory.readers.PlayerActor
import dev.cricamfe.digivice.core.memory.readers.TaskRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * A 2 MiB PS1 RAM image made for the tests, with nothing taken from a dump of the game: every byte is zero but the ones
 * written here, at the addresses of `Backend/Memory/Definitions` (read through the core's address repository) and of
 * the task layout the readers follow. [build] then sets the last four bytes of the [GameSignature] region so that its
 * CRC32 is the game's, which is all the connector checks.
 *
 * [game] is a game in progress in a field, [title] the title screen, [outOfField] a mode with no field (as in a battle).
 * Their layout is in the constants below, which the tests use to change the RAM between cycles.
 */
class SyntheticRam {
    private val bytes = ByteArray(FakeRetroArch.RAM_SIZE)
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    private var registered = 0

    fun int32(address: Long, value: Int) = apply { buffer.putInt(address.toInt(), value) }

    fun int16(address: Long, value: Int) = apply { buffer.putShort(address.toInt(), value.toShort()) }

    fun byte(address: Long, value: Int) = apply { bytes[address.toInt()] = value.toByte() }

    /** The Digimon of the party slots, in order; [EMPTY_SLOT] for a free one. */
    fun party(vararg ids: Int) = apply {
        require(ids.size == party.slots.size) { "the party has ${party.slots.size} slots" }
        party.slots.zip(ids.toList()).forEach { (slot, id) -> byte(slot.address, id) }
    }

    /** The mode the game is in, which is the map id while in a field. */
    fun mode(mode: Int) = int16(player.mapId, mode)

    /** A task with its header at [address], registered in the next free entry of the task registry. */
    fun task(address: Long, id: Int, key1: Int = 0, key2: Int = 0, state: Int = TaskRegistry.STATE_RUN) = apply {
        require(registered < TaskRegistry.SIZE) { "the task registry is full" }
        int32(player.taskRegistry + registered++ * Int.SIZE_BYTES, (KSEG0 + address).toInt())
        int32(address + TaskRegistry.ID, id)
        int32(address + TaskRegistry.KEY1, key1)
        int32(address + TaskRegistry.KEY2, key2)
        int32(address + TaskRegistry.STATE, state)
    }

    /** An actor of the field that is drawn: running, with an animation file and a tile. */
    fun actor(address: Long, key1: Int, kind: Int, x: Int, y: Int, dir: Int = 0) = apply {
        task(address, PlayerActor.FIELD_TASK_ACTOR, key1, kind)
        int32(address + player.actorTileX, x)
        int32(address + player.actorTileY, y)
        int32(address + player.actorDir, dir)
        int32(address + player.actorAnimFile, ANIMATION_FILE)
    }

    /** The image, with the region of the signature closed by four bytes that give it the CRC32 [signature]. */
    fun build(signature: Long? = GameSignature.CRC32): ByteArray {
        val image = bytes.copyOf()
        if (signature != null) sign(image, signature)
        return image
    }

    /** One character of the map: what `FieldActorsReader` reports for it. */
    data class Character(val key1: Int, val x: Int, val y: Int, val dir: Int)

    companion object {
        private val repository by lazy { TestEnvironment.createAddressesRepository() }
        private val player by lazy { repository.getPlayerAddresses() }
        private val party by lazy { repository.getPartyAddresses() }

        private const val KSEG0 = 0x80000000L
        private const val ANIMATION_FILE = 0x1234

        val EMPTY_SLOT: Int get() = party.emptySlotId

        /** Agumon, Veemon and Guilmon: three different partners, as a loaded save always has. */
        val PARTY = intArrayOf(3, 4, 5)

        /** A mode of the field (`0x2xx`), and one that is not a field. */
        const val FIELD_MODE = 0x0211
        const val NO_FIELD_MODE = 0x0100

        /** The field's main task, first in the registry; a partner that follows the player; the player's actor. */
        const val FIELD_TASK = 0xAA100L
        const val PARTNER_ACTOR = 0xAC500L
        const val PLAYER_ACTOR = 0xAC63CL

        /** Where the player stands. */
        const val START_X = 519
        const val START_Y = 629

        /** The characters of the map, one after another from [FIRST_CHARACTER], all in the page at 0xB8000. */
        const val FIRST_CHARACTER = 0xB831CL
        const val CHARACTER_STRIDE = 0x100L
        val CHARACTERS = listOf(
            Character(0x21, 144, 647, 1),
            Character(0x4D, 209, 201, 1),
            Character(0x4E, 768, 490, 1),
            Character(0x134, 377, 310, 7),
            Character(0x136, 736, 233, 1),
            Character(0x137, 1072, 608, 1),
        )

        /** Two tasks of a mode with no field, which are neither actors nor the field. */
        val OTHER_TASKS = listOf(0xB0040L, 0xB4040L)

        /** A game in progress in a field, with the player at ([START_X], [START_Y]) and the [CHARACTERS] of the map. */
        fun game(): ByteArray = SyntheticRam().apply {
            party(*PARTY)
            mode(FIELD_MODE)
            task(FIELD_TASK, FieldTaskProbe.FIELD_TASK_FIELD)
            actor(PARTNER_ACTOR, key1 = PARTY[0], kind = 2, x = START_X - 16, y = START_Y)
            actor(PLAYER_ACTOR, key1 = 2, kind = PlayerActor.PLAYER_KIND, x = START_X, y = START_Y)
            CHARACTERS.forEachIndexed { index, it ->
                actor(FIRST_CHARACTER + index * CHARACTER_STRIDE, it.key1, kind = 1, x = it.x, y = it.y, dir = it.dir)
            }
        }.build()

        /** The title screen: the game's code is there, no save is loaded, so the three slots are free. */
        fun title(): ByteArray = SyntheticRam().party(EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT).build()

        /** The game while it boots: its code is there and the rest is still zeros, party slots included. */
        fun booting(): ByteArray = SyntheticRam().build()

        /** The party in a mode that is not a field: no field task, no actor, so no player and no position. */
        fun outOfField(): ByteArray = SyntheticRam().apply {
            party(*PARTY)
            mode(NO_FIELD_MODE)
            OTHER_TASKS.forEachIndexed { index, address -> task(address, id = 0x20 + index) }
        }.build()

        /** Every byte set from its address, different in every page, for the tests of the transport and its pages. */
        fun pattern(): ByteArray = ByteArray(FakeRetroArch.RAM_SIZE) { (it * 7 + (it ushr 8) * 13 + (it ushr 14) * 31).toByte() }

        /**
         * Sets the last four bytes of the signed region so that the CRC32 of the region is [target]. The CRC is linear: what
         * each of those 32 bits changes in it does not depend on the bytes before them, so the bits to set are the solution
         * of a 32 by 32 system over GF(2), which four consecutive bytes always have.
         */
        private fun sign(image: ByteArray, target: Long) {
            val end = GameSignature.END.toInt()
            val last = end - 3
            image.fill(0, last, end + 1)
            val unsigned = regionCrc(image)

            val zeros = crc32(ByteArray(4))
            // basis[b]: a combination of bits (mask) whose effect on the CRC has b as its highest bit.
            val basisEffect = LongArray(32)
            val basisMask = LongArray(32)
            for (bit in 0 until 32) {
                val alone = ByteArray(4).also { it[bit / 8] = (1 shl (bit % 8)).toByte() }
                var effect = crc32(alone) xor zeros
                var mask = 1L shl bit
                for (b in 31 downTo 0) {
                    if (effect shr b and 1L == 0L) continue
                    if (basisEffect[b] == 0L) {
                        basisEffect[b] = effect
                        basisMask[b] = mask
                        break
                    }
                    effect = effect xor basisEffect[b]
                    mask = mask xor basisMask[b]
                }
            }
            var wanted = target xor unsigned
            var bits = 0L
            for (b in 31 downTo 0) {
                if (wanted shr b and 1L == 0L) continue
                check(basisEffect[b] != 0L) { "the last four bytes cannot give the CRC 0x${target.toString(16)}" }
                wanted = wanted xor basisEffect[b]
                bits = bits xor basisMask[b]
            }
            for (bit in 0 until 32) {
                if (bits shr bit and 1L != 0L) image[last + bit / 8] = (image[last + bit / 8].toInt() or (1 shl (bit % 8))).toByte()
            }
            check(regionCrc(image) == target) { "the signature was not forged" }
        }

        private fun regionCrc(image: ByteArray): Long =
            CRC32().apply { update(image, GameSignature.START.toInt(), GameSignature.LENGTH) }.value

        private fun crc32(data: ByteArray): Long = CRC32().apply { update(data) }.value
    }
}
