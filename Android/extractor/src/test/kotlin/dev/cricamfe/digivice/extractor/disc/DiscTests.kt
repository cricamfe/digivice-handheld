package dev.cricamfe.digivice.extractor.disc

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables
import org.junit.jupiter.api.io.TempDir
import java.io.EOFException
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DiscTests {
    @Test fun `iso sector maps to lba times 2048`() {
        val bytes = image()
        bytes[30 * 2048 + 7] = 42
        openDisc(MemorySource(bytes))!!.use { assertEquals(42, it.read(30, 8)[7].toInt()) }
    }

    @Test fun `bin reads across sectors after the 24 byte headers`() {
        val iso = image()
        iso[30 * 2048 + 7] = 42
        iso[31 * 2048 + 2] = 43
        openDisc(MemorySource(raw(iso)))!!.use {
            val bytes = it.read(30, 2051)
            assertEquals(42, bytes[7].toInt())
            assertEquals(43, bytes[2050].toInt())
        }
    }

    @Test fun `format is detected by contents despite swapped extensions`(@TempDir temp: File) {
        val rawIso = File(temp, "raw.iso").apply { writeBytes(raw(image())) }
        val logicalBin = File(temp, "logical.bin").apply { writeBytes(image()) }
        listOf(rawIso, logicalBin).forEach { file ->
            FileByteSource(file).use { source ->
                openDisc(source)!!.use { assertEquals("CD001", it.read(16, 6).copyOfRange(1, 6).decodeToString()) }
            }
        }
    }

    @Test fun `iso9660 enumerates hidden path table directories and strips file versions`() {
        openDisc(MemorySource(image()))!!.use { disc ->
            val files = DiscFiles(disc)
            assertEquals(DiscFile("HIDDEN/TEST.DAT", 25, 4), files.byPath("hidden/test.dat;1"))
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), files.read(files.atLba(25)!!))
            assertNull(files.byPath("missing"))
        }
    }

    @Test fun `another boot executable is WrongGame`() {
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(image("BOOT = cdrom:\\SLUS_000.00;1\r\n"))))
    }

    @Test fun `correct boot executable name with wrong hash is WrongGame`() {
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(image())))
    }

    @Test fun `boot name in a comment does not identify the game`() {
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(image("# SLES_039.36\r\nBOOT = cdrom:\\SLUS_000.00;1"))))
    }

    @Test fun `a raw bin cut in half is Truncated`() {
        val bytes = raw(image()).copyOf(32 * 2352)
        assertEquals(GameCheck.Truncated, checkGame(MemorySource(bytes)))
    }

    @Test fun `a partial raw sector is Truncated`() {
        assertEquals(GameCheck.Truncated, checkGame(MemorySource(raw(image()).dropLast(1).toByteArray())))
    }

    @Test fun `a random file is NotADisc`() {
        assertEquals(GameCheck.NotADisc, checkGame(MemorySource(ByteArray(100_000) { 23 })))
    }

    @Test fun `out of range reads fail before allocation`() {
        openDisc(MemorySource(image()))!!.use { disc ->
            assertThrows(IllegalArgumentException::class.java) { disc.read(-1, 20) }
            assertThrows(IllegalArgumentException::class.java) { disc.read(0, Int.MAX_VALUE) }
            assertThrows(EOFException::class.java) { disc.read(Int.MAX_VALUE, 2048) }
        }
    }

    @Test fun `cyclic path table parent is rejected`() {
        val bytes = image()
        put16(bytes, 18 * 2048 + 10 + 6, 2)
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(bytes)))
    }

    @Test fun `aliased directory extents are rejected`() {
        val bytes = image()
        put32(bytes, 18 * 2048 + 10 + 2, 20)
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(bytes)))
    }

    @Test fun `invalid raw sector mode is rejected`() {
        val bytes = raw(image())
        bytes[20 * 2352 + 15] = 1
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(bytes)))
    }

    @Test fun `an incomplete logical image is Truncated`() {
        assertEquals(GameCheck.Truncated, checkGame(MemorySource(image().dropLast(1).toByteArray())))
    }

    @Test fun `directory record length is validated`() {
        val bytes = image()
        bytes[20 * 2048] = 5
        assertEquals(GameCheck.WrongGame, checkGame(MemorySource(bytes)))
    }

    @Test fun `oversized directory and path table are rejected without allocation`() {
        listOf(132, 156 + 10).forEach { offset ->
            val bytes = image()
            put32(bytes, 16 * 2048 + offset, Int.MAX_VALUE)
            assertEquals(GameCheck.WrongGame, checkGame(MemorySource(bytes)))
        }
    }

    @Test fun `file extent beyond the image is Truncated`() {
        val bytes = image()
        put32(bytes, 21 * 2048 + 68 + 2, 999)
        assertEquals(GameCheck.Truncated, checkGame(MemorySource(bytes)))
    }

    @Test fun `checkGame borrows its source and a disc owns its source`() {
        val source = MemorySource(image())
        checkGame(source)
        assertFalse(source.closed)
        openDisc(source)!!.close()
        assertTrue(source.closed)
    }

    @Test fun `file byte source checks offsets and reads exactly`(@TempDir temp: File) {
        FileByteSource(File(temp, "data").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }).use { source ->
            val out = ByteArray(4)
            source.read(1, out, 1, 3)
            assertArrayEquals(byteArrayOf(0, 2, 3, 4), out)
            assertThrows(EOFException::class.java) { source.read(2, out, 0, 3) }
            assertThrows(IndexOutOfBoundsException::class.java) { source.read(0, out, 3, 2) }
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
    fun `the real iso is Valid and its table points at SnnnPACK files`() {
        FileByteSource(File(System.getenv("DW3_ISO"))).use { source ->
            assertEquals(GameCheck.Valid, checkGame(source))
            openDisc(source)!!.use { disc ->
                val files = DiscFiles(disc)
                val table = GameFileTable(files)
                val mapPaths = (0 until 2382).mapNotNull { files.atLba(table.lbaOf(it))?.path }
                    .filter { Regex(".*/S[0-9]{3}PACK\\.BIN").matches(it) }
                assertTrue(mapPaths.any { it.endsWith("S331PACK.BIN") }, mapPaths.take(5).toString())
                assertTrue(mapPaths.any { it.endsWith("S206PACK.BIN") })
                assertThrows(IndexOutOfBoundsException::class.java) { table.lbaOf(2382) }
            }
        }
    }

    @Test
    @EnabledIfEnvironmentVariables(value = [
        EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+"),
        EnabledIfEnvironmentVariable(named = "DW3_BIN", matches = ".+"),
    ])
    fun `the real bin is Valid and 1000 samples equal the iso`() {
        FileByteSource(File(System.getenv("DW3_BIN"))).use { binSource ->
            assertEquals(GameCheck.Valid, checkGame(binSource))
            openDisc(binSource)!!.use { bin ->
                FileByteSource(File(System.getenv("DW3_ISO"))).use { isoSource ->
                    openDisc(isoSource)!!.use { iso ->
                        val sectors = (isoSource.size / 2048).toInt()
                        repeat(1000) { i ->
                            val lba = (i.toLong() * (sectors - 3) / 999).toInt()
                            assertArrayEquals(iso.read(lba, 4097), bin.read(lba, 4097), "LBA $lba")
                        }
                    }
                }
            }
        }
    }
}

