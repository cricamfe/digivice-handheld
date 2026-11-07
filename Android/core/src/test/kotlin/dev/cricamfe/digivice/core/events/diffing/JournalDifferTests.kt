package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/JournalDifferTests.cs`. */
class JournalDifferTests {
    private fun quest(id: String, stepIsDone: Boolean, stepNumber: Int = 0) =
        Quest(id = id, requisites = emptyList(), steps = listOf(Step(number = stepNumber, isDone = stepIsDone)))

    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val previous = Journal(mainQuest = Quest(id = "1"), sideQuests = emptyList())
        val new = Journal(mainQuest = Quest(id = "1"), sideQuests = emptyList())

        val result = JournalDiffer.diff(previous, new)

        assertAbsent(result.mainQuest)
        assertAbsent(result.sideQuests)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val new = Journal(mainQuest = Quest(id = "1"), sideQuests = emptyList())

        val result = JournalDiffer.diff(null, new)

        assertTrue(result.mainQuest.hasValue)
        assertEquals("1", result.mainQuest.value.id)
    }

    @Test
    fun `diff returns a delta when the main quest changed`() {
        val previous = Journal(mainQuest = quest("1", stepIsDone = false), sideQuests = emptyList())
        val new = Journal(mainQuest = quest("1", stepIsDone = true), sideQuests = emptyList())

        val result = JournalDiffer.diff(previous, new)

        assertEquals("1", result.mainQuest.value.id)
        val steps = result.mainQuest.value.steps.value
        assertEquals(1, steps.size)
        assertPresent(true, steps[0].isDone)
    }

    @Test
    fun `diff returns a delta when the side quests changed`() {
        val previous = Journal(mainQuest = Quest(id = "1"), sideQuests = listOf(quest("2", stepIsDone = false)))
        val new = Journal(mainQuest = Quest(id = "1"), sideQuests = listOf(quest("2", stepIsDone = true)))

        val result = JournalDiffer.diff(previous, new)

        assertAbsent(result.mainQuest)
        val sideQuests = result.sideQuests.value
        assertEquals(1, sideQuests.size)
        assertEquals("2", sideQuests[0].id)
        assertPresent(true, sideQuests[0].steps.value[0].isDone)
    }

    @Test
    fun `diff returns multiple deltas when the main quest and the side quests changed`() {
        val previous = Journal(mainQuest = quest("1", stepIsDone = false), sideQuests = listOf(quest("2", stepIsDone = false)))
        val new = Journal(mainQuest = quest("1", stepIsDone = true), sideQuests = listOf(quest("2", stepIsDone = true)))

        val result = JournalDiffer.diff(previous, new)

        assertEquals("1", result.mainQuest.value.id)
        assertPresent(true, result.mainQuest.value.steps.value[0].isDone)
        val sideQuests = result.sideQuests.value
        assertEquals(1, sideQuests.size)
        assertEquals("2", sideQuests[0].id)
        assertPresent(true, sideQuests[0].steps.value[0].isDone)
    }

    @Test
    fun `diff returns a delta when the legendary weapons changed`() {
        val previous = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = listOf(quest("eternally", stepIsDone = false, stepNumber = 1)),
        )
        val new = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = listOf(quest("eternally", stepIsDone = true, stepNumber = 1)),
        )

        val result = JournalDiffer.diff(previous, new)

        assertAbsent(result.mainQuest)
        assertAbsent(result.sideQuests)
        val legendaryWeapons = result.legendaryWeapons.value
        assertEquals(1, legendaryWeapons.size)
        assertEquals("eternally", legendaryWeapons[0].id)
        assertPresent(true, legendaryWeapons[0].steps.value[0].isDone)
    }

    @Test
    fun `diff returns a delta when the dri agents changed`() {
        val previous = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = emptyList(),
            driAgents = listOf(quest("driAgentGuilmon", stepIsDone = false, stepNumber = 1)),
        )
        val new = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = emptyList(),
            driAgents = listOf(quest("driAgentGuilmon", stepIsDone = true, stepNumber = 1)),
        )

        val result = JournalDiffer.diff(previous, new)

        assertAbsent(result.mainQuest)
        assertAbsent(result.sideQuests)
        assertAbsent(result.legendaryWeapons)
        val driAgents = result.driAgents.value
        assertEquals(1, driAgents.size)
        assertEquals("driAgentGuilmon", driAgents[0].id)
        assertPresent(true, driAgents[0].steps.value[0].isDone)
    }

    @Test
    fun `diff returns a delta when the duel island changed`() {
        val previous = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = emptyList(),
            driAgents = emptyList(),
            duelIsland = listOf(quest("asukaTrophy", stepIsDone = false, stepNumber = 1)),
        )
        val new = Journal(
            mainQuest = Quest(id = "1"),
            sideQuests = emptyList(),
            legendaryWeapons = emptyList(),
            driAgents = emptyList(),
            duelIsland = listOf(quest("asukaTrophy", stepIsDone = true, stepNumber = 1)),
        )

        val result = JournalDiffer.diff(previous, new)

        assertAbsent(result.mainQuest)
        assertAbsent(result.sideQuests)
        assertAbsent(result.legendaryWeapons)
        assertAbsent(result.driAgents)
        val duelIsland = result.duelIsland.value
        assertEquals(1, duelIsland.size)
        assertEquals("asukaTrophy", duelIsland[0].id)
        assertPresent(true, duelIsland[0].steps.value[0].isDone)
    }
}
