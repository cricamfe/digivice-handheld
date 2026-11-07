package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/CardBattleConverterTests.cs`. */
class CardBattleConverterTests {
    @Test
    fun `toDto maps the id`() {
        val dto = CardBattleConverter.toDto(CardBattle(id = 11))

        assertTrue(dto.id.hasValue)
        assertEquals(11, dto.id.value)
    }

    @Test
    fun `toDto sends an explicit null when there is no card battle`() {
        val dto = CardBattleConverter.toDto(CardBattle(id = null))

        assertEquals(Optional.Present(null), dto.id)
    }
}
