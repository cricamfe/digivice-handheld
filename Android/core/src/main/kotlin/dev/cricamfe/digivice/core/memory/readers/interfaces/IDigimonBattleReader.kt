package dev.cricamfe.digivice.core.memory.readers.interfaces

import dev.cricamfe.digivice.core.memory.addresses.DigimonBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource

/** Port of `Backend/Memory/Readers/Interfaces/IDigimonBattleReader.cs`. */
interface IDigimonBattleReader {
    fun read(digimonBattleAddresses: DigimonBattleAddresses, enemyAddresses: EnemyAddresses): DigimonBattleResource
}
