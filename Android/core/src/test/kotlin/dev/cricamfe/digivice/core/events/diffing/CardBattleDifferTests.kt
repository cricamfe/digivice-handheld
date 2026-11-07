package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/CardBattleDifferTests.cs`. */
class CardBattleDifferTests {
    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val result = CardBattleDiffer.diff(CardBattle(id = 1), CardBattle(id = 1))

        assertAbsent(result.id)
    }

    @Test
    fun `diff returns the full dto when the previous card battle is null`() {
        val result = CardBattleDiffer.diff(null, CardBattle(id = 3))

        assertPresent(3, result.id)
    }

    @Test
    fun `diff returns the id delta when only the id changed`() {
        val result = CardBattleDiffer.diff(CardBattle(id = 1), CardBattle(id = 11))

        assertPresent(11, result.id)
    }

    @Test
    fun `diff returns an explicit null id when the id becomes null`() {
        val result = CardBattleDiffer.diff(CardBattle(id = 11), CardBattle(id = null))

        assertPresent(null, result.id)
    }
}
