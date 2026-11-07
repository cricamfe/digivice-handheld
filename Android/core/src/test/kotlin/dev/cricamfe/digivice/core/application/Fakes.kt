package dev.cricamfe.digivice.core.application

import dev.cricamfe.digivice.core.application.loaders.interfaces.IAuctionsLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.ICardBattleLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IDigimonBattleLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IImportantItemsLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IJournalLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.INpcsLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IPartyLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IPlayerLoader
import dev.cricamfe.digivice.core.application.loaders.interfaces.IQuestLoader
import dev.cricamfe.digivice.core.application.providers.interfaces.IAuctionsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.ICardBattleProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IDigimonBattleProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IImportantItemsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IJournalProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.INpcsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IPartyProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IPlayerProvider
import dev.cricamfe.digivice.core.domain.models.Auctions
import dev.cricamfe.digivice.core.domain.models.CardBattle
import dev.cricamfe.digivice.core.domain.models.DigimonBattle
import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.domain.models.Journal
import dev.cricamfe.digivice.core.domain.models.Npcs
import dev.cricamfe.digivice.core.domain.models.Party
import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource
import dev.cricamfe.digivice.core.memory.resources.JournalResource
import dev.cricamfe.digivice.core.memory.resources.NpcsResource
import dev.cricamfe.digivice.core.memory.resources.PartyResource
import dev.cricamfe.digivice.core.memory.resources.PlayerResource
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

// Hand-written stand-ins for the Moq `Mock<ILoader>` / `Mock<IProvider>` of the C# tests. Each one answers the value it
// was given and counts how often it was asked, which replaces `Verify(..., Times.Once)`.

class FakeAuctionsLoader(private val result: AuctionsResource) : IAuctionsLoader {
    var calls = 0
    override fun load(): AuctionsResource = result.also { calls++ }
}

class FakeCardBattleLoader(private val result: CardBattleResource) : ICardBattleLoader {
    var calls = 0
    override fun load(): CardBattleResource = result.also { calls++ }
}

class FakeDigimonBattleLoader(private val result: DigimonBattleResource) : IDigimonBattleLoader {
    var calls = 0
    override fun load(): DigimonBattleResource = result.also { calls++ }
}

class FakeImportantItemsLoader(private val result: ImportantItemsResource) : IImportantItemsLoader {
    var calls = 0
    override fun load(): ImportantItemsResource = result.also { calls++ }
}

class FakeJournalLoader(private val result: JournalResource) : IJournalLoader {
    var calls = 0
    override fun load(): JournalResource = result.also { calls++ }
}

class FakeNpcsLoader(private val result: NpcsResource) : INpcsLoader {
    var calls = 0
    override fun load(): NpcsResource = result.also { calls++ }
}

class FakePartyLoader(private val result: PartyResource) : IPartyLoader {
    var calls = 0
    override fun load(): PartyResource = result.also { calls++ }
}

class FakePlayerLoader(private val result: PlayerResource) : IPlayerLoader {
    var calls = 0
    override fun load(): PlayerResource = result.also { calls++ }
}

class FakeQuestLoader(
    private val mainQuest: QuestResource,
    private val sideQuests: List<QuestResource>,
    private val legendaryWeapons: List<QuestResource>,
    private val driAgents: List<QuestResource>,
    private val duelIsland: List<QuestResource>,
) : IQuestLoader {
    override fun loadMainQuest(): QuestResource = mainQuest
    override fun loadSideQuests(): List<QuestResource> = sideQuests
    override fun loadLegendaryWeapons(): List<QuestResource> = legendaryWeapons
    override fun loadDriAgents(): List<QuestResource> = driAgents
    override fun loadDuelIsland(): List<QuestResource> = duelIsland
}

class FakePlayerProvider(private val result: Player) : IPlayerProvider {
    var calls = 0
    override fun get(): Player = result.also { calls++ }
}

class FakeImportantItemsProvider(private val result: ImportantItems) : IImportantItemsProvider {
    var calls = 0
    override fun get(): ImportantItems = result.also { calls++ }
}

class FakePartyProvider(private val result: Party) : IPartyProvider {
    var calls = 0
    override fun get(): Party = result.also { calls++ }
}

class FakeDigimonBattleProvider(private val result: DigimonBattle) : IDigimonBattleProvider {
    var calls = 0
    override fun get(): DigimonBattle = result.also { calls++ }
}

class FakeCardBattleProvider(private val result: CardBattle) : ICardBattleProvider {
    var calls = 0
    override fun get(): CardBattle = result.also { calls++ }
}

class FakeAuctionsProvider(private val result: Auctions) : IAuctionsProvider {
    var calls = 0
    override fun get(): Auctions = result.also { calls++ }
}

class FakeNpcsProvider(private val result: Npcs) : INpcsProvider {
    var calls = 0
    override fun get(): Npcs = result.also { calls++ }
}

class FakeJournalProvider(private val result: Journal) : IJournalProvider {
    var calls = 0
    override fun get(): Journal = result.also { calls++ }
}
