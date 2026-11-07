package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.models.EventType
import dev.cricamfe.digivice.core.events.presentValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/DigimonBattleEventFactoryTests.cs`. */
class DigimonBattleEventFactoryTests {
    private fun battle(enemy: Enemy?) = DigimonBattle(field = 0x00u, enemy = enemy)

    private fun enemy() = Enemy(
        id = 122,
        groupId = 201,
        condition = 0x01,
        strength = 250,
        defense = 180,
        speed = 84,
        hp = Vital(current = 600, max = 672),
    )

    @Test
    fun `create returns no events when the digimon battle has no changes`() {
        val previous = State(digimonBattle = battle(enemy()))
        val new = State(digimonBattle = battle(enemy()))

        assertTrue(DigimonBattleEventFactory.create(previous, new).isEmpty())
    }

    @Test
    fun `create returns no events when out of battle on both states`() {
        val previous = State(digimonBattle = battle(enemy = null))
        val new = State(digimonBattle = battle(enemy = null))

        assertTrue(DigimonBattleEventFactory.create(previous, new).isEmpty())
    }

    @Test
    fun `create returns the full enemy when the battle starts`() {
        val previous = State(digimonBattle = battle(enemy = null))
        val new = State(digimonBattle = battle(enemy()))

        val event = DigimonBattleEventFactory.create(previous, new).single()

        assertEquals(EventType.DigimonBattleChanged, event.type)
        val dto = assertInstanceOf(DigimonBattleDto::class.java, event.payload)
        val enemy = presentValue(dto.enemy)
        assertPresent(122, enemy.id)
        assertPresent(201, enemy.groupId)
    }

    @Test
    fun `create returns an explicit null enemy when the battle ends`() {
        val previous = State(digimonBattle = battle(enemy()))
        val new = State(digimonBattle = battle(enemy = null))

        val event = DigimonBattleEventFactory.create(previous, new).single()

        assertEquals(EventType.DigimonBattleChanged, event.type)
        val dto = assertInstanceOf(DigimonBattleDto::class.java, event.payload)
        assertPresent(null, dto.enemy)
        assertAbsent(dto.field)
    }
}
