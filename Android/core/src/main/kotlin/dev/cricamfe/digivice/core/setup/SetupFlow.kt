package dev.cricamfe.digivice.core.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** The screen of the preparation page; the page localizes everything from it. */
@Serializable
enum class SetupPhase {
    @SerialName("choose") Choose,
    @SerialName("checking") Checking,
    @SerialName("working") Working,
    @SerialName("waiting") WaitingForGame,
    @SerialName("pausing") Pausing,
    @SerialName("paused") Paused,
    @SerialName("failed") Failed,
    @SerialName("ready") Ready,
}

/** What the page's single button does; the page sends it back through the bridge. */
@Serializable
enum class SetupAction {
    @SerialName("choose") Choose,
    @SerialName("chooseOther") ChooseOther,
    @SerialName("pause") Pause,
    @SerialName("resume") Resume,
    @SerialName("retry") Retry,
    @SerialName("continue") Continue,
    ;

    /** The name the page knows it by. */
    val wireName: String get() = serializer().descriptor.getElementName(ordinal)

    companion object {
        /** The action the page sent back, or null for a name it should not know. */
        fun named(name: String): SetupAction? = entries.firstOrNull { it.wireName == name }
    }
}

/**
 * The state pushed to the preparation page (`?launcher&setup`). [progress] is the overall share from 0 to 1, or null
 * when it is not known (checking the file, or a pause carried over from an earlier process). [done] and [total] count
 * the resources of the current [piece]. [enabled] is false while the button's action is already under way.
 */
@Serializable
data class SetupPage(
    val phase: SetupPhase,
    val action: SetupAction,
    val enabled: Boolean = true,
    val file: String? = null,
    val piece: String? = null,
    val done: Int = 0,
    val total: Int = 0,
    val progress: Double? = null,
    val problem: SetupError? = null,
    val language: String? = null,
) {
    /** The screen stays on while the console works, not while it waits for the user or for RetroArch. */
    val keepsScreenOn: Boolean
        get() = phase == SetupPhase.Checking || phase == SetupPhase.Working || phase == SetupPhase.Pausing

    fun toJson(): String = json.encodeToString(this)

    private companion object {
        val json = Json {
            encodeDefaults = true
            explicitNulls = true
        }
    }
}

/**
 * What the preparation page shows, as a function of the service's state and what the activity knows. Pure state: the
 * activity calls [page] on every refresh and pushes the result when it changes.
 *
 * It remembers the last overall progress, so a pause (for RetroArch or the user) keeps the bar where it was.
 *
 * Not thread safe: use it from the main thread.
 */
class SetupFlow {
    private var lastProgress: Double? = null

    /**
     * [service] is the service's state; [localFailure] a failure the activity met before the service ran (no picker, a
     * lost grant). [readyAtOpen]: a published pack existed when the activity opened. [hasSavedDisc]: a chosen file is
     * still granted, so the work can resume without choosing it again. [file] is its display name, when known.
     */
    fun page(
        service: ExtractionState,
        localFailure: SetupError? = null,
        readyAtOpen: Boolean = false,
        hasSavedDisc: Boolean = false,
        file: String? = null,
        language: String? = null,
    ): SetupPage {
        val state = when {
            localFailure != null -> ExtractionState.Failed(localFailure)
            service == ExtractionState.Idle && readyAtOpen -> ExtractionState.Ready
            else -> service
        }
        val shownFile = file?.takeIf { it.isNotBlank() }
        val page = when (state) {
            // An idle service with a granted file is work an earlier process left half done.
            ExtractionState.Idle, ExtractionState.Paused ->
                if (hasSavedDisc) SetupPage(SetupPhase.Paused, SetupAction.Resume, file = shownFile, progress = lastProgress)
                else SetupPage(SetupPhase.Choose, SetupAction.Choose)
            ExtractionState.Checking -> {
                lastProgress = null
                SetupPage(SetupPhase.Checking, SetupAction.Pause, file = shownFile)
            }
            is ExtractionState.Progress -> {
                val overall = overallProgress(state)
                lastProgress = overall
                SetupPage(
                    SetupPhase.Working, SetupAction.Pause, file = shownFile, piece = state.piece,
                    done = state.done, total = state.total, progress = overall,
                )
            }
            ExtractionState.PausedForGame ->
                SetupPage(SetupPhase.WaitingForGame, SetupAction.Pause, file = shownFile, progress = lastProgress)
            ExtractionState.Cancelling ->
                SetupPage(SetupPhase.Pausing, SetupAction.Pause, enabled = false, file = shownFile, progress = lastProgress)
            ExtractionState.Ready -> SetupPage(SetupPhase.Ready, SetupAction.Continue, file = shownFile, progress = 1.0)
            is ExtractionState.Failed -> {
                val retry = hasSavedDisc && (state.reason == SetupError.NoSpace || state.reason == SetupError.Interrupted)
                SetupPage(
                    SetupPhase.Failed, if (retry) SetupAction.Retry else SetupAction.ChooseOther,
                    file = shownFile, problem = state.reason,
                )
            }
        }
        return page.copy(language = language)
    }

    companion object {
        /** The extractor's pieces in order, with what each took on the AYN Thor (seconds), to share out the bar. */
        private val pieceSeconds = linkedMapOf(
            "Text" to 1.0, "Cards" to 0.3, "Sprites" to 1.4, "Icons" to 0.1, "World" to 0.3, "Maps" to 41.0, "Models" to 70.0,
        )

        /**
         * The overall share of [progress], weighted by the time each piece takes, so the bar moves at an even pace. A
         * set of pieces this table does not know gets equal shares. Only a published pack is 100%, so this stops at 99%.
         */
        fun overallProgress(progress: ExtractionState.Progress): Double {
            val fraction = if (progress.total <= 0) 0.0 else progress.done.toDouble() / progress.total
            val names = pieceSeconds.keys.toList()
            val known = progress.pieces == names.size && names.getOrNull(progress.index - 1) == progress.piece
            val share = if (known) {
                val before = names.take(progress.index - 1).sumOf { pieceSeconds.getValue(it) }
                (before + pieceSeconds.getValue(progress.piece) * fraction) / pieceSeconds.values.sum()
            } else {
                (progress.index - 1 + fraction) / progress.pieces.coerceAtLeast(1)
            }
            return share.coerceIn(0.0, 0.99)
        }
    }
}
