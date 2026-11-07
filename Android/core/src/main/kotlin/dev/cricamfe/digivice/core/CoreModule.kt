package dev.cricamfe.digivice.core

import dev.cricamfe.digivice.core.application.StateComposer
import dev.cricamfe.digivice.core.application.loaders.AuctionsLoader
import dev.cricamfe.digivice.core.application.loaders.CardBattleLoader
import dev.cricamfe.digivice.core.application.loaders.DigimonBattleLoader
import dev.cricamfe.digivice.core.application.loaders.DigimonLoader
import dev.cricamfe.digivice.core.application.loaders.ImportantItemsLoader
import dev.cricamfe.digivice.core.application.loaders.JournalLoader
import dev.cricamfe.digivice.core.application.loaders.NpcsLoader
import dev.cricamfe.digivice.core.application.loaders.PartyLoader
import dev.cricamfe.digivice.core.application.loaders.PlayerLoader
import dev.cricamfe.digivice.core.application.loaders.QuestLoader
import dev.cricamfe.digivice.core.application.providers.AuctionsProvider
import dev.cricamfe.digivice.core.application.providers.CardBattleProvider
import dev.cricamfe.digivice.core.application.providers.DigimonBattleProvider
import dev.cricamfe.digivice.core.application.providers.ImportantItemsProvider
import dev.cricamfe.digivice.core.application.providers.JournalProvider
import dev.cricamfe.digivice.core.application.providers.NpcsProvider
import dev.cricamfe.digivice.core.application.providers.PartyProvider
import dev.cricamfe.digivice.core.application.providers.PlayerProvider
import dev.cricamfe.digivice.core.memory.MemoryAccessor
import dev.cricamfe.digivice.core.memory.MemoryReader
import dev.cricamfe.digivice.core.memory.readers.AuctionsReader
import dev.cricamfe.digivice.core.memory.readers.CardBattleReader
import dev.cricamfe.digivice.core.memory.readers.DigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.readers.DigimonBattleReader
import dev.cricamfe.digivice.core.memory.readers.DigimonReader
import dev.cricamfe.digivice.core.memory.readers.DigimonSlotReader
import dev.cricamfe.digivice.core.memory.readers.EnemyReader
import dev.cricamfe.digivice.core.memory.readers.FieldActorsReader
import dev.cricamfe.digivice.core.memory.readers.FieldTaskProbe
import dev.cricamfe.digivice.core.memory.readers.QuestConditionsReader
import dev.cricamfe.digivice.core.memory.readers.PartnerTechniquesReader
import dev.cricamfe.digivice.core.memory.readers.ImportantItemsReader
import dev.cricamfe.digivice.core.memory.readers.InBattleReader
import dev.cricamfe.digivice.core.memory.readers.NpcsReader
import dev.cricamfe.digivice.core.memory.readers.PartyReader
import dev.cricamfe.digivice.core.memory.readers.PlayerPositionProbe
import dev.cricamfe.digivice.core.memory.readers.PlayerReader
import dev.cricamfe.digivice.core.memory.readers.QuestReader
import dev.cricamfe.digivice.core.memory.readers.RequisiteReader
import dev.cricamfe.digivice.core.memory.readers.StepReader
import dev.cricamfe.digivice.core.memory.readers.StoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.repositories.AddressesRepository
import dev.cricamfe.digivice.core.memory.repositories.IAddressesRepository

/**
 * Manual wiring of the core, in the same order and with the same dependencies as
 * `Backend/Infrastructure/DependencyInjection.cs` (without the framework: every object is built once, here).
 * The Windows/Duckstation session classes and the game loop have no part in it.
 *
 * The definitions are reached through functions so the core knows nothing about the file system or Android assets;
 * see [AddressesRepository] for the contract of [readDefinition] and [listDefinitions].
 *
 * @param accessor the RAM of the emulated console.
 * @param readDefinition returns the text of a definitions file, by path relative to the definitions folder.
 * @param listDefinitions returns the file names inside a definitions folder (a third parameter beyond the plan's
 *   two, because the repository must list the quest folders).
 */
class CoreModule(
    accessor: MemoryAccessor,
    readDefinition: (String) -> String,
    listDefinitions: (String) -> List<String>,
) {
    private val memoryReader = MemoryReader(accessor)

    /** The address definitions the readers use; the RetroArch connector reads the party slots through it. */
    val addressesRepository: IAddressesRepository = AddressesRepository(readDefinition, listDefinitions)

    private val digimonReader = DigimonReader(
        memoryReader,
        DigievolutionSlotReader(),
        StoredDigievolutionReader(),
        InBattleReader(memoryReader),
    )
    private val digimonSlotReader = DigimonSlotReader(memoryReader)
    private val partyReader = PartyReader(digimonSlotReader)
    private val digimonBattleReader = DigimonBattleReader(memoryReader, EnemyReader(memoryReader))
    private val requisiteReader = RequisiteReader(memoryReader)
    private val stepReader = StepReader(memoryReader, requisiteReader)
    private val playerReader = PlayerReader(memoryReader)
    private val cardBattleReader = CardBattleReader(memoryReader)
    private val importantItemsReader = ImportantItemsReader(memoryReader)
    private val auctionsReader = AuctionsReader(memoryReader)
    private val questReader = QuestReader(requisiteReader, stepReader)
    private val npcsReader = NpcsReader(memoryReader)

    private val playerLoader = PlayerLoader(addressesRepository, playerReader)
    private val importantItemsLoader = ImportantItemsLoader(addressesRepository, importantItemsReader)
    private val auctionsLoader = AuctionsLoader(addressesRepository, auctionsReader)
    private val npcsLoader = NpcsLoader(addressesRepository, npcsReader)
    private val questLoader = QuestLoader(addressesRepository, questReader)
    private val journalLoader = JournalLoader(questLoader)
    private val digimonLoader = DigimonLoader(addressesRepository, digimonReader)
    private val partyLoader = PartyLoader(addressesRepository, partyReader, digimonLoader)
    private val digimonBattleLoader = DigimonBattleLoader(addressesRepository, digimonBattleReader)
    private val cardBattleLoader = CardBattleLoader(addressesRepository, cardBattleReader)

    private val playerProvider = PlayerProvider(playerLoader)
    private val importantItemsProvider = ImportantItemsProvider(importantItemsLoader)
    private val partyProvider = PartyProvider(partyLoader)
    private val digimonBattleProvider = DigimonBattleProvider(digimonBattleLoader)
    private val cardBattleProvider = CardBattleProvider(cardBattleLoader)
    private val auctionsProvider = AuctionsProvider(auctionsLoader)
    private val npcsProvider = NpcsProvider(npcsLoader)
    private val journalProvider = JournalProvider(journalLoader)

    /** The characters of the map the field draws; see [GameLoop]. */
    val fieldActors = FieldActorsReader(memoryReader, addressesRepository)
    val fieldTask = FieldTaskProbe(memoryReader, addressesRepository)
    val partnerTechniques = PartnerTechniquesReader(memoryReader, addressesRepository)
    val questConditions = QuestConditionsReader(memoryReader, readDefinition)

    /** Follows the player's actor between full cycles; see [GameLoop]. */
    val playerPosition = PlayerPositionProbe(playerReader, addressesRepository)

    /** Reads the whole game state from RAM; each call reads memory again. */
    val stateComposer = StateComposer(
        playerProvider,
        importantItemsProvider,
        partyProvider,
        digimonBattleProvider,
        cardBattleProvider,
        auctionsProvider,
        npcsProvider,
        journalProvider,
    )
}
