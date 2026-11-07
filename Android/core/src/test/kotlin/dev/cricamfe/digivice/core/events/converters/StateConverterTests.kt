package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/StateConverterTests.cs`. */
class StateConverterTests {
    private fun sampleState() = State(
        player = Player(bits = 100, mapId = "0001"),
        importantItems = ImportantItems(treeBoots = true, fishingPole = false, asukaTrophy = true),
        party = Party(
            slots = listOf(
                DigimonSlot(index = 1, digimonId = 1, digimon = Digimon(level = 5)),
                DigimonSlot(index = 2, digimonId = null, digimon = null),
                DigimonSlot(index = 3, digimonId = null, digimon = null),
            ),
        ),
        digimonBattle = DigimonBattle(),
        cardBattle = CardBattle(id = null),
        auctions = Auctions(divineBarrier = true),
        npcs = Npcs(),
        journal = Journal(mainQuest = Quest(id = "MainQuest"), sideQuests = emptyList()),
    )

    @Test
    fun `toDto maps all state sections`() {
        val dto = StateConverter.toDto(sampleState())

        assertTrue(dto.player.bits.hasValue)
        assertEquals(100, dto.player.bits.value)

        assertTrue(dto.importantItems.treeBoots.hasValue)
        assertTrue(dto.importantItems.treeBoots.value)
        assertTrue(dto.importantItems.fishingPole.hasValue)
        assertFalse(dto.importantItems.fishingPole.value)

        assertTrue(dto.party.slots.hasValue)
        val slots = dto.party.slots.value
        assertEquals(3, slots.size)
        assertEquals(1, slots[0].digimonId.value)
        assertNotNull(slots[0].digimon.value)
        assertTrue(slots[1].digimonId.hasValue)
        assertNull(slots[1].digimonId.value)
        assertTrue(slots[1].digimon.hasValue)
        assertNull(slots[1].digimon.value)

        assertTrue(dto.digimonBattle.enemy.hasValue)
        assertNull(dto.digimonBattle.enemy.value)

        assertTrue(dto.cardBattle.id.hasValue)
        assertNull(dto.cardBattle.id.value)

        assertTrue(dto.auctions.divineBarrier.hasValue)
        assertTrue(dto.auctions.divineBarrier.value)

        assertTrue(dto.npcs.genji.hasValue)

        assertTrue(dto.journal.mainQuest.hasValue)
        assertEquals("MainQuest", dto.journal.mainQuest.value.id)
    }

    @Test
    fun `toDto does not change the state it reads`() {
        val state = sampleState()
        val untouched = sampleState()

        StateConverter.toDto(state)

        assertEquals(untouched, state)
    }
}
