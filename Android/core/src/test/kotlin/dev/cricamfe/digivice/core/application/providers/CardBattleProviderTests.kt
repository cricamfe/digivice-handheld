package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeCardBattleLoader
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CardBattleProviderTests {
    @Test
    fun `get should load resource and apply card battle assembler`() {
        val loader = FakeCardBattleLoader(CardBattleResource(id = 7))

        val result = CardBattleProvider(loader).get()

        assertEquals(7, result.id)
        assertEquals(1, loader.calls)
    }

    @Test
    fun `get should return null id when resource id is zero`() {
        val loader = FakeCardBattleLoader(CardBattleResource(id = 0))

        val result = CardBattleProvider(loader).get()

        assertNull(result.id)
        assertEquals(1, loader.calls)
    }
}
