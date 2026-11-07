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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Port of `Backend/Memory/Repositories/AddressesRepository.cs`: loads the address definitions
 * (`Backend/Memory/Definitions`) on first use and caches them. A load that fails is not cached.
 *
 * The definitions are reached through two functions so the core stays free of any file system API; both take paths
 * relative to the definitions folder, with "/" separators (for example `"Parties/DigimonStatusAddresses.json"`):
 *  - [readText] returns the content of a definitions file. It must throw [java.io.FileNotFoundException] when the
 *    file does not exist.
 *  - [listFiles] returns the names (not paths) of the files in a definitions folder, in any order. It must throw
 *    [java.io.FileNotFoundException] when the folder does not exist; note that Android's `AssetManager.list`
 *    answers an empty array for a missing folder, so the Android adapter has to translate that.
 */
class AddressesRepository(
    private val readText: (relativePath: String) -> String,
    private val listFiles: (relativeFolder: String) -> List<String>,
) : IAddressesRepository {
    private val playerAddressesCache: PlayerAddresses by lazy { load("PlayerAddresses.json") { PlayerAddresses() } }
    private val importantItemsAddressesCache: ImportantItemsAddresses by lazy {
        load("ImportantItemsAddresses.json") { ImportantItemsAddresses() }
    }
    private val partyAddressesCache: PartyAddresses by lazy { load("PartyAddresses.json") { PartyAddresses() } }
    private val digimonStatusAddressesCache: DigimonStatusAddresses by lazy {
        load("Parties/DigimonStatusAddresses.json") { DigimonStatusAddresses() }
    }
    private val inBattleAddressesCache: InBattleAddresses by lazy {
        load("Parties/InBattleAddresses.json") { InBattleAddresses() }
    }
    private val digimonBattleAddressesCache: DigimonBattleAddresses by lazy {
        load("Battles/DigimonBattleAddresses.json") { DigimonBattleAddresses() }
    }
    private val enemyAddressesCache: EnemyAddresses by lazy { load("Battles/EnemyAddresses.json") { EnemyAddresses() } }
    private val digimonAddressesCache: Map<Int, DigimonAddress> by lazy {
        load("Parties/DigimonsAddresses.json") { emptyMap() }
    }
    private val mainQuestAddressesCache: QuestAddresses by lazy {
        load("Quests/MainQuestAddresses.json") { QuestAddresses() }
    }
    private val sideQuestAddressesCache: List<QuestAddresses> by lazy {
        loadAllQuestAddressesFromFolder("Quests/SideQuests")
    }
    private val legendaryWeaponAddressesCache: List<QuestAddresses> by lazy {
        loadAllQuestAddressesFromFolder("Quests/LegendaryWeapons")
    }
    private val driAgentAddressesCache: List<QuestAddresses> by lazy {
        loadAllQuestAddressesFromFolder("Quests/DriAgents")
    }
    private val duelIslandAddressesCache: List<QuestAddresses> by lazy {
        loadAllQuestAddressesFromFolder("Quests/DuelIsland")
    }
    private val auctionsAddressesCache: AuctionsAddresses by lazy {
        load("AuctionAddresses.json") { AuctionsAddresses() }
    }
    private val npcsAddressesCache: NpcsAddresses by lazy { load("NpcAddresses.json") { NpcsAddresses() } }
    private val cardBattleAddressesCache: CardBattleAddresses by lazy {
        load("CardBattleAddresses.json") { CardBattleAddresses() }
    }

    /** Parses a definitions file. A JSON `null` yields [default], like `Deserialize<T>(json) ?? new T()`. */
    private inline fun <reified T : Any> load(fileName: String, default: () -> T): T {
        val element = json.parseToJsonElement(readText(fileName))
        return if (element is JsonNull) default() else json.decodeFromJsonElement<T>(element)
    }

    private fun loadAllQuestAddressesFromFolder(relativeFolder: String): List<QuestAddresses> =
        listFiles(relativeFolder)
            .filter { it.endsWith(".json", ignoreCase = true) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .map { fileName -> load("$relativeFolder/$fileName") { QuestAddresses() } }

    override fun getPlayerAddresses(): PlayerAddresses = playerAddressesCache

    override fun getImportantItemsAddresses(): ImportantItemsAddresses = importantItemsAddressesCache

    override fun getPartyAddresses(): PartyAddresses = partyAddressesCache

    override fun getDigimonStatusAddresses(): DigimonStatusAddresses = digimonStatusAddressesCache

    override fun getInBattleAddresses(): InBattleAddresses = inBattleAddressesCache

    override fun getDigimonBattleAddresses(): DigimonBattleAddresses = digimonBattleAddressesCache

    override fun getEnemyAddresses(): EnemyAddresses = enemyAddressesCache

    override fun getDigimonsAddresses(): Map<Int, DigimonAddress> = digimonAddressesCache

    override fun getDigimonAddressById(id: Int): DigimonAddress? = getDigimonsAddresses()[id]

    override fun getMainQuest(): QuestAddresses = mainQuestAddressesCache

    override fun getAllSideQuests(): List<QuestAddresses> = sideQuestAddressesCache

    override fun getAllLegendaryWeapons(): List<QuestAddresses> = legendaryWeaponAddressesCache

    override fun getAllDriAgents(): List<QuestAddresses> = driAgentAddressesCache

    override fun getAllDuelIsland(): List<QuestAddresses> = duelIslandAddressesCache

    override fun getAuctionsAddresses(): AuctionsAddresses = auctionsAddressesCache

    override fun getNpcsAddresses(): NpcsAddresses = npcsAddressesCache

    override fun getCardBattleAddresses(): CardBattleAddresses = cardBattleAddressesCache

    private companion object {
        /** Like System.Text.Json's defaults, members that the types do not declare are ignored. */
        val json = Json { ignoreUnknownKeys = true }
    }
}
