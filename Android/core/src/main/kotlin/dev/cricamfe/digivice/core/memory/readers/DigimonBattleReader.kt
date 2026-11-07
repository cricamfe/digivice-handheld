package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.DigimonBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonBattleReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IEnemyReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.DigimonBattleResource

/** Port of `Backend/Memory/Readers/DigimonBattleReader.cs`. */
class DigimonBattleReader(
    private val memoryReader: IMemoryReader,
    private val enemyReader: IEnemyReader,
) : IDigimonBattleReader {
    override fun read(
        digimonBattleAddresses: DigimonBattleAddresses,
        enemyAddresses: EnemyAddresses,
    ): DigimonBattleResource {
        return DigimonBattleResource(
            field = memoryReader.readByte(digimonBattleAddresses.field),
            fieldAmount = memoryReader.readInt16(digimonBattleAddresses.fieldAmount).toInt(),
            activeFighter = memoryReader.readInt32(digimonBattleAddresses.activeFighter),
            stage = memoryReader.readInt32(digimonBattleAddresses.stage),
            enemy = enemyReader.read(enemyAddresses),
        )
    }
}
