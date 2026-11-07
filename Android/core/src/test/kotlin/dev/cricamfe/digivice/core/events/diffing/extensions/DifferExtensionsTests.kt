package dev.cricamfe.digivice.core.events.diffing.extensions

import dev.cricamfe.digivice.core.domain.models.Player
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/Extensions/DifferExtensionsTests.cs`. */
class DifferExtensionsTests {
    @Test
    fun `hasNoChanges returns true when the new object is null`() {
        val new: Player? = null
        val previous = Player(bits = 100, mapId = "0001")

        assertTrue(new.hasNoChanges(previous))
    }

    @Test
    fun `hasNoChanges returns true when the objects are value equal`() {
        val new = Player(bits = 100, mapId = "0001")
        val previous = Player(bits = 100, mapId = "0001")

        assertTrue(new.hasNoChanges(previous))
    }

    @Test
    fun `hasNoChanges returns false when the objects are different`() {
        val new = Player(bits = 101, mapId = "0001")
        val previous = Player(bits = 100, mapId = "0001")

        assertFalse(new.hasNoChanges(previous))
    }

    @Test
    fun `hasNoChanges returns false when there is no previous object`() {
        assertFalse(Player(bits = 100, mapId = "0001").hasNoChanges(null))
    }
}
