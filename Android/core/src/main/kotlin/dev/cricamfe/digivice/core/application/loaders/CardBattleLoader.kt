package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.ICardBattleLoader
import dev.cricamfe.digivice.core.memory.resources.CardBattleResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.ICardBattleReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/CardBattleLoader.cs`. */
class CardBattleLoader(
    private val addressesRepository: IAddressesRepository,
    private val cardBattleReader: ICardBattleReader,
) : ICardBattleLoader {
    override fun load(): CardBattleResource = cardBattleReader.read(addressesRepository.getCardBattleAddresses())
}
