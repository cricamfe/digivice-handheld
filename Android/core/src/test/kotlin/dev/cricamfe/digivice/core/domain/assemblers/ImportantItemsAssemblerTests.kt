package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportantItemsAssemblerTests {
    @Test
    fun `assemble should map non zero bytes to true`() {
        val resource = ImportantItemsResource(
            treeBoots = 0x01u,
            fishingPole = 0x02u,
            asukaTrophy = 0xFFu,
            sunTrophy = 0x01u,
        )

        val result = ImportantItemsAssembler.assemble(resource)

        assertTrue(result.treeBoots)
        assertTrue(result.fishingPole)
        assertTrue(result.asukaTrophy)
        assertTrue(result.sunTrophy)
    }

    @Test
    fun `assemble should map zero to false`() {
        val resource = ImportantItemsResource(
            treeBoots = 0x00u,
            fishingPole = 0x00u,
            asukaTrophy = 0x00u,
            sunTrophy = 0x00u,
        )

        val result = ImportantItemsAssembler.assemble(resource)

        assertFalse(result.treeBoots)
        assertFalse(result.fishingPole)
        assertFalse(result.asukaTrophy)
        assertFalse(result.sunTrophy)
    }
}
