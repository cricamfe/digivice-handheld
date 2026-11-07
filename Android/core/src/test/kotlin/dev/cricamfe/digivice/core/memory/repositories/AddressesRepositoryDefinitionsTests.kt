package dev.cricamfe.digivice.core.memory.repositories

import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.addresses.NpcAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcsAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

/** Integration tests against the real definitions of `Backend/Memory/Definitions`. */
class AddressesRepositoryDefinitionsTests {
    @Test
    fun `real definitions should load every addresses file`() {
        val repository = TestEnvironment.createAddressesRepository()

        assertEquals(0x00048DA0L, repository.getPlayerAddresses().bits)
        assertNotEquals(0L, repository.getImportantItemsAddresses().treeBoots)
        assertNotEquals(0L, repository.getCardBattleAddresses().id)
        assertNotEquals(0L, repository.getDigimonBattleAddresses().field)
        assertNotEquals(0L, repository.getEnemyAddresses().enemySlotBase)
        assertEquals(3, repository.getEnemyAddresses().slotCount)
        assertNotEquals(0L, repository.getInBattleAddresses().allySlotBase)
        assertNotEquals(0L, repository.getAuctionsAddresses().divineBarrier.address)

        val partyAddresses = repository.getPartyAddresses()
        assertEquals(0xFF, partyAddresses.emptySlotId)
        assertEquals(listOf(1, 2, 3), partyAddresses.slots.map { it.index })
        assertTrue(partyAddresses.slots.all { it.address != 0L })

        val digimonStatusAddresses = repository.getDigimonStatusAddresses()
        assertNotEquals(0, digimonStatusAddresses.level)
        assertTrue(digimonStatusAddresses.digievolutions.slots.isNotEmpty())

        val digimonsAddresses = repository.getDigimonsAddresses()
        assertTrue(digimonsAddresses.containsKey(0))
        assertTrue(digimonsAddresses.values.all { it.memoryBlockAddress != 0L })
    }

    @Test
    fun `real definitions should load every npc with battles`() {
        val npcsAddresses = TestEnvironment.createAddressesRepository().getNpcsAddresses()

        val npcProperties = NpcsAddresses::class.memberProperties
        assertEquals(29, npcProperties.size)
        for (property in npcProperties) {
            val npcAddresses = property.get(npcsAddresses) as NpcAddresses
            assertTrue(npcAddresses.battles.isNotEmpty(), "${property.name} has no battles")
        }
    }

    @Test
    fun `real definitions should load every quest folder`() {
        val repository = TestEnvironment.createAddressesRepository()

        assertTrue(repository.getMainQuest().steps.isNotEmpty())
        assertEquals(4, repository.getAllSideQuests().size)
        assertEquals(5, repository.getAllLegendaryWeapons().size)
        assertEquals(8, repository.getAllDriAgents().size)
        assertEquals(5, repository.getAllDuelIsland().size)
        val quests = repository.getAllSideQuests() +
            repository.getAllLegendaryWeapons() +
            repository.getAllDriAgents() +
            repository.getAllDuelIsland()
        for (quest in quests) {
            assertFalse(quest.id.isEmpty())
            assertTrue(quest.steps.isNotEmpty())
        }
    }
}
