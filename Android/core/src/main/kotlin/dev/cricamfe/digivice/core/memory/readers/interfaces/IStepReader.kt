package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource

/** Port of `Backend/Memory/Readers/Interfaces/IStepReader.cs`. */
interface IStepReader {
    fun read(addresses: StepAddresses): StepResource
}
