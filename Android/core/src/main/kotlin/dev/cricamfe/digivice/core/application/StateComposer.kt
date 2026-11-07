package dev.cricamfe.digivice.core.application

import dev.cricamfe.digivice.core.application.providers.interfaces.IAuctionsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.ICardBattleProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IDigimonBattleProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IImportantItemsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IJournalProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.INpcsProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IPartyProvider
import dev.cricamfe.digivice.core.application.providers.interfaces.IPlayerProvider
import dev.cricamfe.digivice.core.domain.models.State

/** Port of `Backend/Application/StateComposer.cs`: asks every provider once and gathers the answers into a [State]. */
class StateComposer(
    private val playerProvider: IPlayerProvider,
    private val importantItemsProvider: IImportantItemsProvider,
    private val partyProvider: IPartyProvider,
    private val digimonBattleProvider: IDigimonBattleProvider,
    private val cardBattleProvider: ICardBattleProvider,
    private val auctionsProvider: IAuctionsProvider,
    private val npcsProvider: INpcsProvider,
    private val journalProvider: IJournalProvider,
) {
    fun compose(): State = State(
        player = playerProvider.get(),
        importantItems = importantItemsProvider.get(),
        party = partyProvider.get(),
        digimonBattle = digimonBattleProvider.get(),
        cardBattle = cardBattleProvider.get(),
        auctions = auctionsProvider.get(),
        npcs = npcsProvider.get(),
        journal = journalProvider.get(),
    )
}
