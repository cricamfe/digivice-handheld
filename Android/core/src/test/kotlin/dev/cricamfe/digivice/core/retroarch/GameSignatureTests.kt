package dev.cricamfe.digivice.core.retroarch

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.zip.CRC32

class GameSignatureTests {
    private val fake = FakeRetroArch()
    private val client = RetroArchClient("127.0.0.1", fake.port, 300)

    @AfterEach
    fun tearDown() {
        client.close()
        fake.close()
    }

    private fun crcOf(ram: ByteArray): Long {
        val crc = CRC32()
        crc.update(ram, 0x10105, 0x3BAE3 - 0x10105 + 1)
        return crc.value
    }

    // ---- on the real RAM of the game: the dumps, which are not in the repository ----------------------------------

    @Test
    fun `the constant is the crc of the region on every snapshot and on the title screen`() {
        assertEquals(178655, GameSignature.LENGTH)
        val rams = Rams.snapshotNames.map { it to Rams.snapshot(it) } + ("thor-title" to Rams.title)
        assertEquals(19, rams.size)

        for ((name, ram) in rams) {
            assertEquals(GameSignature.CRC32, crcOf(ram), name)
        }
        assertEquals(0x5C09DC6DL, GameSignature.CRC32)
    }

    @Test
    fun `signature accepts the game`() {
        for (name in Rams.snapshotNames) {
            fake.ram = Rams.snapshot(name)
            assertTrue(GameSignature.matches(client), name)
        }
        fake.ram = Rams.title
        assertTrue(GameSignature.matches(client), "thor-title")
    }

    @Test
    fun `signature accepts Flawe Mod 2 before and after initialization and rejects further changes`() {
        fake.ram = Rams.game.copyOf().apply {
            // The four edits in the signed region of Flawe's Combined 2.0 patch.
            byteArrayOf(0x40, 0x2D).copyInto(this, 0x12228)
            byteArrayOf(0x80.toByte(), 0x2D, 0x00, 0x0C).copyInto(this, 0x13100)
            this[0x1CBCF] = 0x16
            byteArrayOf(0xCC.toByte(), 0x91.toByte(), 0x02, 0x08, 0, 0, 0, 0).copyInto(this, 0x1D608)
        }
        assertEquals(0x520643ABL, crcOf(fake.ram))
        assertTrue(GameSignature.matches(client))

        // The startup hook restores these two original instructions once the mod is initialized.
        Rams.game.copyInto(fake.ram, 0x1D608, 0x1D608, 0x1D610)
        assertEquals(0x2E6961FEL, crcOf(fake.ram))
        assertTrue(GameSignature.matches(client))

        fake.ram[0x10105] = (fake.ram[0x10105] + 1).toByte()
        assertFalse(GameSignature.matches(client))
    }

    // ---- on synthetic RAM --------------------------------------------------------------------------------------

    @Test
    fun `signature accepts a RAM with any of the three checksums of the game`() {
        for (crc in listOf(GameSignature.CRC32, GameSignature.FLAWE_MOD_2_CRC32, GameSignature.FLAWE_MOD_2_STARTUP_CRC32)) {
            fake.ram = SyntheticRam().build(signature = crc)
            assertEquals(crc, crcOf(fake.ram))
            assertTrue(GameSignature.matches(client), "0x${crc.toString(16)}")
        }
        fake.ram = SyntheticRam().build(signature = 0x12345678L)
        assertFalse(GameSignature.matches(client), "a checksum of no version of the game")
    }

    @Test
    fun `signature reads the region in requests of at most 16 KB`() {
        fake.ram = SyntheticRam.game()
        GameSignature.matches(client)

        val reads = fake.commands.map { it.split(' ') }
        assertEquals(11, reads.size)
        assertTrue(reads.all { it[0] == "READ_CORE_MEMORY" && it[2].toInt() <= 16384 })
        assertEquals("10105", reads.first()[1].lowercase())
        assertEquals(GameSignature.LENGTH, reads.sumOf { it[2].toInt() })
    }

    @Test
    fun `signature rejects other RAM`() {
        fake.ram = ByteArray(FakeRetroArch.RAM_SIZE)

        assertFalse(GameSignature.matches(client))
    }

    @Test
    fun `one changed byte is rejected`() {
        val game = SyntheticRam.game()
        fake.ram = game
        assertTrue(GameSignature.matches(client))

        fake.ram = game.copyOf().also { it[0x3BAE3] = (it[0x3BAE3] + 1).toByte() }
        assertFalse(GameSignature.matches(client))

        fake.ram = game.copyOf().also { it[0x10105] = (it[0x10105] + 1).toByte() }
        assertFalse(GameSignature.matches(client))
    }

    @Test
    fun `a refused or lost read is not a mismatch`() {
        fake.memoryMap = false
        assertThrows<MemoryReadRefused> { GameSignature.matches(client) }

        fake.memoryMap = true
        fake.silent = true
        assertThrows<MemoryReadLost> { GameSignature.matches(RetroArchClient("127.0.0.1", fake.port, 30)) }
    }
}
