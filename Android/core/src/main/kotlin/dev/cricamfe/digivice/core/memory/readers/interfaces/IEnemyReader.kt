package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource

/** Port of `Backend/Memory/Readers/Interfaces/IEnemyReader.cs`. */
interface IEnemyReader {
    fun read(addresses: EnemyAddresses): EnemyResource
}
