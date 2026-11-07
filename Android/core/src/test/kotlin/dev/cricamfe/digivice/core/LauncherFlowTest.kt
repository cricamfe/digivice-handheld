package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.LauncherFlow.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LauncherFlowTest {
    private val flow = LauncherFlow()

    @Test
    fun `with the permission it starts the overlay and finishes, without a first-run page`() {
        assertEquals(Action.StartOverlayAndFinish, flow.onCheck(canDrawOverlays = true))
    }

    @Test
    fun `without the permission it shows the first-run page`() {
        assertEquals(Action.ShowFirstRun, flow.onCheck(canDrawOverlays = false))
    }

    @Test
    fun `the first-run page is shown once and then left alone while the permission is missing`() {
        assertEquals(Action.ShowFirstRun, flow.onCheck(canDrawOverlays = false))

        // Back from the settings without having granted it, or any other resume.
        assertEquals(Action.None, flow.onCheck(canDrawOverlays = false))
        assertEquals(Action.None, flow.onCheck(canDrawOverlays = false))
    }

    @Test
    fun `back from the settings with the permission granted it starts the overlay and finishes`() {
        flow.onCheck(canDrawOverlays = false)

        assertEquals(Action.StartOverlayAndFinish, flow.onCheck(canDrawOverlays = true))
    }

    @Test
    fun `the overlay is started once however many times the activity is resumed`() {
        assertEquals(Action.StartOverlayAndFinish, flow.onCheck(canDrawOverlays = true))

        assertEquals(Action.None, flow.onCheck(canDrawOverlays = true))
        assertEquals(Action.None, flow.onCheck(canDrawOverlays = false), "a revoked permission does not bring the page back")
    }
}
