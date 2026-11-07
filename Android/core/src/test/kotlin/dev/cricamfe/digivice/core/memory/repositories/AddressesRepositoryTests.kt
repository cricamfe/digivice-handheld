package dev.cricamfe.digivice.core.memory.repositories

import dev.cricamfe.digivice.core.memory.DirectoryDefinitions
import dev.cricamfe.digivice.core.memory.addresses.ImportantItemsAddresses
import dev.cricamfe.digivice.core.memory.addresses.PartyAddresses
import dev.cricamfe.digivice.core.memory.addresses.PlayerAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.QuestAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonAddress
import dev.cricamfe.digivice.core.memory.addresses.parties.DigimonStatusAddresses
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.FileNotFoundException

class AddressesRepositoryTests {
    /** Like System.Text.Json's default serialization, the default-valued members are written too. */
    private val json = Json { encodeDefaults = true }

    @TempDir
    lateinit var tempDirectory: File

    private lateinit var repository: AddressesRepository

    @BeforeEach
    fun setUp() {
        File(tempDirectory, "Parties").mkdirs()
        File(tempDirectory, "Quests/SideQuests").mkdirs()
        repository = DirectoryDefinitions(tempDirectory).repository()
    }

    private fun write(relativePath: String, content: String) = File(tempDirectory, relativePath).writeText(content)

    @Test
    fun `getPlayerAddresses should load and deserialize correctly`() {
        val fakePlayer = PlayerAddresses(
            bits = 0x00048DA0,
            mapId = 0x0004B3F8,
            previousMapId = 0x0004B400,
            seabedRoute = 0x00048D78,
            mapVariant = 0x00048D7A,
            taskRegistry = 0x0004B610,
            actorTileX = 0x00000058,
            actorTileY = 0x0000005C,
        )
        write("PlayerAddresses.json", json.encodeToString(fakePlayer))

        val result = repository.getPlayerAddresses()

        assertEquals(0x00048DA0L, result.bits)
        assertEquals(0x0004B3F8L, result.mapId)
        assertEquals(0x0004B400L, result.previousMapId)
        assertEquals(0x00048D78L, result.seabedRoute)
        assertEquals(0x00048D7AL, result.mapVariant)
        assertEquals(0x0004B610L, result.taskRegistry)
        assertEquals(0x58L, result.actorTileX)
        assertEquals(0x5CL, result.actorTileY)
    }

