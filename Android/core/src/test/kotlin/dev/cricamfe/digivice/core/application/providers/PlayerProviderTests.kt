package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakePlayerLoader
import dev.cricamfe.digivice.core.memory.resources.PlayerResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayerProviderTests {
    @Test
    fun `get should load resource and apply player assembler`() {
        val loader = FakePlayerLoader(PlayerResource(bits = 250, mapId = 1))

        val result = PlayerProvider(loader).get()

        assertEquals(250, result.bits)
        assertEquals("0001", result.mapId)
        assertEquals(1, loader.calls)
    }
}
