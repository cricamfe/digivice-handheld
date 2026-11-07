package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeDigimonBattleLoader
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.VitalResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DigimonBattleProviderTests {
    @Test
    fun `get should load resource and apply digimon battle assembler`() {
        val resource = DigimonBattleResource(
            field = 0x02u,
            enemy = EnemyResource(id = 122, groupId = 201, speed = 84, hp = VitalResource(current = 600, max = 672)),
        )
        val loader = FakeDigimonBattleLoader(resource)

        val result = DigimonBattleProvider(loader).get()

        assertEquals(0x02.toUByte(), result.field)
        assertNotNull(result.enemy)
        assertEquals(122, result.enemy!!.id)
        assertEquals(600, result.enemy!!.hp.current)
        assertEquals(1, loader.calls)
    }

    @Test
    fun `get should return null enemy when out of battle`() {
        val result = DigimonBattleProvider(FakeDigimonBattleLoader(DigimonBattleResource())).get()

        assertEquals(0.toUByte(), result.field)
        assertNull(result.enemy)
    }
}
