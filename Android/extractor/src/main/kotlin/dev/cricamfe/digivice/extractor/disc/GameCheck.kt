package dev.cricamfe.digivice.extractor.disc

import java.io.EOFException
import java.security.MessageDigest

const val EXE_SHA256 = "15f37849a81b9f00e7f20d00de7a465b663a9e86b9d08b05d32dd386be95c546"

sealed interface GameCheck {
    data object Valid : GameCheck
    data object NotADisc : GameCheck
    data object WrongGame : GameCheck
    data object Truncated : GameCheck
}

/** Validation borrows the source, so callers can extract from it after success. */
fun checkGame(source: ByteSource): GameCheck = try {
    val borrowed = object : ByteSource by source { override fun close() = Unit }
    val disc = openDisc(borrowed)
    if (disc == null) GameCheck.NotADisc else disc.use {
        val files = DiscFiles(it)
        val config = files.byPath("SYSTEM.CNF")
        val exe = files.byPath("SLES_039.36")
        if (config == null || config.size !in 1..4096 || exe == null || exe.size != 923648) GameCheck.WrongGame
        else {
            val boot = files.read(config).toString(Charsets.US_ASCII)
            val hash = MessageDigest.getInstance("SHA-256").digest(files.read(exe)).joinToString("") { byte -> "%02x".format(byte) }
            if (Regex("(?im)^\\s*BOOT\\s*=\\s*cdrom:\\\\SLES_039\\.36;1\\s*$").containsMatchIn(boot) && hash == EXE_SHA256) GameCheck.Valid
            else GameCheck.WrongGame
        }
    }
} catch (_: EOFException) {
    GameCheck.Truncated
} catch (_: InvalidDisc) {
    GameCheck.WrongGame
}
