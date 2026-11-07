package dev.cricamfe.digivice.core

/**
 * When the overlay is on screen and when the core should be reading RetroArch. Pure state: the caller says what
 * happened and when ([nowMs] is any monotonic clock, in milliseconds) and asks [shouldShow] / [shouldPoll].
 *
 * - The overlay appears when RetroArch answers ([onReachable]) and goes away when it has not answered for
 *   [hideAfterMs] ([onUnreachable]). The countdown counts from the first report and a repeated report does not
 *   restart it. Nothing announces the passing of time, so the caller schedules a call to [onTick] at [hideDeadlineMs].
 * - The console asleep is not an unreachable RetroArch (it does not answer while asleep). [onDisplayChanged] with
 *   `false` stops the polling and drops the countdown; with `true` it polls again and the countdown, if RetroArch is
 *   still unreachable, starts over at the next [onTick]. That is never sooner than if the timer had only been paused.
 * - The user decides over all of it: after [onUserHide] the overlay stays away whatever RetroArch does (the polling
 *   goes on, so the loop still notices it) until [onUserShow], which shows it at once and, if RetroArch is not
 *   answering, gives it the usual grace period.
 *
 * - RetroArch closed is not RetroArch silent: in the background it does not answer, but it keeps its port. Once the port
 *   was open and stays closed for [hideAfterMs] ([onPortState]), [shouldExit] says there is nothing left to wait for. A
 *   RetroArch not opened yet is waited for: the app may be opened first.
 *
 * Not thread safe: use it from one thread.
 */
class OverlayPolicy(private val hideAfterMs: Long = DEFAULT_HIDE_AFTER_MS) {
    /** What the policy alone wants, ignoring the user: set by an answer, cleared by the end of a grace period. */
    private var visibleByReachability = false

    /** The last thing known about RetroArch; null until the first report. */
    private var reachable: Boolean? = null

    /** When the current stretch without an answer started; null while no stretch is being counted. */
    private var unreachableSinceMs: Long? = null

    private var displayOn = true

    /** RetroArch's port was seen open since the app started. */
    private var portSeenOpen = false

    /** When the port was found closed, while it stays so; null while no stretch is being counted. */
    private var portClosedSinceMs: Long? = null

    /** RetroArch was here and it has been closed: the app has nothing left to do. */
    var shouldExit = false
        private set

    /** The user asked to hide the overlay and has not asked to show it again. */
    var userHidden = false
        private set

    /** The overlay should be on screen. */
    val shouldShow: Boolean get() = visibleByReachability && !userHidden

    /** The core should be reading RetroArch. False only while the display is off. */
    val shouldPoll: Boolean get() = displayOn

    /** When [onTick] will hide the overlay if RetroArch has not answered by then; null when no countdown is running. */
    val hideDeadlineMs: Long? get() = unreachableSinceMs?.plus(hideAfterMs)

    fun onReachable(nowMs: Long) {
        reachable = true
        unreachableSinceMs = null
        visibleByReachability = true
    }

    fun onUnreachable(nowMs: Long) {
        reachable = false
        onTick(nowMs)
    }

    /** Evaluates the countdown at [nowMs]. Starts it if RetroArch is unreachable and nothing is counting yet. */
    fun onTick(nowMs: Long) {
        if (reachable != false || !visibleByReachability || !displayOn) return
        val since = unreachableSinceMs ?: nowMs.also { unreachableSinceMs = it }
        if (nowMs - since >= hideAfterMs) {
            visibleByReachability = false
            unreachableSinceMs = null
        }
    }

    fun onDisplayChanged(isOn: Boolean) {
        displayOn = isOn
        if (!isOn) {
            unreachableSinceMs = null
            portClosedSinceMs = null
        }
    }

    /** What the last request found of RetroArch's port: nothing listening, or something there, answering or not. */
    fun onPortState(closed: Boolean, nowMs: Long) {
        if (!displayOn) return
        if (!closed) {
            portSeenOpen = true
            portClosedSinceMs = null
            return
        }
        if (!portSeenOpen) return
        val since = portClosedSinceMs ?: nowMs.also { portClosedSinceMs = it }
        if (nowMs - since >= hideAfterMs) shouldExit = true
    }

    fun onUserHide() {
        userHidden = true
        unreachableSinceMs = null
    }

    fun onUserShow() {
        userHidden = false
        visibleByReachability = true
        unreachableSinceMs = null
    }

    companion object {
        const val DEFAULT_HIDE_AFTER_MS = 5_000L
    }
}
