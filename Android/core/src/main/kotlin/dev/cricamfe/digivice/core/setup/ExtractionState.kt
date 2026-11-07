package dev.cricamfe.digivice.core.setup

/** Why a preparation stopped. [needsNewDisc]: the chosen file cannot work, so it is forgotten and another is asked for. */
enum class SetupError(val needsNewDisc: Boolean = false) {
    WrongGame(true), NotADisc(true), InvalidChd(true), Truncated(true), FileGone(true), LocalFile(true),
    NoSpace, Interrupted, Failed,
}

/** What the preparation service is doing, as its process-wide state reports it. */
sealed interface ExtractionState {
    data object Idle : ExtractionState
    data object Checking : ExtractionState

    /** RetroArch has a game loaded; work resumes on its own once it closes. */
    data object PausedForGame : ExtractionState

    /** The user asked to pause and the worker is closing its files. */
    data object Cancelling : ExtractionState

    /** The user paused: the staging generation is kept and a later run goes on from it. */
    data object Paused : ExtractionState
    data object Ready : ExtractionState

    /** [piece] is the extractor's piece name (`Text`, `Cards`, ... `Models`), the [index]th of [pieces], from 1. */
    data class Progress(val piece: String, val index: Int, val pieces: Int, val done: Int, val total: Int) : ExtractionState
    data class Failed(val reason: SetupError) : ExtractionState
}
