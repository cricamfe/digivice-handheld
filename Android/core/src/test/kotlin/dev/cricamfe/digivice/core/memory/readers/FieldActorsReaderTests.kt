package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.MemoryReader
import dev.cricamfe.digivice.core.memory.SnapshotAccessor
import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.readers.FieldActorsReader.Actor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FieldActorsReaderTests {
    // A RAM of 2 MiB behind a real MemoryReader, so a read outside it would throw, as it does on the device.
    private class Ram {
        val bytes = ByteArray(0x200000)
        val reader = FieldActorsReader(MemoryReader(SnapshotAccessor(bytes)), TestEnvironment.createAddressesRepository())

        fun put(address: Int, value: Int) {
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(address, value)
        }

        fun register(index: Int, pointer: Long) = put(REGISTRY + index * 4, pointer.toInt())

        /** A task at `address` (RAM address); the defaults make a character of the map the game draws. */
        fun task(
            address: Int,
            id: Int = 5,
            key1: Int = 0x9D,
            key2: Int = 1,
            state: Int = 1,
            animFile: Int = 0x0412002D,
            x: Int = 1072,
            y: Int = 665,
            dir: Int = 1,
        ) {
            put(address, id)
            put(address + 4, key1)
            put(address + 8, key2)
            put(address + 0x0C, state)
            put(address + 0x58, x)
            put(address + 0x5C, y)
            put(address + 0x60, dir)
            put(address + 0x9C, animFile)
        }
    }

    @Test
    fun `read lists the characters of the map the registry points to, in its order`() {
        val ram = Ram().apply {
            task(0xAB8EC, id = 7, key1 = 0, key2 = 0)                                  // the field's task
            register(0, 0x800AB8EC)
            task(0xACD14, key1 = 2, key2 = 0, x = 1454, y = 274)                       // the leader
            register(2, 0x800ACD14)
            task(0xB0000, key1 = 0x6, key2 = 2, x = 1450, y = 270)                     // a partner
            register(3, 0x800B0000)
            task(0xB0200, key1 = 0x9D, x = 1072, y = 665, dir = 1)
            register(5, 0x800B0200)
            task(0xB0400, key1 = 0x9E, x = 864, y = 577, dir = 5)
            register(6, 0x800B0400)
        }

        assertEquals(listOf(Actor(0x9D, 1072, 665, 1), Actor(0x9E, 864, 577, 5)), ram.reader.read())
    }

    @Test
    fun `read leaves out the leader and the three partners`() {
        val ram = Ram().apply {
            for ((index, kind) in listOf(0, 2, 4, 8).withIndex()) {
                task(0xB0000 + index * 0x200, key1 = 2 + index, key2 = kind)
                register(index, 0x800B0000L + index * 0x200)
            }
        }

        assertEquals(emptyList<Actor>(), ram.reader.read())
    }

    @Test
    fun `read leaves out the actors the game does not draw`() {
        val ram = Ram().apply {
            task(0xB0000, key1 = 1, state = 0)                // still starting: its state is not TASK_RUN
            task(0xB0200, key1 = 2, state = 3)                // being killed
            task(0xB0400, key1 = 3, animFile = 0)             // no animation file: a sign or an invisible one
            task(0xB0600, key1 = 4, x = 0, y = 0)             // its tile is (0, 0)
            task(0xB0800, key1 = 5, id = 4)                   // not an actor
            task(0xB0A00, key1 = 6, x = 0, y = 12)            // one coordinate at 0 is a place
            for (index in 0 until 6) register(index, 0x800B0000L + index * 0x200)
        }

        assertEquals(listOf(Actor(6, 0, 12, 1)), ram.reader.read())
    }

    @Test
    fun `read skips null entries and gives nothing out of the field`() {
        val ram = Ram().apply {
            task(0xAB8E4, id = 12, key1 = 0, key2 = 0)
            register(0, 0x800AB8E4)
        }

        assertEquals(emptyList<Actor>(), ram.reader.read())
        assertEquals(emptyList<Actor>(), Ram().reader.read())
    }

    @Test
    fun `read never reads a pointer that is not an address of the RAM`() {
        val ram = Ram().apply {
            // Not KSEG0, beyond the 2 MiB, null in KSEG0, not word aligned, and too close to the end for the actor.
            for ((index, pointer) in listOf(0x000ACD14L, 0xFFFFFFFFL, 0x80200000L, 0x80000000L, 0x800ACD15L, 0x801FFFFCL, 0x801FFF70L)
                .withIndex()) {
                register(index, pointer)
            }
            task(0xB0000, key1 = 0x9D)
            register(7, 0x800B0000)
        }

        assertEquals(listOf(Actor(0x9D, 1072, 665, 1)), ram.reader.read())
    }

    @Test
    fun `read is bounded by the 100 entries of the registry`() {
        val last = Ram().apply {
            task(0xB0000, key1 = 0x9D)
            register(99, 0x800B0000)
        }
        val past = Ram().apply {
            task(0xB0000, key1 = 0x9D)
            register(100, 0x800B0000)         // the word after the registry's tasks
        }

        assertEquals(1, last.reader.read().size)
        assertEquals(0, past.reader.read().size)
    }

    @Test
    fun `read lists a hundred actors without trouble`() {
        val ram = Ram().apply {
            for (index in 0 until 100) {
                task(0xB0000 + index * 0x200, key1 = 100 + index, x = index + 1, y = 7)
                register(index, 0x800B0000L + index * 0x200)
            }
        }

        assertEquals(100, ram.reader.read().size)
    }

    private companion object {
        const val REGISTRY = 0x4B610
    }
}
