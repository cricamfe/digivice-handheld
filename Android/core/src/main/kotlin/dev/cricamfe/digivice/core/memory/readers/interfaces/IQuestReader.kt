package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.journals.QuestAddresses
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource

/** Port of `Backend/Memory/Readers/Interfaces/IQuestReader.cs`. */
interface IQuestReader {
    fun read(addresses: QuestAddresses): QuestResource
}
