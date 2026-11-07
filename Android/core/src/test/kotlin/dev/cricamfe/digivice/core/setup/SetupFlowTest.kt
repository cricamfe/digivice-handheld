package dev.cricamfe.digivice.core.setup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupFlowTest {
    private val flow = SetupFlow()
    private val disc = "Digimon World 2003 (Europe).chd"

    private fun progress(piece: String, index: Int, done: Int, total: Int) =
        ExtractionState.Progress(piece, index, 7, done, total)

    @Test
    fun `with nothing chosen it asks for a file`() {
        val page = flow.page(ExtractionState.Idle)
        assertEquals(SetupPhase.Choose, page.phase)
        assertEquals(SetupAction.Choose, page.action)
        assertTrue(page.enabled)
        assertNull(page.file)
        assertFalse(page.keepsScreenOn)
    }

    @Test
    fun `checking shows the chosen file with no known progress and can be paused`() {
        val page = flow.page(ExtractionState.Checking, hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.Checking, page.phase)
        assertEquals(SetupAction.Pause, page.action)
        assertEquals(disc, page.file)
        assertNull(page.progress)
        assertTrue(page.keepsScreenOn)
    }

    @Test
    fun `extracting shows the piece, its counts and the overall progress`() {
        val page = flow.page(progress("Maps", 6, 100, 293), hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.Working, page.phase)
        assertEquals(SetupAction.Pause, page.action)
        assertEquals("Maps", page.piece)
        assertEquals(100, page.done)
        assertEquals(293, page.total)
        val expected = (1.0 + 0.3 + 1.4 + 0.1 + 0.3 + 41.0 * 100 / 293) / 114.1
        assertEquals(expected, page.progress!!, 1e-9)
        assertTrue(page.keepsScreenOn)
    }

    @Test
    fun `the overall progress is weighted by the time each piece takes and stops short of 100 percent`() {
        assertEquals(0.0, SetupFlow.overallProgress(progress("Text", 1, 0, 6)), 1e-9)
        // The five quick pieces are about 3 % of the work, not five sevenths of it.
        assertEquals(3.1 / 114.1, SetupFlow.overallProgress(progress("Maps", 6, 0, 293)), 1e-9)
        assertEquals(0.99, SetupFlow.overallProgress(progress("Models", 7, 418, 418)), 1e-9)
        // Pieces this table does not know get equal shares.
        assertEquals(0.5, SetupFlow.overallProgress(ExtractionState.Progress("Other", 2, 2, 0, 10)), 1e-9)
        // A piece with no resources counts as not started.
        assertEquals(1.0 / 114.1, SetupFlow.overallProgress(progress("Cards", 2, 0, 0)), 1e-9)
    }

    @Test
    fun `waiting for RetroArch keeps the last progress and can still be paused`() {
        val working = flow.page(progress("Models", 7, 200, 418), hasSavedDisc = true, file = disc)
        val page = flow.page(ExtractionState.PausedForGame, hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.WaitingForGame, page.phase)
        assertEquals(SetupAction.Pause, page.action)
        assertEquals(working.progress, page.progress)
        assertFalse(page.keepsScreenOn)
    }

    @Test
    fun `while pausing the button is disabled`() {
        flow.page(progress("Maps", 6, 10, 293), hasSavedDisc = true)
        val page = flow.page(ExtractionState.Cancelling, hasSavedDisc = true)
        assertEquals(SetupPhase.Pausing, page.phase)
        assertFalse(page.enabled)
        assertTrue(page.progress != null)
    }

    @Test
    fun `a pause by the user resumes the same file and keeps the bar`() {
        val working = flow.page(progress("Maps", 6, 10, 293), hasSavedDisc = true, file = disc)
        val page = flow.page(ExtractionState.Paused, hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.Paused, page.phase)
        assertEquals(SetupAction.Resume, page.action)
        assertEquals(working.progress, page.progress)
        assertEquals(disc, page.file)
    }

    @Test
    fun `an idle service with a granted file is a pause left by an earlier process`() {
        val page = flow.page(ExtractionState.Idle, hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.Paused, page.phase)
        assertEquals(SetupAction.Resume, page.action)
        assertNull(page.progress)
    }

    @Test
    fun `a pause without the file any more asks for one`() {
        assertEquals(SetupAction.Choose, flow.page(ExtractionState.Paused, hasSavedDisc = false).action)
    }

    @Test
    fun `a new check forgets the progress of the previous run`() {
        flow.page(progress("Maps", 6, 10, 293), hasSavedDisc = true)
        flow.page(ExtractionState.Checking, hasSavedDisc = true)
        assertNull(flow.page(ExtractionState.PausedForGame, hasSavedDisc = true).progress)
    }

    @Test
    fun `a file that cannot work asks for another one`() {
        for (reason in listOf(
            SetupError.WrongGame, SetupError.NotADisc, SetupError.InvalidChd, SetupError.Truncated,
            SetupError.FileGone, SetupError.LocalFile, SetupError.Failed,
        )) {
            val page = flow.page(ExtractionState.Failed(reason), hasSavedDisc = !reason.needsNewDisc, file = disc)
            assertEquals(SetupPhase.Failed, page.phase, "$reason")
            assertEquals(SetupAction.ChooseOther, page.action, "$reason")
            assertEquals(reason, page.problem)
            assertEquals(disc, page.file)
        }
    }

    @Test
    fun `no space and an interruption retry the same file`() {
        for (reason in listOf(SetupError.NoSpace, SetupError.Interrupted)) {
            val page = flow.page(ExtractionState.Failed(reason), hasSavedDisc = true)
            assertEquals(SetupAction.Retry, page.action, "$reason")
            assertEquals(reason, page.problem)
            // Without the file there is nothing to retry.
            assertEquals(SetupAction.ChooseOther, flow.page(ExtractionState.Failed(reason)).action)
        }
    }

    @Test
    fun `a local failure wins over the service state`() {
        val page = flow.page(ExtractionState.Idle, localFailure = SetupError.FileGone, readyAtOpen = true)
        assertEquals(SetupPhase.Failed, page.phase)
        assertEquals(SetupError.FileGone, page.problem)
    }

    @Test
    fun `finished, or ready when opened, continues to the connection chooser`() {
        val done = flow.page(ExtractionState.Ready, hasSavedDisc = true, file = disc)
        assertEquals(SetupPhase.Ready, done.phase)
        assertEquals(SetupAction.Continue, done.action)
        assertEquals(1.0, done.progress)
        val opened = flow.page(ExtractionState.Idle, readyAtOpen = true)
        assertEquals(SetupPhase.Ready, opened.phase)
        assertEquals(SetupAction.Continue, opened.action)
    }

    @Test
    fun `the page JSON names every field the page reads`() {
        val page = flow.page(progress("Cards", 2, 3, 315), hasSavedDisc = true, file = disc, language = "es-ES")
        val json = Json.parseToJsonElement(page.toJson()).jsonObject
        assertEquals("working", json.getValue("phase").jsonPrimitive.content)
        assertEquals("pause", json.getValue("action").jsonPrimitive.content)
        assertEquals("Cards", json.getValue("piece").jsonPrimitive.content)
        assertEquals("es-ES", json.getValue("language").jsonPrimitive.content)
        assertEquals(JsonNull, json.getValue("problem"))
        assertEquals(
            setOf("phase", "action", "enabled", "file", "piece", "done", "total", "progress", "problem", "language"),
            json.keys,
        )
        val failed = Json.parseToJsonElement(flow.page(ExtractionState.Failed(SetupError.NoSpace)).toJson()).jsonObject
        assertEquals("NoSpace", failed.getValue("problem").jsonPrimitive.content)
        assertEquals("failed", failed.getValue("phase").jsonPrimitive.content)
        assertEquals("chooseOther", failed.getValue("action").jsonPrimitive.content)
    }

    @Test
    fun `the page's action names come back to the same actions`() {
        for (action in SetupAction.entries) assertEquals(action, SetupAction.named(action.wireName))
        assertEquals(SetupAction.ChooseOther, SetupAction.named("chooseOther"))
        assertNull(SetupAction.named("ChooseOther"))
        assertNull(SetupAction.named("delete"))
    }
}
