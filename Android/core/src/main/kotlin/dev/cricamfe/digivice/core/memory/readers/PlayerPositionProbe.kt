package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The position of the player between two full cycles: where the last cycle found the player's actor, and how to read the
 * position from a small block of its bytes. It does not read memory itself; the loop asks the connector for the bytes
 * (one request) and hands them to [position].
 *
 * Only the loop's thread uses it.
 */
class PlayerPositionProbe(
    private val playerReader: PlayerReader,
    private val addressesRepository: IAddressesRepository,
) {
    /** The position in pixels of the map picture. */
    data class Position(val x: Int, val y: Int)

    /** Where the actor was at the last full cycle, or null when it was not found (or is forgotten). */
    val actor: Long? get() = playerReader.actorAddress

    /** How many bytes from the start of the actor hold its header and its tile: one request. */
    val size: Int
        get() {
            val addresses = addressesRepository.getPlayerAddresses()
            return maxOf(PlayerActor.HEADER_SIZE, maxOf(addresses.actorTileX, addresses.actorTileY) + Int.SIZE_BYTES).toInt()
        }

    fun forget() = playerReader.forgetActor()

    /**
     * The position in [bytes], the first [size] bytes of the actor, or null when they are not the player's actor any
     * more (the task was freed and the memory reused): the next full cycle finds it again.
     */
    fun position(bytes: ByteArray): Position? {
        val addresses = addressesRepository.getPlayerAddresses()
        val needed = maxOf(PlayerActor.HEADER_SIZE, maxOf(addresses.actorTileX, addresses.actorTileY) + Int.SIZE_BYTES)
        if (bytes.size < needed) return null

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (!PlayerActor.isPlayer(id = buffer.getInt(0), key1 = buffer.getInt(4), key2 = buffer.getInt(8))) return null
        return Position(buffer.getInt(addresses.actorTileX.toInt()), buffer.getInt(addresses.actorTileY.toInt()))
    }
}
