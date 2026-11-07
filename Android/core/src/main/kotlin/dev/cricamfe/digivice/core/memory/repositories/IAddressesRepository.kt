package dev.cricamfe.digivice.core.memory.repositories

import dev.cricamfe.digivice.core.memory.addresses.AuctionsAddresses
import dev.cricamfe.digivice.core.memory.addresses.CardBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.DigimonBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.addresses.ImportantItemsAddresses
import dev.cricamfe.digivice.core.memory.addresses.NpcsAddresses
import dev.cricamfe.digivice.core.memory.addresses.PartyAddresses
import dev.cricamfe.digivice.core.memory.addresses.PlayerAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.QuestAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonAddress
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonStatusAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses

/** Port of `Backend/Memory/Repositories/IAddressesRepository.cs`. */
interface IAddressesRepository {
    fun getPlayerAddresses(): PlayerAddresses
    fun getImportantItemsAddresses(): ImportantItemsAddresses
    fun getPartyAddresses(): PartyAddresses
    fun getDigimonStatusAddresses(): DigimonStatusAddresses
    fun getInBattleAddresses(): InBattleAddresses
    fun getDigimonBattleAddresses(): DigimonBattleAddresses
    fun getEnemyAddresses(): EnemyAddresses
    fun getDigimonsAddresses(): Map<Int, DigimonAddress>
    fun getDigimonAddressById(id: Int): DigimonAddress?
    fun getMainQuest(): QuestAddresses
    fun getAllSideQuests(): List<QuestAddresses>
    fun getAllLegendaryWeapons(): List<QuestAddresses>
    fun getAllDriAgents(): List<QuestAddresses>
    fun getAllDuelIsland(): List<QuestAddresses>
    fun getAuctionsAddresses(): AuctionsAddresses
    fun getNpcsAddresses(): NpcsAddresses
    fun getCardBattleAddresses(): CardBattleAddresses
}
