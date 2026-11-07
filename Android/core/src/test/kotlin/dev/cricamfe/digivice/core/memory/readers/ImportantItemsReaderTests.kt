package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.ImportantItemsAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportantItemsReaderTests {
    @Test
    fun `read should map important items resource correctly`() {
        val addresses = ImportantItemsAddresses(
            treeBoots = 0x00048DB4,
            fishingPole = 0x00048DB5,
            asukaTrophy = 0x00048DC2,
            sunTrophy = 0x00048DC4,
        )
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DB4] = 0x01u
            bytes[0x00048DB5] = 0x00u
            bytes[0x00048DC2] = 0x02u
            bytes[0x00048DC4] = 0x01u
        }

        val result = ImportantItemsReader(memoryReader).read(addresses)

        assertEquals(0x01.toUByte(), result.treeBoots)
        assertEquals(0x00.toUByte(), result.fishingPole)
        assertEquals(0x02.toUByte(), result.asukaTrophy)
        assertEquals(0x01.toUByte(), result.sunTrophy)
    }
}
