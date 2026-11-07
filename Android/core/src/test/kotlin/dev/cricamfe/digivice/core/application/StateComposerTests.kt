package dev.cricamfe.digivice.core.application

import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.Npc
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.domain.models.journals.Quest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class StateComposerTests {
    @Test
    fun `compose should return state with provider results`() {
        val player = Player(bits = 123, mapId = "0001")
        val importantItems = ImportantItems(treeBoots = true)
        val party = Party(slots = emptyList())
        val digimonBattle = DigimonBattle()
        val cardBattle = CardBattle(id = 0)
        val auctions = Auctions(divineBarrier = true)
        val npcs = Npcs(genji = Npc())
        val journal = Journal(mainQuest = Quest(id = "MainQuest"), sideQuests = emptyList())

        val playerProvider = FakePlayerProvider(player)
        val importantItemsProvider = FakeImportantItemsProvider(importantItems)
        val partyProvider = FakePartyProvider(party)
        val digimonBattleProvider = FakeDigimonBattleProvider(digimonBattle)
        val cardBattleProvider = FakeCardBattleProvider(cardBattle)
        val auctionsProvider = FakeAuctionsProvider(auctions)
        val npcsProvider = FakeNpcsProvider(npcs)
        val journalProvider = FakeJournalProvider(journal)

        val composer = StateComposer(
            playerProvider,
            importantItemsProvider,
            partyProvider,
            digimonBattleProvider,
            cardBattleProvider,
            auctionsProvider,
            npcsProvider,
            journalProvider,
        )

        val state = composer.compose()

        assertSame(player, state.player)
        assertSame(importantItems, state.importantItems)
        assertSame(party, state.party)
        assertSame(digimonBattle, state.digimonBattle)
        assertSame(cardBattle, state.cardBattle)
        assertSame(auctions, state.auctions)
        assertSame(npcs, state.npcs)
        assertSame(journal, state.journal)
        assertEquals(1, playerProvider.calls)
        assertEquals(1, importantItemsProvider.calls)
        assertEquals(1, partyProvider.calls)
        assertEquals(1, digimonBattleProvider.calls)
        assertEquals(1, cardBattleProvider.calls)
        assertEquals(1, auctionsProvider.calls)
        assertEquals(1, npcsProvider.calls)
        assertEquals(1, journalProvider.calls)
    }
}
