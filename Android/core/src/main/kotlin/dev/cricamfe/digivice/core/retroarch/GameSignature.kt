package dev.cricamfe.digivice.core.retroarch

/**
 * What tells Digimon World 2003 (PAL, `SLES-03936`) apart from any other game in the emulated RAM: the CRC32 of the
 * program's constant region, [START] to [END] inclusive. It is the same on the 18 snapshots of
 * `Tools/MemoryScanner/Snapshots` and on the RAM dumped from the real console at the title screen, so it holds before a
 * save is loaded. `GameSignatureTests` recomputes [CRC32] from those files. Flawe Mod 2.0 Combined has its own
 * [FLAWE_MOD_2_CRC32], verified on the console. Its [FLAWE_MOD_2_STARTUP_CRC32] covers the patched PAL executable before
 * initialization restores the two instructions of the startup hook.
 */
object GameSignature {
    const val START = 0x10105L
    const val END = 0x3BAE3L
    const val LENGTH = (END - START + 1).toInt()
    const val CRC32 = 0x5C09DC6DL
    const val FLAWE_MOD_2_CRC32 = 0x2E6961FEL
    const val FLAWE_MOD_2_STARTUP_CRC32 = 0x520643ABL

    /** The CRC32 of the region, reading it with [read] in requests of at most [RetroArchClient.MAX_READ_BYTES]. */
    fun checksum(read: (address: Long, size: Int) -> ByteArray): Long {
        val crc = java.util.zip.CRC32()
        var address = START
        while (address <= END) {
            val size = minOf(RetroArchClient.MAX_READ_BYTES.toLong(), END - address + 1).toInt()
            crc.update(read(address, size))
            address += size
        }
        return crc.value
    }

    /**
     * Whether the RAM RetroArch serves is the game's. Reads the region straight through [client], not through the page
     * cache: it is 11 requests of 16 KB that would only fill the cache with pages nobody else needs.
     *
     * @throws MemoryReadRefused if RetroArch will not hand out the memory.
     * @throws MemoryReadLost if a reply did not arrive (it says nothing about the game).
     */
    fun matches(client: RetroArchClient): Boolean {
        val crc = checksum(client::read)
        return crc == CRC32 || crc == FLAWE_MOD_2_CRC32 || crc == FLAWE_MOD_2_STARTUP_CRC32
    }
}
