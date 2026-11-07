package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.application.loaders.interfaces.IQuestLoader
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.readers.interfaces.IQuestReader
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/** Port of `Backend/Application/Loaders/QuestLoader.cs`. */
class QuestLoader(
    private val addressesRepository: IAddressesRepository,
    private val questReader: IQuestReader,
) : IQuestLoader {
    override fun loadMainQuest(): QuestResource = questReader.read(addressesRepository.getMainQuest())

    override fun loadSideQuests(): List<QuestResource> =
        addressesRepository.getAllSideQuests().map(questReader::read)

    override fun loadLegendaryWeapons(): List<QuestResource> =
        addressesRepository.getAllLegendaryWeapons().map(questReader::read)

    override fun loadDriAgents(): List<QuestResource> =
        addressesRepository.getAllDriAgents().map(questReader::read)

    override fun loadDuelIsland(): List<QuestResource> =
        addressesRepository.getAllDuelIsland().map(questReader::read)
}
