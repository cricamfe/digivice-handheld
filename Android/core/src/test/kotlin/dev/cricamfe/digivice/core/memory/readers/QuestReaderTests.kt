package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeRequisiteReader
import dev.cricamfe.digivice.core.memory.FakeStepReader
import dev.cricamfe.digivice.core.memory.addresses.journals.QuestAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestReaderTests {
    @Test
    fun `read should map quest resource correctly`() {
        val reqAddresses = RequisiteAddresses(id = "Req1", address = 0x4000)
        val stepAddresses = StepAddresses(number = 1, address = 0x5000)
        val addresses = QuestAddresses(
            id = "QuestA",
            requisites = listOf(reqAddresses),
            steps = listOf(stepAddresses),
        )
        val expectedRequisiteResource = RequisiteResource(id = "Req1", value = 1u)
        val expectedStepResource = StepResource(number = 1, value = 3u)
        val requisiteReader = FakeRequisiteReader(mapOf(reqAddresses to expectedRequisiteResource))
        val stepReader = FakeStepReader(mapOf(stepAddresses to expectedStepResource))

        val result = QuestReader(requisiteReader, stepReader).read(addresses)

        assertEquals("QuestA", result.id)
        assertEquals(listOf(expectedRequisiteResource), result.requisites)
        assertEquals(listOf(expectedStepResource), result.steps)
    }
}
