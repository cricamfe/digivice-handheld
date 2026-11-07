package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReader
import dev.cricamfe.digivice.core.memory.SnapshotAccessor
import dev.cricamfe.digivice.core.memory.addresses.PlayerAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PlayerReaderTests {
    @Test
    fun `read should map player resource correctly`() {
        val addresses = PlayerAddresses(
            bits = 0x1000,
            mapId = 0x3000,
            previousMapId = 0x3500,
            seabedRoute = 0x4000,
            mapVariant = 0x5000,
        )
        val memoryReader = FakeMemoryReader().apply {
            int32[0x1000] = 15000
            int16[0x3000] = 4
            int16[0x3500] = 0x023E
            bytes[0x4000] = 0x08u
            bytes[0x5000] = 0x01u
        }

        val result = PlayerReader(memoryReader).read(addresses)

        assertEquals(15000, result.bits)
        assertEquals(4.toShort(), result.mapId)
        assertEquals(0x023E.toShort(), result.previousMapId)
        assertEquals(0x08.toUByte(), result.seabedRoute)
        assertEquals(0x01.toUByte(), result.mapVariant)
        assertEquals(0, result.positionX, "an empty registry has no player")
        assertEquals(0, result.positionY)
    }

    @Test
    fun `only STORY_EXTRA selects the alternate stage table`() {
        val memory = FakeMemoryReader()
        val reader = PlayerReader(memory)
        for (progress in listOf(0x2C, 0x2D, 0x2E, 0x12D)) {
            memory.int32[0x4B370] = progress
            assertEquals(progress == 0x2D, reader.read(PlayerAddresses()).extraChapter)
        }
    }

    // ---- the position, through the game's task registry -------------------------------------------------------------
    // A RAM of 2 MiB behind a real MemoryReader, so a read outside it throws, as it would on the device.

    private class Ram {
        val bytes = ByteArray(0x200000)
        val reader = MemoryReader(SnapshotAccessor(bytes))

        fun put(address: Int, value: Int) {
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(address, value)
        }

        /** Registers a pointer in the entry `index` of the registry. */
        fun register(index: Int, pointer: Long) = put(REGISTRY + index * 4, pointer.toInt())

        /** A task of the registry at `address` (RAM address): its header and, for an actor, its tile. */
        fun task(address: Int, id: Int, key1: Int, key2: Int, tileX: Int = 0, tileY: Int = 0) {
            put(address, id)
            put(address + 4, key1)
            put(address + 8, key2)
            put(address + TILE_X, tileX)
            put(address + TILE_Y, tileY)
        }

        fun read() = PlayerReader(reader).read(ADDRESSES)
    }

    @Test
    fun `read should take the position from the player actor the registry points to`() {
        // Verified on the device in the map 0272: the entry at 0x4B618 points to 0x800ACD14.
        val ram = Ram().apply {
            task(0xAB8EC, id = 7, key1 = 0, key2 = 0)
            register(0, 0x800AB8EC)
            task(0xACD14, id = 5, key1 = 2, key2 = 0, tileX = 776, tileY = 224)
            register(2, 0x800ACD14)
        }

        val result = ram.read()

        assertEquals(776, result.positionX)
        assertEquals(224, result.positionY)
    }

    @Test
    fun `read should find no position when the registry has no player`() {
        val ram = Ram().apply {
            task(0xAB8E4, id = 12, key1 = 0, key2 = 0)
            register(0, 0x800AB8E4)
            task(0xC419C, id = 14, key1 = 0, key2 = 0)
            register(1, 0x800C419C)
        }

        val result = ram.read()

        assertEquals(0, result.positionX)
        assertEquals(0, result.positionY)
    }

    @Test
    fun `read should skip null entries and the tasks that are not the player`() {
        val ram = Ram().apply {
            register(0, 0)
            task(0xAB8EC, id = 7, key1 = 2, key2 = 0, tileX = 1, tileY = 1) // not an actor
            register(1, 0x800AB8EC)
            task(0xB0000, id = 5, key1 = 3, key2 = 2, tileX = 500, tileY = 500) // a follower
            register(3, 0x800B0000)
            task(0xB0200, id = 5, key1 = 0x21, key2 = 1, tileX = 144, tileY = 647) // another character
            register(5, 0x800B0200)
            task(0xB0400, id = 5, key1 = 2, key2 = 0, tileX = 519, tileY = 629)
            register(9, 0x800B0400)
        }

        val result = ram.read()

        assertEquals(519, result.positionX)
        assertEquals(629, result.positionY)
    }

    @Test
    fun `read should take the first player when the registry has two`() {
        val ram = Ram().apply {
            task(0xB0000, id = 5, key1 = 2, key2 = 0, tileX = 10, tileY = 20)
            register(4, 0x800B0000)
            task(0xB0200, id = 5, key1 = 2, key2 = 0, tileX = 30, tileY = 40)
            register(6, 0x800B0200)
        }

        val result = ram.read()

        assertEquals(10, result.positionX)
        assertEquals(20, result.positionY)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 0x6A, 0x146, 0x147])
    fun `read should follow the character a field lets lead instead of the player`(leader: Int) {
        val ram = Ram().apply {
            task(0xB0000, id = 5, key1 = leader, key2 = 0, tileX = 321, tileY = 201)
            register(2, 0x800B0000)
        }

        val result = ram.read()

        assertEquals(321, result.positionX)
        assertEquals(201, result.positionY)
    }

    @Test
    fun `read should not take for the player a task of kind 0 that is not a character of the party`() {
        val ram = Ram().apply {
            task(0xB0000, id = 5, key1 = 0x134, key2 = 0, tileX = 321, tileY = 201)
            register(2, 0x800B0000)
        }

        assertEquals(0, ram.read().positionX)
    }

    @Test
    fun `read should never read a pointer that is not an address of the RAM`() {
        val ram = Ram().apply {
            // Not KSEG0, beyond the 2 MiB, null in KSEG0, not word aligned, and too close to the end for the actor.
            for ((index, pointer) in listOf(0x000ACD14L, 0xFFFFFFFFL, 0x80200000L, 0x80000000L, 0x800ACD15L, 0x801FFFFCL, 0x7FFFFFFCL)
                .withIndex()) {
                register(index, pointer)
            }
            task(0xB0000, id = 5, key1 = 2, key2 = 0, tileX = 88, tileY = 99)
            register(7, 0x800B0000)
        }

        val result = ram.read()

        assertEquals(88, result.positionX)
        assertEquals(99, result.positionY)
    }

    @Test
    fun `read should find no position when every pointer is bad`() {
        val ram = Ram().apply {
            for (index in 0 until 100) register(index, 0xFFFFFFF0L)
        }

        val result = ram.read()

        assertEquals(0, result.positionX)
        assertEquals(0, result.positionY)
    }

    @Test
    fun `read should look at the 100 entries of the registry and no more`() {
        val last = Ram().apply {
            task(0xB0000, id = 5, key1 = 2, key2 = 0, tileX = 7, tileY = 8)
            register(99, 0x800B0000)
        }
        val past = Ram().apply {
            task(0xB0000, id = 5, key1 = 2, key2 = 0, tileX = 7, tileY = 8)
            register(100, 0x800B0000) // the word after the registry's tasks
        }

        assertEquals(7, last.read().positionX)
        assertEquals(0, past.read().positionX)
    }

    @Test
    fun `read should keep the position as signed 32 bit values`() {
        val ram = Ram().apply {
            task(0xB0000, id = 5, key1 = 2, key2 = 0, tileX = Int.MIN_VALUE, tileY = Int.MAX_VALUE)
            register(2, 0x800B0000)
        }

        val result = ram.read()

        assertEquals(Int.MIN_VALUE, result.positionX)
        assertEquals(Int.MAX_VALUE, result.positionY)
    }

    private companion object {
        const val REGISTRY = 0x4B610
        const val TILE_X = 0x58
        const val TILE_Y = 0x5C
        val ADDRESSES = PlayerAddresses(
            taskRegistry = REGISTRY.toLong(),
            actorTileX = TILE_X.toLong(),
            actorTileY = TILE_Y.toLong(),
        )
    }
}
