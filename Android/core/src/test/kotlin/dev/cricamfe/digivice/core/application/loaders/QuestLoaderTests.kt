package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.readers.QuestReader
import dev.cricamfe.digivice.core.memory.readers.RequisiteReader
import dev.cricamfe.digivice.core.memory.readers.StepReader
import dev.cricamfe.digivice.core.memory.resources.journals.QuestResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestLoaderTests : LoaderIntegrationTestBase() {
    private fun createQuestLoader(memoryReader: FakeMemoryReader): QuestLoader {
        val requisiteReader = RequisiteReader(memoryReader)
        val stepReader = StepReader(memoryReader, requisiteReader)
        return QuestLoader(createAddressesRepository(), QuestReader(requisiteReader, stepReader))
    }

    private fun List<QuestResource>.single(id: String) = single { it.id == id }

    private fun valueOf(step: StepResource) = step.value.toInt()

    @Test
    fun `load main quest should integrate quest addresses and reader pipeline`() {
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B3B6L] = 0x80u
            bytes[0x0004B3B7L] = 0x01u
        }
        val questLoader = createQuestLoader(memoryReader)

        val mainQuest = questLoader.loadMainQuest()

        assertEquals("mainQuest", mainQuest.id)
        assertTrue(mainQuest.requisites.isEmpty())
        assertEquals(133, mainQuest.steps.size)
        assertEquals(1, mainQuest.steps[0].number)
        assertEquals(0x80, valueOf(mainQuest.steps[0]))
        assertEquals(2, mainQuest.steps[1].number)
        assertEquals(0x01, valueOf(mainQuest.steps[1]))
        assertEquals(133, mainQuest.steps.last().number)
    }

    @Test
    fun `load side quests should integrate side quest addresses and reader pipeline`() {
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x00048F42L] = 1u
            bytes[0x04B3B0L] = 0xA4u
            bytes[0x048E57L] = 1u
            bytes[0x048E09L] = 1u
            bytes[0x00048DB7L] = 0u
            bytes[0x048DB5L] = 1u
        }
        val questLoader = createQuestLoader(memoryReader)

        val sideQuests = questLoader.loadSideQuests()

        assertEquals(4, sideQuests.size)

        val folderBag = sideQuests.single("folderBag")
        assertTrue(folderBag.requisites.isEmpty())
        assertEquals(1, folderBag.steps.size)
        assertEquals(1, valueOf(folderBag.steps[0]))

        val treeBoots = sideQuests.single("treeBoots")
        assertEquals(1, treeBoots.requisites.size)
        assertEquals("folderBag", treeBoots.requisites[0].id)
        assertEquals(1, treeBoots.requisites[0].value.toInt())
        assertEquals(7, treeBoots.steps.size)
        assertEquals(0x04, valueOf(treeBoots.steps[0]))
        assertEquals(0xA0, valueOf(treeBoots.steps[3]))
        assertEquals(0x80, valueOf(treeBoots.steps[4]))

        val fishingPole = sideQuests.single("fishingPole")
        assertEquals(1, fishingPole.requisites.size)
        assertEquals(3, fishingPole.steps.size)
        assertEquals(3, fishingPole.steps[2].requisites.size)
        assertEquals("bambooSpear", fishingPole.steps[2].requisites[0].id)
        assertEquals(1, fishingPole.steps[2].requisites[0].value.toInt())
        assertEquals("spiderWeb", fishingPole.steps[2].requisites[1].id)
        assertEquals(1, fishingPole.steps[2].requisites[1].value.toInt())
        assertEquals("redSnapper", fishingPole.steps[2].requisites[2].id)
        assertEquals(0, fishingPole.steps[2].requisites[2].value.toInt())
        assertEquals(1, valueOf(fishingPole.steps[2]))
    }

    @Test
    fun `load legendary weapons should integrate legendary weapon addresses and reader pipeline`() {
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B38EL] = 0x1Fu
        }
        val questLoader = createQuestLoader(memoryReader)

        val legendaryWeapons = questLoader.loadLegendaryWeapons()

        assertEquals(5, legendaryWeapons.size)

        val eternally = legendaryWeapons.single("eternally")
        assertTrue(eternally.requisites.isEmpty())
        assertEquals(1, eternally.steps.size)
        assertEquals(1, eternally.steps[0].number)
        assertEquals(0x01, valueOf(eternally.steps[0]))

        fun assertWeapon(id: String, requisiteId: String, stepValue: Int) {
            val weapon = legendaryWeapons.single(id)
            assertEquals(1, weapon.requisites.size)
            assertEquals(requisiteId, weapon.requisites[0].id)
            assertEquals(0, weapon.requisites[0].value.toInt())
            assertEquals(1, weapon.steps.size)
            assertEquals(1, weapon.steps[0].number)
            assertEquals(stepValue, valueOf(weapon.steps[0]))
        }

        assertWeapon("invincible", "seiryuLeader", 0x02)
        assertWeapon("muramasa", "smellHerb", 0x04)
        assertWeapon("superNova", "submarimon", 0x08)
        assertWeapon("punishment", "numemon", 0x10)
    }

    @Test
    fun `load dri agents should integrate dri agent addresses and reader pipeline`() {
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B38CL] = 0xFFu
            bytes[0x0004B3B1L] = 0xFFu
            bytes[0x0004B3B7L] = 0xFCu
            bytes[0x0004B3B8L] = 0x03u
            bytes[0x00048DD2L] = 1u
            bytes[0x0004A7E0L] = 0x08u
            bytes[0x00048DB6L] = 1u
            bytes[0x0004A028L] = 0x06u
            bytes[0x00048DD3L] = 1u
            bytes[0x0004A404L] = 0x07u
            bytes[0x00048F3BL] = 1u
            bytes[0x00049870L] = 0x04u
            bytes[0x00048F18L] = 1u
            bytes[0x00049C4CL] = 0x05u
            bytes[0x00048DC3L] = 1u
            bytes[0x00049494L] = 0x03u
            bytes[0x00048DD6L] = 1u
            bytes[0x0004ABBCL] = 0x09u
            bytes[0x00048DD7L] = 1u
            bytes[0x0004AF98L] = 0x0Au
        }
        val questLoader = createQuestLoader(memoryReader)

        val driAgents = questLoader.loadDriAgents()

        assertEquals(8, driAgents.size)

        fun assertAgent(id: String, requisiteId: String, stepValues: List<Int>, ddnaId: String) {
            val agent = driAgents.single(id)
            assertEquals(1, agent.requisites.size)
            assertEquals(requisiteId, agent.requisites[0].id)
            assertEquals(0, agent.requisites[0].value.toInt())
            assertEquals(3, agent.steps.size)
            assertEquals(stepValues, agent.steps.map { valueOf(it) })
            assertEquals(1, agent.steps[2].requisites.size)
            assertEquals(ddnaId, agent.steps[2].requisites[0].id)
            assertEquals(1, agent.steps[2].requisites[0].value.toInt())
        }

        assertAgent("driAgentGuilmon", "seiryuLeader", listOf(0x02, 0x08, 0x08), "guilmonDDNA")
        assertAgent("driAgentAgumon", "seiryuLeader", listOf(0x01, 0x04, 0x06), "agumonDDNA")
        assertAgent("driAgentVeemon", "byakkoLeader", listOf(0x02, 0x02, 0x07), "veemonDDNA")
        assertAgent("driAgentKumamon", "submarimon", listOf(0x20, 0x01, 0x04), "kumamonDDNA")
        assertAgent("driAgentMonmon", "submarimon", listOf(0x40, 0x80, 0x05), "monmonDDNA")
        assertAgent("driAgentKotemon", "submarimon", listOf(0x10, 0x40, 0x03), "kotemonDDNA")
        assertAgent("driAgentRenamon", "submarimon", listOf(0x08, 0x20, 0x09), "renamonDDNA")
        assertAgent("driAgentPatamon", "submarimon", listOf(0x04, 0x10, 0x0A), "patamonDDNA")
    }

    @Test
    fun `load duel island should integrate asuka trophy addresses and reader pipeline`() {
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B3B2L] = 0xCFu
            bytes[0x0004B3B3L] = 0x0Fu
            bytes[0x00048DC2L] = 0x01u
            bytes[0x0004B3BFL] = 0x20u
        }
        val questLoader = createQuestLoader(memoryReader)

        val duelIsland = questLoader.loadDuelIsland()

        assertEquals(5, duelIsland.size)

        val asukaTrophy = duelIsland.single("asukaTrophy")
        assertTrue(asukaTrophy.requisites.isEmpty())
        assertEquals(6, asukaTrophy.steps.size)
        assertEquals(listOf(0x80, 0x01, 0x02, 0x04, 0x08, 1), asukaTrophy.steps.map { valueOf(it) })

        val asukaRematch = duelIsland.single("asukaRematch")
        assertEquals(1, asukaRematch.requisites.size)
        assertEquals("asukaTrophy", asukaRematch.requisites[0].id)
        assertEquals(1, asukaRematch.requisites[0].value.toInt())
        assertEquals(6, asukaRematch.steps.size)
        assertEquals(listOf(0x80, 0x01, 0x02, 0x04, 0x08, 0x20), asukaRematch.steps.map { valueOf(it) })
    }
}
