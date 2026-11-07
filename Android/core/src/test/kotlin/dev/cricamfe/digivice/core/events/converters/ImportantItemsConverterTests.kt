package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/ImportantItemsConverterTests.cs`. */
class ImportantItemsConverterTests {
    @Test
    fun `toDto maps all important items fields`() {
        val importantItems = ImportantItems(
            treeBoots = true,
            fishingPole = false,
            asukaTrophy = true,
            sunTrophy = false,
        )

        val dto = ImportantItemsConverter.toDto(importantItems)

        assertTrue(dto.treeBoots.hasValue)
        assertTrue(dto.treeBoots.value)
        assertTrue(dto.fishingPole.hasValue)
        assertFalse(dto.fishingPole.value)
        assertTrue(dto.asukaTrophy.hasValue)
        assertTrue(dto.asukaTrophy.value)
        assertTrue(dto.sunTrophy.hasValue)
        assertFalse(dto.sunTrophy.value)
    }
}
