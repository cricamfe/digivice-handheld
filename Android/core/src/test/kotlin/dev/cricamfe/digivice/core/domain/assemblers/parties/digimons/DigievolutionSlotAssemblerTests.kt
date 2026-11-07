package dev.cricamfe.digivice.core.domain.assemblers.parties.digimons

import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DigievolutionSlotAssemblerTests {
    @Test
    fun `assemble should use stored digievolution when id is found`() {
        val resource = DigievolutionSlotResource(index = 1, digievolutionId = 32)
        val storedDigievolutions = listOf(
            StoredDigievolutionResource(digievolutionId = 10, level = 3, dvexp = 50),
            StoredDigievolutionResource(digievolutionId = 32, level = 15, dvexp = 700),
        )

        val result = DigievolutionSlotAssembler.assemble(resource, storedDigievolutions)

        assertEquals(1, result.index)
        assertEquals(32, result.digievolutionId)
        assertNotNull(result.digievolution)
        assertEquals(15, result.digievolution!!.level)
        assertEquals(700, result.digievolution!!.dvexp)
    }

    @Test
    fun `assemble should use default level and dvexp when id is not stored`() {
        val resource = DigievolutionSlotResource(index = 2, digievolutionId = 32)
        val storedDigievolutions = listOf(StoredDigievolutionResource(digievolutionId = 10, level = 3, dvexp = 50))

        val result = DigievolutionSlotAssembler.assemble(resource, storedDigievolutions)

        assertEquals(2, result.index)
        assertEquals(32, result.digievolutionId)
        assertNotNull(result.digievolution)
        assertEquals(1, result.digievolution!!.level)
        assertEquals(0, result.digievolution!!.dvexp)
    }

    @ParameterizedTest
    @CsvSource("-1,0", "-2147483648,0", "0,0", "1,1")
    fun `assemble should clamp negative dvexp to zero`(rawDvexp: Int, expectedDvexp: Int) {
        val resource = DigievolutionSlotResource(index = 1, digievolutionId = 32)
        val storedDigievolutions = listOf(StoredDigievolutionResource(digievolutionId = 32, level = 4, dvexp = rawDvexp))

        val result = DigievolutionSlotAssembler.assemble(resource, storedDigievolutions)

        assertNotNull(result.digievolution)
        assertEquals(expectedDvexp, result.digievolution!!.dvexp)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `assemble should return null fields when digievolution id is not positive`(rawDigievolutionId: Int) {
        val resource = DigievolutionSlotResource(index = 3, digievolutionId = rawDigievolutionId)

        val result = DigievolutionSlotAssembler.assemble(resource, emptyList())

        assertEquals(3, result.index)
        assertNull(result.digievolutionId)
        assertNull(result.digievolution)
    }
}