private class MemorySource(private val bytes: ByteArray) : ByteSource {
    var closed = false
    override val size: Long get() = bytes.size.toLong()
    override fun read(position: Long, into: ByteArray, offset: Int, length: Int) {
        if (position < 0 || position > size - length) throw EOFException()
        bytes.copyInto(into, offset, position.toInt(), position.toInt() + length)
    }
    override fun close() { closed = true }
}

private fun put32(bytes: ByteArray, offset: Int, value: Int) {
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
}
private fun put16(bytes: ByteArray, offset: Int, value: Int) {
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value.toShort())
}
private fun record(lba: Int, size: Int, name: ByteArray, directory: Boolean = false): ByteArray {
    val bytes = ByteArray(33 + name.size + if (name.size % 2 == 0) 1 else 0)
    bytes[0] = bytes.size.toByte()
    put32(bytes, 2, lba)
    put32(bytes, 10, size)
    bytes[25] = if (directory) 2 else 0
    bytes[32] = name.size.toByte()
    name.copyInto(bytes, 33)
    return bytes
}
private fun image(boot: String = "BOOT = cdrom:\\SLES_039.36;1\r\n"): ByteArray {
    val bytes = ByteArray(512 * 2048)
    val pvd = 16 * 2048
    bytes[pvd] = 1
    "CD001".toByteArray().copyInto(bytes, pvd + 1)
    bytes[pvd + 6] = 1
    put32(bytes, pvd + 80, 512)
    put16(bytes, pvd + 128, 2048)
    put32(bytes, pvd + 132, 24)
    put32(bytes, pvd + 140, 18)
    record(20, 2048, byteArrayOf(0), true).copyInto(bytes, pvd + 156)
    fun path(offset: Int, lba: Int, parent: Int, name: ByteArray) {
        val pos = 18 * 2048 + offset
        bytes[pos] = name.size.toByte()
        put32(bytes, pos + 2, lba)
        put16(bytes, pos + 6, parent)
        name.copyInto(bytes, pos + 8)
    }
    path(0, 20, 1, byteArrayOf(0))
    path(10, 21, 1, "HIDDEN".toByteArray())
    fun directory(lba: Int, entries: List<ByteArray>) {
        var pos = lba * 2048
        (listOf(record(lba, 2048, byteArrayOf(0), true), record(20, 2048, byteArrayOf(1), true)) + entries).forEach {
            it.copyInto(bytes, pos)
            pos += it.size
        }
    }
    directory(20, listOf(record(23, boot.length, "SYSTEM.CNF;1".toByteArray()), record(24, 923648, "SLES_039.36;1".toByteArray())))
    directory(21, listOf(record(25, 4, "TEST.DAT;1".toByteArray())))
    boot.toByteArray().copyInto(bytes, 23 * 2048)
    byteArrayOf(1, 2, 3, 4).copyInto(bytes, 25 * 2048)
    return bytes
}
private fun raw(iso: ByteArray): ByteArray {
    val bytes = ByteArray(iso.size / 2048 * 2352)
    repeat(iso.size / 2048) { lba ->
        val offset = lba * 2352
        for (i in 1..10) bytes[offset + i] = 0xff.toByte()
        bytes[offset + 15] = 2
        iso.copyInto(bytes, offset + 24, lba * 2048, (lba + 1) * 2048)
    }
    return bytes
}
