package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.memory.resources.CardBattleResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CardBattleAssemblerTests {
    @Test
    fun `assemble should map id correctly when resource is valid`() {
        val result = CardBattleAssembler.assemble(CardBattleResource(id = 5))

        assertEquals(5, result.id)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `assemble should return null id when id is not positive`(rawId: Int) {
        val result = CardBattleAssembler.assemble(CardBattleResource(id = rawId))

        assertNull(result.id)
    }
}
