package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource

/** Port of `Backend/Memory/Readers/Interfaces/IRequisiteReader.cs`. */
interface IRequisiteReader {
    fun read(addresses: RequisiteAddresses): RequisiteResource
}
