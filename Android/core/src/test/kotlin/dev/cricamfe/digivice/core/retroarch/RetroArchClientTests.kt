package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.MemoryReadException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.DatagramSocket

class RetroArchClientTests {
    private val fake = FakeRetroArch(ram = SyntheticRam.pattern())
    private val clients = mutableListOf<RetroArchClient>()

    private fun client(timeoutMs: Int = 300, port: Int = fake.port) =
        RetroArchClient("127.0.0.1", port, timeoutMs).also { clients += it }

    @AfterEach
    fun tearDown() {
        clients.forEach { it.close() }
        fake.close()
    }

    @Test
    fun `tells a closed port from a silent one`() {
        val free = DatagramSocket().use { it.localPort }
        val closed = client(port = free)
        assertNull(closed.portState, "nothing asked yet")
        assertNull(closed.version())
        assertEquals(true, closed.portState, "nothing listens there")

        DatagramSocket().use { silent ->
            val waiting = client(timeoutMs = 100, port = silent.localPort)
            assertNull(waiting.version())
            assertEquals(false, waiting.portState, "RetroArch in the background: there, but it does not answer")
        }

        val answering = client()
        answering.version()
        assertEquals(false, answering.portState)
    }

    @Test
    fun `parses memory reply`() {
        fake.ram[0x48DA0] = 0xB3.toByte()
        fake.ram[0x48DA1] = 0
        fake.ram[0x48DA2] = 0
        fake.ram[0x48DA3] = 0

        val bytes = client().read(0x48DA0, 4)

        assertArrayEquals(byteArrayOf(0xB3.toByte(), 0, 0, 0), bytes)
        assertEquals(listOf("READ_CORE_MEMORY 48DA0 4"), fake.commands)
    }

    @Test
    fun `parses a large read of the real reply format`() {
        val bytes = client().read(0x10105, 16384)

        assertArrayEquals(fake.ram.copyOfRange(0x10105, 0x10105 + 16384), bytes)
    }

    @Test
    fun `reads the version and the status`() {
        assertEquals("1.22.2", client().version())
        assertEquals(RetroArchStatus.Playing(FakeRetroArch.DEFAULT_TITLE), client().status())

        fake.state = "PAUSED"
        assertEquals(RetroArchStatus.Paused(FakeRetroArch.DEFAULT_TITLE), client().status())

        fake.state = "CONTENTLESS"
        assertEquals(RetroArchStatus.Contentless, client().status())
    }

    @Test
    fun `refused read throws with reason`() {
        fake.memoryMap = false

        val refused = assertThrows<MemoryReadRefused> { client().read(0x48DA0, 4) }

        assertEquals("no memory map defined", refused.reason)
        assertEquals(0x48DA0L, refused.address)
        val asReadException: MemoryReadException = refused  // it passes through MemoryReader unchanged
        assertEquals(refused, asReadException)
    }

    @Test
    fun `read outside the ram is refused with the descriptor reason`() {
        val refused = assertThrows<MemoryReadRefused> { client().read(FakeRetroArch.RAM_SIZE.toLong(), 4) }

        assertEquals("no descriptor for address", refused.reason)
    }

    @Test
    fun `truncated read is refused`() {
        fake.truncateReads = true

        val refused = assertThrows<MemoryReadRefused> { client().read(0x48DA0, 16) }

        assertTrue(refused.reason.contains("TRUNCATED"), refused.reason)
    }

    @Test
    fun `a reply with fewer bytes than requested is refused`() {
        // Asks past the end of the image: the fake answers the bytes that exist, which are fewer than requested.
        val refused = assertThrows<MemoryReadRefused> { client().read(FakeRetroArch.RAM_SIZE - 2L, 8) }

        assertTrue(refused.reason.contains("2"), refused.reason)
    }

    @Test
    fun `late reply is discarded`() {
        fake.staleReplyBeforeRead = true

        val bytes = client().read(0x48DA0, 8)

        assertArrayEquals(fake.ram.copyOfRange(0x48DA0, 0x48DA8), bytes)
    }

    @Test
    fun `a duplicated reply of an earlier request is not taken as the next answer`() {
        fake.duplicateReadReplies = true
        val client = client()
        assertArrayEquals(fake.ram.copyOfRange(0x48DA0, 0x48DA4), client.read(0x48DA0, 4))

        fake.ram[0x48DA0] = 0x77

        assertEquals(0x77.toByte(), client.read(0x48DA0, 4)[0])
    }

