package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.TestEnvironment

/**
 * The RAM dumps of the game, for the tests that check the real memory against the code: the 18 scanner snapshots and the
 * title screen dumped on the Thor. A test that reads one is skipped without it; the behaviour of the client and the loop
 * is tested on [SyntheticRam] instead.
 */
object Rams {
    val snapshotNames: List<String> get() = TestEnvironment.snapshotNames()

    fun snapshot(name: String): ByteArray = TestEnvironment.ramDump("${TestEnvironment.SNAPSHOTS}/$name.bin").readBytes()

    /** RAM of the real console at the title screen: the game is loaded but no save is, so the party is empty. */
    val title: ByteArray by lazy { TestEnvironment.ramDump("Tools/RetroArchProbe/thor-title.bin").readBytes() }

    /** A snapshot of a game in progress, the one the other tests use. */
    const val GAME = "antes-patrulheiro-opcional-1-centro-admin-subsolo-andar-1"

    val game: ByteArray by lazy { snapshot(GAME) }
}
