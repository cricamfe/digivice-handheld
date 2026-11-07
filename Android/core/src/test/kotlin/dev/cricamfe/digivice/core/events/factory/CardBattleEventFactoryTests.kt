package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.dto.CardBattleDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/CardBattleEventFactoryTests.cs`. */
class CardBattleEventFactoryTests {
    @Test
    fun `create returns a card battle changed event when a card battle starts`() {
        val previous = State(cardBattle = CardBattle(id = null))
        val new = State(cardBattle = CardBattle(id = 1))

        val event = CardBattleEventFactory.create(previous, new).single()

        assertEquals(EventType.CardBattleChanged, event.type)
        assertPresent(1, assertInstanceOf(CardBattleDto::class.java, event.payload).id)
    }

    @Test
    fun `create returns an explicit null id when the card battle ends`() {
        val previous = State(cardBattle = CardBattle(id = 1))
        val new = State(cardBattle = CardBattle(id = null))

        val event = CardBattleEventFactory.create(previous, new).single()

        assertPresent(null, assertInstanceOf(CardBattleDto::class.java, event.payload).id)
    }

    @Test
    fun `create returns no events when the card battle has no changes`() {
        val previous = State(cardBattle = CardBattle(id = 5))
        val new = State(cardBattle = CardBattle(id = 5))

        assertTrue(CardBattleEventFactory.create(previous, new).isEmpty())
    }
}