    @Test
    fun `getPlayerAddresses should parse hex string addresses`() {
        write(
            "PlayerAddresses.json",
            """{ "Bits": "0x00048DA0", "MapId": "0x0004B3F8", "PreviousMapId": "0x0004B400", "SeabedRoute": "0x00048D78", "MapVariant": "0x00048D7A", "TaskRegistry": "0x0004B610", "ActorTileX": "0x00000058", "ActorTileY": "0x0000005C" }""",
        )

        val result = repository.getPlayerAddresses()

        assertEquals(0x00048DA0L, result.bits)
        assertEquals(0x0004B3F8L, result.mapId)
        assertEquals(0x00048D7AL, result.mapVariant)
        assertEquals(0x0004B610L, result.taskRegistry)
        assertEquals(0x58L, result.actorTileX)
        assertEquals(0x5CL, result.actorTileY)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\"0xFF\"", "\"255\"", "255"])
    fun `getPartyAddresses should parse empty slot id from hex or decimal`(emptySlotIdJson: String) {
        write(
            "PartyAddresses.json",
            """{ "Slots": [{ "Index": 1, "Address": "0x00048DA4" }], "EmptySlotId": $emptySlotIdJson }""",
        )

        val result = repository.getPartyAddresses()

        assertEquals(0xFF, result.emptySlotId)
        assertEquals(0x00048DA4L, result.slots.single().address)
    }

    @Test
    fun `getImportantItemsAddresses should load and deserialize correctly`() {
        val fakeImportantItems = ImportantItemsAddresses(
            treeBoots = 0x00048DB4,
            fishingPole = 0x00048DB5,
            asukaTrophy = 0x00048DC2,
            sunTrophy = 0x00048DC4,
        )
        write("ImportantItemsAddresses.json", json.encodeToString(fakeImportantItems))

        val result = repository.getImportantItemsAddresses()

        assertEquals(0x00048DB4L, result.treeBoots)
        assertEquals(0x00048DB5L, result.fishingPole)
        assertEquals(0x00048DC2L, result.asukaTrophy)
        assertEquals(0x00048DC4L, result.sunTrophy)
    }

    @Test
    fun `getPartyAddresses should load and deserialize correctly`() {
        val fakeParty = PartyAddresses(emptySlotId = 99, slots = emptyList())
        write("PartyAddresses.json", json.encodeToString(fakeParty))

        val result = repository.getPartyAddresses()

        assertEquals(99, result.emptySlotId)
    }

    @Test
    fun `getDigimonStatusAddresses should load and deserialize correctly`() {
        write("Parties/DigimonStatusAddresses.json", json.encodeToString(DigimonStatusAddresses()))

        val result = repository.getDigimonStatusAddresses()

        assertNotNull(result)
    }

    @Test
    fun `getDigimonsAddresses should load and deserialize correctly`() {
        val fakeDigimons = mapOf(
            1 to DigimonAddress(name = "Agumon", memoryBlockAddress = 0x800100, blastAddress = 0x00042B76),
            2 to DigimonAddress(name = "Gabumon", memoryBlockAddress = 0x800200, blastAddress = 0x00042B78),
        )
        write("Parties/DigimonsAddresses.json", json.encodeToString(fakeDigimons))

        val result = repository.getDigimonsAddresses()

        assertEquals(2, result.size)
        assertEquals("Agumon", result.getValue(1).name)
        assertEquals(0x800100L, result.getValue(1).memoryBlockAddress)
        assertEquals(0x00042B76L, result.getValue(1).blastAddress)
        assertEquals(0x00042B78L, result.getValue(2).blastAddress)
    }

    @Test
    fun `getDigimonAddressById should return correct address when id exists`() {
        val fakeDigimons = mapOf(3 to DigimonAddress(name = "Patamon", memoryBlockAddress = 0x800300))
        write("Parties/DigimonsAddresses.json", json.encodeToString(fakeDigimons))

        val result = repository.getDigimonAddressById(3)

        assertNotNull(result)
        assertEquals("Patamon", result!!.name)
        assertEquals(0x800300L, result.memoryBlockAddress)
    }

    @Test
    fun `getDigimonAddressById should return null when id does not exist`() {
        write("Parties/DigimonsAddresses.json", json.encodeToString(emptyMap<Int, DigimonAddress>()))

        val result = repository.getDigimonAddressById(99)

        assertNull(result)
    }

    @Test
    fun `getMainQuest should load and deserialize correctly`() {
        write("Quests/MainQuestAddresses.json", json.encodeToString(QuestAddresses(id = "Main1")))

        val result = repository.getMainQuest()

        assertEquals("Main1", result.id)
    }

    @Test
    fun `getAllSideQuests should load all side quests correctly`() {
        write("Quests/SideQuests/FolderBagAddresses.json", json.encodeToString(QuestAddresses(id = "FolderBag")))
        write("Quests/SideQuests/TreeBootsAddresses.json", json.encodeToString(QuestAddresses(id = "TreeBoots")))
        write("Quests/SideQuests/FishingPoleAddresses.json", json.encodeToString(QuestAddresses(id = "FishingPole")))

        val result = repository.getAllSideQuests()

        assertEquals(3, result.size)
        assertEquals(setOf("FolderBag", "TreeBoots", "FishingPole"), result.map { it.id }.toSet())
    }

    @Test
    fun `getAllSideQuests should cache loaded list`() {
        write("Quests/SideQuests/FolderBagAddresses.json", json.encodeToString(QuestAddresses(id = "FolderBag")))

        val firstResult = repository.getAllSideQuests()
        val secondResult = repository.getAllSideQuests()

        assertSame(firstResult, secondResult)
    }

    @Test
    fun `getAllSideQuests should order quests by file name alphabetically`() {
        write("Quests/SideQuests/TreeBootsAddresses.json", json.encodeToString(QuestAddresses(id = "TreeBoots")))
        write("Quests/SideQuests/FishingPoleAddresses.json", json.encodeToString(QuestAddresses(id = "FishingPole")))
        write("Quests/SideQuests/folderBagAddresses.json", json.encodeToString(QuestAddresses(id = "FolderBag")))

        val result = repository.getAllSideQuests()

        assertEquals(listOf("FishingPole", "FolderBag", "TreeBoots"), result.map { it.id })
    }

    @Test
    fun `getAllSideQuests should ignore files that are not json`() {
        write("Quests/SideQuests/FolderBagAddresses.json", json.encodeToString(QuestAddresses(id = "FolderBag")))
        write("Quests/SideQuests/notes.txt", "not a quest")

        val result = repository.getAllSideQuests()

        assertEquals(listOf("FolderBag"), result.map { it.id })
    }

    @Test
    fun `getAllSideQuests should throw FileNotFoundException when folder is missing`() {
        // The C# test expects DirectoryNotFoundException. The JVM has no such type, so the listFiles contract is
        // "throw FileNotFoundException when the folder does not exist" and the repository lets it through.
        File(tempDirectory, "Quests/SideQuests").deleteRecursively()

        assertThrows(FileNotFoundException::class.java) { repository.getAllSideQuests() }
    }

    @Test
    fun `getPlayerAddresses should cache loaded instance`() {
        write("PlayerAddresses.json", json.encodeToString(PlayerAddresses(bits = 0x00048DA0)))

        val firstResult = repository.getPlayerAddresses()
        val secondResult = repository.getPlayerAddresses()

        assertSame(firstResult, secondResult)
    }

    @Test
    fun `getPlayerAddresses should throw FileNotFoundException when file is missing`() {
        assertThrows(FileNotFoundException::class.java) { repository.getPlayerAddresses() }
    }

    @Test
    fun `getPlayerAddresses should return empty instance when json is null`() {
        write("PlayerAddresses.json", "null")

        val result = repository.getPlayerAddresses()

        assertEquals(0L, result.bits)
        assertEquals(0L, result.mapId)
    }

    @Test
    fun `getDigimonsAddresses should return empty map when json is null`() {
        write("Parties/DigimonsAddresses.json", "null")

        assertEquals(emptyMap<Int, DigimonAddress>(), repository.getDigimonsAddresses())
    }

    @Test
    fun `unknown json members are ignored like System Text Json`() {
        write("PlayerAddresses.json", """{ "Bits": "0x10", "SomethingNew": 1 }""")

        assertEquals(0x10L, repository.getPlayerAddresses().bits)
    }

    @Test
    fun `a failed load is not cached and can succeed later`() {
        assertThrows(FileNotFoundException::class.java) { repository.getPlayerAddresses() }

        write("PlayerAddresses.json", """{ "Bits": "0x10" }""")

        assertEquals(0x10L, repository.getPlayerAddresses().bits)
    }
}
