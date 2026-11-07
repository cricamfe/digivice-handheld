package dev.cricamfe.digivice.core.domain.assemblers.parties.digimons

import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StoredDigievolutionAssemblerTests {
    @Test
    fun `assemble should map all fields correctly`() {
        val resource = StoredDigievolutionResource(digievolutionId = 386, level = 14)

        val result = StoredDigievolutionAssembler.assemble(resource)

        assertEquals(386, result.digievolutionId)
        assertEquals(14, result.level)
    }
}
