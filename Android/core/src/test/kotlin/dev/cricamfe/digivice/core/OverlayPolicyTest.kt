package dev.cricamfe.digivice.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OverlayPolicyTest {
    private val policy = OverlayPolicy(hideAfterMs = 5_000)

    // ---- the five rules of the brief -----------------------------------------------------------------------------

    @Test
    fun `hidden until RetroArch answers`() {
        assertFalse(policy.shouldShow)

        policy.onReachable(0)

        assertTrue(policy.shouldShow)
    }

    @Test
    fun `stays visible through short gaps`() {
        policy.onReachable(0)

        policy.onUnreachable(1_000)
        policy.onTick(5_000) // 4 s without an answer

        assertTrue(policy.shouldShow)
    }

    @Test
    fun `hides after five seconds unreachable`() {
        policy.onReachable(0)

        policy.onUnreachable(1_000)
        policy.onTick(5_999)
        assertTrue(policy.shouldShow, "4.999 s is still a short gap")
        policy.onTick(6_000)

        assertFalse(policy.shouldShow)
    }

    @Test
    fun `display off stops polling`() {
        assertTrue(policy.shouldPoll, "it polls from the start, or nothing would ever answer")

        policy.onDisplayChanged(false)

        assertFalse(policy.shouldPoll)
    }

    @Test
    fun `display on resumes polling and the overlay stays`() {
        policy.onReachable(0)
        policy.onDisplayChanged(false)

        policy.onDisplayChanged(true)

        assertTrue(policy.shouldPoll)
        assertTrue(policy.shouldShow)
    }

    // ---- reachability details -------------------------------------------------------------------------------------

    @Test
    fun `being reachable again before the deadline cancels the countdown`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)
        policy.onReachable(3_000)

        policy.onTick(60_000)

        assertTrue(policy.shouldShow)
        assertNull(policy.hideDeadlineMs)
    }

    @Test
    fun `reporting unreachable again does not restart the countdown`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)

        policy.onUnreachable(5_000)
        assertTrue(policy.shouldShow)
        policy.onUnreachable(6_000)

        assertFalse(policy.shouldShow, "the five seconds count from the first report")
    }

    @Test
    fun `hideDeadlineMs says when the overlay goes away`() {
        assertNull(policy.hideDeadlineMs, "nothing counting yet")
        policy.onReachable(0)
        assertNull(policy.hideDeadlineMs)

        policy.onUnreachable(1_000)

        assertEquals(6_000L, policy.hideDeadlineMs)
    }

    @Test
    fun `unreachable before ever answering keeps it hidden`() {
        policy.onUnreachable(0)
        policy.onTick(100_000)

        assertFalse(policy.shouldShow)
        assertNull(policy.hideDeadlineMs)
    }

    @Test
    fun `once hidden it comes back when RetroArch answers`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)
        policy.onTick(6_000)
        assertFalse(policy.shouldShow)

        policy.onReachable(20_000)

        assertTrue(policy.shouldShow)
    }

    // ---- the console asleep -----------------------------------------------------------------------------------------

    @Test
    fun `time with the display off does not count as unreachable`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)
        policy.onDisplayChanged(false)

        // A long sleep: RetroArch cannot answer, and that says nothing about it.
        policy.onTick(600_000)
        policy.onDisplayChanged(true)
        policy.onTick(600_001)

        assertTrue(policy.shouldShow, "waking up must not hide the overlay")
    }

    @Test
    fun `after waking up the grace period starts over`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)
        policy.onDisplayChanged(false)
        policy.onDisplayChanged(true)

        policy.onTick(600_000)
        assertTrue(policy.shouldShow)
        assertEquals(605_000L, policy.hideDeadlineMs)
        policy.onTick(604_999)
        assertTrue(policy.shouldShow)
        policy.onTick(605_000)

        assertFalse(policy.shouldShow, "still unreachable five seconds after waking up")
    }

    @Test
    fun `a report that arrives while the display is off does not start the countdown`() {
        policy.onReachable(0)
        policy.onDisplayChanged(false)

        policy.onUnreachable(1_000)
        policy.onTick(100_000)

        assertTrue(policy.shouldShow)
        assertNull(policy.hideDeadlineMs)
    }

    @Test
    fun `an answer while the display is off still counts`() {
        policy.onDisplayChanged(false)

        policy.onReachable(0)

        assertTrue(policy.shouldShow)
    }

    // ---- user intent ------------------------------------------------------------------------------------------------

    @Test
    fun `user hide wins over a reachable RetroArch`() {
        policy.onReachable(0)

        policy.onUserHide()
        policy.onReachable(1_000)

        assertFalse(policy.shouldShow)
    }

    @Test
    fun `user hide keeps polling`() {
        policy.onReachable(0)

        policy.onUserHide()

        assertTrue(policy.shouldPoll, "the loop must still notice RetroArch")
    }

    @Test
    fun `user show brings the overlay back and shows it before RetroArch answers`() {
        policy.onUserShow()

        assertTrue(policy.shouldShow)
    }

    @Test
    fun `user show after a hide shows it again`() {
        policy.onReachable(0)
        policy.onUserHide()

        policy.onUserShow()

        assertTrue(policy.shouldShow)
    }

    @Test
    fun `user show while RetroArch is unreachable gives five seconds of grace`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)
        policy.onTick(6_000)
        assertFalse(policy.shouldShow)

        policy.onUserShow()
        policy.onTick(100_000)
        assertTrue(policy.shouldShow, "the countdown starts at the first tick after the user asked")
        assertEquals(105_000L, policy.hideDeadlineMs)
        policy.onTick(105_000)

        assertFalse(policy.shouldShow)
    }

    @Test
    fun `user hide stops the countdown from mattering`() {
        policy.onReachable(0)
        policy.onUnreachable(1_000)

        policy.onUserHide()
        policy.onTick(100_000)
        assertFalse(policy.shouldShow)
        policy.onUserShow()
        policy.onReachable(100_001)

        assertTrue(policy.shouldShow)
    }

    @Test
    fun `leaves when the port RetroArch listens on stays closed after having been open`() {
        val policy = OverlayPolicy(hideAfterMs = 1_000)
        policy.onPortState(closed = false, nowMs = 0)
        policy.onPortState(closed = true, nowMs = 100)
        assertFalse(policy.shouldExit, "a moment closed is not RetroArch gone: a reload closes it too")

        policy.onPortState(closed = true, nowMs = 1_100)
        assertTrue(policy.shouldExit)
    }

    @Test
    fun `waits for a RetroArch that has not been opened yet`() {
        val policy = OverlayPolicy(hideAfterMs = 1_000)
        policy.onPortState(closed = true, nowMs = 0)
        policy.onPortState(closed = true, nowMs = 100_000)
        assertFalse(policy.shouldExit)
    }

    @Test
    fun `a RetroArch in the background keeps its port and the app stays`() {
        val policy = OverlayPolicy(hideAfterMs = 1_000)
        policy.onPortState(closed = false, nowMs = 0)
        policy.onUnreachable(nowMs = 0)
        policy.onPortState(closed = false, nowMs = 100_000)
        assertFalse(policy.shouldExit)
    }

    @Test
    fun `a port that opens again stops the countdown`() {
        val policy = OverlayPolicy(hideAfterMs = 1_000)
        policy.onPortState(closed = false, nowMs = 0)
        policy.onPortState(closed = true, nowMs = 100)
        policy.onPortState(closed = false, nowMs = 500)
        policy.onPortState(closed = true, nowMs = 1_200)
        assertFalse(policy.shouldExit)
    }

    @Test
    fun `the console asleep says nothing about the port`() {
        val policy = OverlayPolicy(hideAfterMs = 1_000)
        policy.onPortState(closed = false, nowMs = 0)
        policy.onPortState(closed = true, nowMs = 100)
        policy.onDisplayChanged(false)
        policy.onPortState(closed = true, nowMs = 5_000)
        assertFalse(policy.shouldExit)
        policy.onDisplayChanged(true)
        policy.onPortState(closed = true, nowMs = 5_100)
        assertFalse(policy.shouldExit, "the countdown starts over")
    }
}
