package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.journals.QuestAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IQuestReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IRequisiteReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStepReader
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Memory/Readers/QuestReader.cs`. */
class QuestReader(
    private val requisiteReader: IRequisiteReader,
    private val stepReader: IStepReader,
) : IQuestReader {
    override fun read(addresses: QuestAddresses): QuestResource {
        return QuestResource(
            id = addresses.id,
            requisites = addresses.requisites.map(requisiteReader::read),
            steps = addresses.steps.map(stepReader::read),
        )
    }
}
