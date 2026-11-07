package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.CoreModule
import dev.cricamfe.digivice.core.memory.MemoryAccessor
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.MemoryReader
import dev.cricamfe.digivice.core.memory.SnapshotAccessor
import dev.cricamfe.digivice.core.memory.TestEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PagedMemoryAccessorTests {
    private val fake = FakeRetroArch(ram = SyntheticRam.pattern())
    private val client = RetroArchClient("127.0.0.1", fake.port, 300)
    private val accessor = PagedMemoryAccessor(client)

    private fun reads() = fake.commands.count { it.startsWith("READ_CORE_MEMORY") }

    @AfterEach
    fun tearDown() {
        client.close()
        fake.close()
    }

    @Test
    fun `pages are read once per cycle`() {
        accessor.beginCycle()

        accessor.readInt32(0x48DA0)
        accessor.readInt16(0x48DA4)
        accessor.readBytes(0x48DB0, 64)
        accessor.readBytes(0x48000, 1)
        accessor.readBytes(0x4BFFF, 1)

        assertEquals(1, reads())
        assertEquals(1, accessor.requestsInCycle)
        assertEquals(listOf("READ_CORE_MEMORY 48000 16384"), fake.commands)
    }

    @Test
    fun `a new cycle reads the page again and sees fresh data`() {
        accessor.beginCycle()
        val before = accessor.readInt32(0x48DA0)

        fake.ram[0x48DA0] = (fake.ram[0x48DA0] + 1).toByte()
        assertEquals(before, accessor.readInt32(0x48DA0), "inside a cycle the page is a snapshot")

        accessor.beginCycle()
        assertEquals(0, accessor.requestsInCycle)
        assertEquals(before + 1, accessor.readInt32(0x48DA0))
        assertEquals(1, accessor.requestsInCycle)
        assertEquals(2, reads())
    }

    @Test
    fun `reads across pages join correctly`() {
        accessor.beginCycle()

        val bytes = accessor.readBytes(0x48000 - 6, 16)

        assertArrayEquals(fake.ram.copyOfRange(0x48000 - 6, 0x48000 + 10), bytes)
        assertEquals(2, accessor.requestsInCycle)
    }

    @Test
    fun `a read of many pages joins them`() {
        accessor.beginCycle()

        val bytes = accessor.readBytes(0x44010, 40000)

        assertArrayEquals(fake.ram.copyOfRange(0x44010, 0x44010 + 40000), bytes)
        assertEquals(3, accessor.requestsInCycle)
    }

    @Test
    fun `an int that straddles two pages is little endian`() {
        accessor.beginCycle()
        val reference = SnapshotAccessor(fake.ram)

        assertEquals(reference.readInt32(0x47FFE), accessor.readInt32(0x47FFE))
        assertEquals(reference.readInt16(0x47FFF), accessor.readInt16(0x47FFF))
    }

    @Test
    fun `a failed page is not cached and the next read asks again`() {
        accessor.beginCycle()
        fake.memoryMap = false
        assertThrows<MemoryReadRefused> { accessor.readInt32(0x48DA0) }

        fake.memoryMap = true
        assertEquals(SnapshotAccessor(fake.ram).readInt32(0x48DA0), accessor.readInt32(0x48DA0))
    }

    @Test
    fun `the errors reach the memory reader unchanged`() {
        val quick = RetroArchClient("127.0.0.1", fake.port, 40)
        try {
            val reader = MemoryReader(PagedMemoryAccessor(quick))
            fake.dropNextReads(2)
            assertThrows<MemoryReadLost> { reader.readByte(0x48DA4) }

            fake.memoryMap = false
            assertThrows<MemoryReadRefused> { reader.readByte(0x48DA4) }
        } finally {
            quick.close()
        }
    }

    @Test
    fun `rejects reads that make no sense`() {
        accessor.beginCycle()

        assertThrows<MemoryReadException> { accessor.readBytes(-1, 4) }
        assertThrows<MemoryReadException> { accessor.readBytes(0, -1) }
        assertThrows<MemoryReadException> { accessor.readBytes(0x1FFFFE, 8) }
    }

    private fun module(accessor: MemoryAccessor) =
        CoreModule(accessor, TestEnvironment.definitions::readText, TestEnvironment.definitions::listFiles)

    @Test
    fun `the state composed over UDP is the one composed from the same RAM in memory`() {
        fake.ram = SyntheticRam.game()
        accessor.beginCycle()

        val state = module(accessor).stateComposer.compose()

        assertEquals(module(SnapshotAccessor(fake.ram)).stateComposer.compose(), state)
        assertEquals(SyntheticRam.START_X, state.player.positionX)
        assertTrue(accessor.requestsInCycle <= 10, "${accessor.requestsInCycle} requests")
    }

    /** On the real RAM of the game: the snapshots, which are not in the repository. */
    @Test
    fun `cycle cost is bounded`() {
        var maxRequests = 0
        var worstSnapshot = ""
        val perSnapshot = mutableListOf<String>()
        val module = CoreModule(
            accessor,
            TestEnvironment.definitions::readText,
            TestEnvironment.definitions::listFiles,
        )

        for (name in Rams.snapshotNames) {
            fake.ram = Rams.snapshot(name)
            accessor.beginCycle()

            val state = module.stateComposer.compose()

            val expected = CoreModule(
                SnapshotAccessor(fake.ram),
                TestEnvironment.definitions::readText,
                TestEnvironment.definitions::listFiles,
            ).stateComposer.compose()
            assertEquals(expected, state, "state composed over UDP differs from the snapshot's for $name")
            perSnapshot += "${accessor.requestsInCycle}"
            if (accessor.requestsInCycle > maxRequests) {
                maxRequests = accessor.requestsInCycle
                worstSnapshot = name
            }
            assertTrue(accessor.requestsInCycle <= 10, "$name needed ${accessor.requestsInCycle} requests")
        }

        println("cycle cost: max requestsInCycle = $maxRequests (worst: $worstSnapshot); per snapshot: $perSnapshot")
    }
}
