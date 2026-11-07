package dev.cricamfe.digivice.core.retroarch

/**
 * The `errorCode` values of the health events (`HealthDto.errorCode`). The first five say why the loop cannot read the
 * game; the last two are the ones the C# `EmulatorConnectionErrorCodes` already had.
 */
object ConnectionCodes {
    /** RetroArch does not answer `VERSION`: it is closed, the network commands are off, or the network is down. */
    const val RETROARCH_UNREACHABLE = "retroarch_unreachable"

    /** RetroArch is open but has no content loaded. */
    const val NO_CONTENT = "no_content"

    /** RetroArch answers, but refuses to hand out the core's memory (a build without the memory map fallback). */
    const val MEMORY_UNAVAILABLE = "memory_unavailable"

    /** The RAM does not carry the signature of Digimon World 2003. */
    const val WRONG_GAME = "wrong_game"

    /** RetroArch is running a game of another console: the player is on something else. */
    const val OTHER_CONSOLE = "other_console"

    /** The game is running but no save is loaded yet (the title screen): the party is empty. */
    const val WAITING_FOR_SAVE = "waiting_for_save"

    /** A read failed while the state was being composed. */
    const val MEMORY_READ_FAILED = "memory_read_failed"

    /** Anything else failed while the state was being composed. */
    const val STATE_COMPOSE_FAILED = "state_compose_failed"
}
