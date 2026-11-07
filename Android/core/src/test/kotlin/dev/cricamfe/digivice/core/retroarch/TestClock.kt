// advanceTimeBy and the scheduler's currentTime are still experimental in kotlinx-coroutines-test.
@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.cricamfe.digivice.core.retroarch

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.util.concurrent.atomic.AtomicLong

/**
 * The time the loop and the [FakeRetroArch] see in a test, which only moves when something moves it: the virtual time of
 * [scheduler] (the loop's `delay`), plus what [advance] adds (the fake's latency of a frame per request, a test that lets
 * time pass between two reads). With no scheduler it stays still until [advance]d.
 *
 * Thread safe: the fake RetroArch advances it from its own thread while the loop waits for the reply.
 */
class TestClock(private val scheduler: TestCoroutineScheduler? = null) {
    private val advanced = AtomicLong()

    fun nanoTime(): Long = (scheduler?.currentTime ?: 0L) * NANOS_PER_MS + advanced.get()

    fun millis(): Long = nanoTime() / NANOS_PER_MS

    fun advance(ms: Long) {
        advanced.addAndGet(ms * NANOS_PER_MS)
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000L
    }
}
