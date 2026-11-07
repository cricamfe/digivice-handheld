package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/ImportantItemsDifferTests.cs`. */
class ImportantItemsDifferTests {
    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val previous = ImportantItems(treeBoots = true, fishingPole = false, asukaTrophy = true, sunTrophy = false)
        val current = ImportantItems(treeBoots = true, fishingPole = false, asukaTrophy = true, sunTrophy = false)

        val result = ImportantItemsDiffer.diff(previous, current)

        assertAbsent(result.treeBoots)
        assertAbsent(result.fishingPole)
        assertAbsent(result.asukaTrophy)
        assertAbsent(result.sunTrophy)
    }

    @Test
    fun `diff returns the full dto when the previous is null`() {
        val current = ImportantItems(treeBoots = true, fishingPole = false, asukaTrophy = true, sunTrophy = true)

        val result = ImportantItemsDiffer.diff(null, current)

        assertPresent(true, result.treeBoots)
        assertPresent(false, result.fishingPole)
        assertPresent(true, result.asukaTrophy)
        assertPresent(true, result.sunTrophy)
    }

    @Test
    fun `diff returns the tree boots delta when only the tree boots changed`() {
        val previous = ImportantItems(treeBoots = false, fishingPole = true, asukaTrophy = true)
        val current = ImportantItems(treeBoots = true, fishingPole = true, asukaTrophy = true)

        val result = ImportantItemsDiffer.diff(previous, current)

        assertPresent(true, result.treeBoots)
        assertAbsent(result.fishingPole)
        assertAbsent(result.asukaTrophy)
    }

    @Test
    fun `diff returns a false delta when an item is lost`() {
        val previous = ImportantItems(treeBoots = true, fishingPole = false, asukaTrophy = false)
        val current = ImportantItems(treeBoots = false, fishingPole = false, asukaTrophy = false)

        val result = ImportantItemsDiffer.diff(previous, current)

        assertPresent(false, result.treeBoots)
        assertAbsent(result.fishingPole)
        assertAbsent(result.asukaTrophy)
    }
}
