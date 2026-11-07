package dev.cricamfe.digivice.core.events.factory

import dev.cricamfe.digivice.core.domain.models.ImportantItems
import dev.cricamfe.digivice.core.domain.models.State
import dev.cricamfe.digivice.core.events.assertPresent
import dev.cricamfe.digivice.core.events.dto.ImportantItemsDto
import dev.cricamfe.digivice.core.events.models.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Factory/ImportantItemsEventFactoryTests.cs`. */
class ImportantItemsEventFactoryTests {
    private fun state(importantItems: ImportantItems) = State(importantItems = importantItems)

    @Test
    fun `create returns no events when the important items have no changes`() {
        val previous = state(ImportantItems(treeBoots = true))
        val new = state(ImportantItems(treeBoots = true))

        assertTrue(ImportantItemsEventFactory.create(previous, new).isEmpty())
    }

    @Test
    fun `create returns an important items changed event when the important items change`() {
        val previous = state(ImportantItems(treeBoots = false))
        val new = state(ImportantItems(treeBoots = true))

        val event = ImportantItemsEventFactory.create(previous, new).single()

        assertEquals(EventType.ImportantItemsChanged, event.type)
        assertPresent(true, assertInstanceOf(ImportantItemsDto::class.java, event.payload).treeBoots)
    }
}
