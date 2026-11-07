package dev.cricamfe.digivice.core.application.providers

import dev.cricamfe.digivice.core.application.FakeImportantItemsLoader
import dev.cricamfe.digivice.core.memory.resources.ImportantItemsResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportantItemsProviderTests {
    @Test
    fun `get should load resource and apply important items assembler`() {
        val resource = ImportantItemsResource(
            treeBoots = 0x01u,
            fishingPole = 0x00u,
            asukaTrophy = 0x02u,
            sunTrophy = 0x00u,
        )
        val loader = FakeImportantItemsLoader(resource)

        val result = ImportantItemsProvider(loader).get()

        assertTrue(result.treeBoots)
        assertFalse(result.fishingPole)
        assertTrue(result.asukaTrophy)
        assertFalse(result.sunTrophy)
        assertEquals(1, loader.calls)
    }
}
