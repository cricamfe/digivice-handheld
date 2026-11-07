package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.NpcBattle
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.domain.models.battles.Enemy
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import dev.cricamfe.digivice.core.domain.models.journals.quests.Step
import dev.cricamfe.digivice.core.domain.models.parties.Digimon
import dev.cricamfe.digivice.core.domain.models.parties.DigimonSlot
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Attributes
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Elements
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Equipments
import dev.cricamfe.digivice.core.domain.models.parties.digimons.Vital
import dev.cricamfe.digivice.core.events.dto.AuctionsDto
import dev.cricamfe.digivice.core.events.dto.CardBattleDto
import dev.cricamfe.digivice.core.events.dto.DigimonBattleDto
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto
import dev.cricamfe.digivice.core.events.dto.JournalDto
import dev.cricamfe.digivice.core.events.dto.NpcsDto
import dev.cricamfe.digivice.core.events.dto.PartyDto
import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.StateDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/StateEventFactoryTests.cs`. */
class StateEventFactoryTests {
    @Test
    fun `create returns the initial state event when the previous state is null`() {
        val result = StateEventFactory.create(null, baseState())

        val event = result.single()
        assertEquals(EventType.InitialState, event.type)
        // StateDto has eight required, non nullable sections, so being a StateDto is the whole check.
        assertInstanceOf(StateDto::class.java, event.payload)
    }

    @Test
    fun `create returns no events when the state has no changes`() {
        assertTrue(StateEventFactory.create(baseState(), baseState()).isEmpty())
    }

    @Test
    fun `create returns a player changed event when only the player changes`() {
        val new = baseState().also { it.player.bits = 999 }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.PlayerChanged, event.type)
        assertInstanceOf(PlayerDto::class.java, event.payload)
    }

    @Test
    fun `create returns an important items changed event when only the important items change`() {
        val new = baseState().also { it.importantItems.treeBoots = true }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.ImportantItemsChanged, event.type)
        assertInstanceOf(ImportantItemsDto::class.java, event.payload)
    }

    @Test
    fun `create returns a party changed event when only the party changes`() {
        val new = baseState().also { it.party.slots[0].digimon!!.level = 22 }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.PartyChanged, event.type)
        assertInstanceOf(PartyDto::class.java, event.payload)
    }

    @Test
    fun `create returns a digimon battle changed event when only the digimon battle changes`() {
        val new = baseState().also { it.digimonBattle.enemy!!.speed = 84 }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.DigimonBattleChanged, event.type)
        assertInstanceOf(DigimonBattleDto::class.java, event.payload)
    }

    @Test
    fun `create returns a card battle changed event when only the card battle changes`() {
        val new = baseState().also { it.cardBattle.id = 11 }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.CardBattleChanged, event.type)
        assertInstanceOf(CardBattleDto::class.java, event.payload)
    }

    @Test
    fun `create returns a journal changed event when only the journal changes`() {
        val new = baseState().also { it.journal.mainQuest.steps[0].isDone = true }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.JournalChanged, event.type)
        assertInstanceOf(JournalDto::class.java, event.payload)
    }

    @Test
    fun `create returns an npcs changed event when only the npcs change`() {
        val new = baseState().also { it.npcs.genji.battles = listOf(NpcBattle(id = "first", won = true)) }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.NpcsChanged, event.type)
        assertInstanceOf(NpcsDto::class.java, event.payload)
    }

    @Test
    fun `create returns an auctions changed event when only the auctions change`() {
        val new = baseState().also { it.auctions.divineBarrier = true }

        val event = StateEventFactory.create(baseState(), new).single()

        assertEquals(EventType.AuctionsChanged, event.type)
        assertInstanceOf(AuctionsDto::class.java, event.payload)
    }

    @Test
    fun `create returns the events in domain order when multiple sections change`() {
        val new = baseState().also {
            it.player.bits = 999
            it.importantItems.asukaTrophy = true
            it.party.slots[0].digimon!!.level = 22
            it.digimonBattle.enemy!!.speed = 84
            it.cardBattle.id = 11
            it.auctions.divineBarrier = true
            it.npcs.genji.battles = listOf(NpcBattle(id = "first", won = true))
            it.journal.mainQuest.steps[0].isDone = true
        }

        val result = StateEventFactory.create(baseState(), new)

        assertEquals(
            listOf(
                EventType.PlayerChanged,
                EventType.ImportantItemsChanged,
                EventType.PartyChanged,
                EventType.DigimonBattleChanged,
                EventType.CardBattleChanged,
                EventType.AuctionsChanged,
                EventType.NpcsChanged,
                EventType.JournalChanged,
            ),
            result.map { it.type },
        )
    }

    @Test
    fun `create does not write to the states it compares`() {
        val previous = baseState()
        val new = baseState().also {
            it.player.bits = 999
            it.party.slots[0].digimon!!.level = 22
        }
        val previousBefore = baseState()
        val newBefore = baseState().also {
            it.player.bits = 999
            it.party.slots[0].digimon!!.level = 22
        }

        StateEventFactory.create(previous, new)
        StateEventFactory.create(null, new)

        assertEquals(previousBefore, previous)
        assertEquals(newBefore, new)
    }

    private fun baseState() = State(
        player = Player(bits = 100, mapId = "0001"),
        importantItems = ImportantItems(treeBoots = false, fishingPole = false, asukaTrophy = false),
        party = Party(
            slots = listOf(
                DigimonSlot(index = 1, digimonId = 1, digimon = baseDigimon()),
                DigimonSlot(index = 2, digimonId = null, digimon = null),
                DigimonSlot(index = 3, digimonId = null, digimon = null),
            ),
        ),
        digimonBattle = DigimonBattle(
            enemy = Enemy(id = 122, groupId = 201, speed = 70, hp = Vital(current = 600, max = 672)),
        ),
        cardBattle = CardBattle(),
        auctions = Auctions(),
        npcs = Npcs(),
        journal = Journal(
            mainQuest = Quest(id = "MainQuest", steps = listOf(Step(number = 1, isDone = false)), requisites = emptyList()),
            sideQuests = emptyList(),
        ),
    )

    private fun baseDigimon() = Digimon(
        level = 10,
        experience = 1000,
        activeDigievolutionId = 3,
        hp = Vital(current = 100, max = 100),
        mp = Vital(current = 50, max = 50),
        attributes = Attributes(strength = 5, defense = 5, spirit = 5, wisdom = 5, speed = 5, charisma = 5),
        elements = Elements(fire = 1, water = 1, ice = 1, wind = 1, thunder = 1, machine = 1, dark = 1),
        equipments = Equipments(),
        digievolutions = emptyList(),
    )
}