    @Test
    fun `lost reply retries once`() {
        val client = client(timeoutMs = 60)
        fake.dropNextReads(1)

        val bytes = client.read(0x48DA0, 4)

        assertArrayEquals(fake.ram.copyOfRange(0x48DA0, 0x48DA4), bytes)
        assertEquals(2, fake.commands.count { it.startsWith("READ_CORE_MEMORY") })
    }

    @Test
    fun `two lost replies in a row throw MemoryReadLost`() {
        val client = client(timeoutMs = 60)
        fake.dropNextReads(2)

        val lost = assertThrows<MemoryReadLost> { client.read(0x48DA0, 4) }

        assertEquals(0x48DA0L, lost.address)
        val asReadException: MemoryReadException = lost
        assertEquals(lost, asReadException)
        assertEquals(2, fake.commands.count { it.startsWith("READ_CORE_MEMORY") })
    }

    @Test
    fun `silent server gives no version and an unreachable status`() {
        fake.silent = true
        val client = client(timeoutMs = 40)

        assertNull(client.version())
        assertEquals(RetroArchStatus.Unreachable, client.status())
    }

    @Test
    fun `nothing listening is treated as no reply, quickly`() {
        val deadPort = fake.port
        fake.close()
        val client = client(timeoutMs = 2000, port = deadPort)

        val started = System.nanoTime()
        assertNull(client.version())
        assertEquals(RetroArchStatus.Unreachable, client.status())
        assertThrows<MemoryReadLost> { client.read(0x48DA0, 4) }

        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(elapsedMs < 1500, "port unreachable should not wait for the timeout: $elapsedMs ms")
    }

    @Test
    fun `refuses reads above the 16384 byte limit before sending them`() {
        assertThrows<IllegalArgumentException> { client().read(0, 16385) }
        assertThrows<IllegalArgumentException> { client().read(0, 0) }
        assertThrows<IllegalArgumentException> { client().read(-1, 4) }

        assertEquals(emptyList<String>(), fake.commands)
    }

    @Test
    fun `leaves no socket open and stays closed until reopened`() {
        val client = client()
        assertEquals("1.22.2", client.version())
        assertEquals(0, client.openSockets)

        client.close()
        assertNull(client.version())
        assertThrows<MemoryReadLost> { client.read(0x48DA0, 4) }
        assertEquals(0, client.openSockets)
        assertEquals(listOf("VERSION"), fake.commands)

        client.reopen()
        assertEquals("1.22.2", client.version())
    }

    @Test
    fun `close interrupts a request that is waiting for a reply`() {
        fake.silent = true
        val client = client(timeoutMs = 5000)
        val finished = java.util.concurrent.CountDownLatch(1)
        Thread { client.version(); finished.countDown() }.start()
        while (fake.commands.isEmpty()) Thread.sleep(2)

        val started = System.nanoTime()
        client.close()

        assertTrue(finished.await(1, java.util.concurrent.TimeUnit.SECONDS), "the waiting request did not return")
        assertTrue((System.nanoTime() - started) / 1_000_000 < 1000)
        assertEquals(0, client.openSockets)
    }

    @Test
    fun `only sends the three read commands`() {
        val client = client()
        client.version()
        client.status()
        client.read(0x48DA0, 4)

        assertEquals(listOf("VERSION", "GET_STATUS", "READ_CORE_MEMORY 48DA0 4"), fake.commands)
    }

    @Test
    fun `tryRead asks once and answers null when the reply is lost`() {
        val client = client(timeoutMs = 300)
        fake.dropNextReads(1)

        val started = System.nanoTime()
        val bytes = client.tryRead(0x48DA0, 4, timeoutMs = 50)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000

        assertNull(bytes)
        assertEquals(1, fake.commands.count { it.startsWith("READ_CORE_MEMORY") })
        assertTrue(elapsedMs < 250, "it waited the given 50 ms, not the client's 300: $elapsedMs ms")
        assertEquals(0, client.openSockets)
    }

    @Test
    fun `tryRead gives the bytes, and null when RetroArch refuses`() {
        val client = client()

        assertArrayEquals(fake.ram.copyOfRange(0x48DA0, 0x48DA4), client.tryRead(0x48DA0, 4, timeoutMs = 300))
        fake.refuseReadsAfter(0)
        assertNull(client.tryRead(0x48DA0, 4, timeoutMs = 300))
    }
}
