package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.ImportantItemsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ImportantItemsLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate important items addresses and reader`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bytes[0x00048DB4L] = 0x01u
            bytes[0x00048DB5L] = 0x00u
            bytes[0x00048DC2L] = 0x01u
            bytes[0x00048DC4L] = 0x00u
        }

        val importantItemsLoader = ImportantItemsLoader(addressesRepository, ImportantItemsReader(memoryReader))

        val resource = importantItemsLoader.load()

        assertEquals(0x01.toUByte(), resource.treeBoots)
        assertEquals(0x00.toUByte(), resource.fishingPole)
        assertEquals(0x01.toUByte(), resource.asukaTrophy)
        assertEquals(0x00.toUByte(), resource.sunTrophy)
    }

    @Test
    fun `load should throw memory read exception when memory reader cannot read`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            byteFailures[0x00048DB4L] = MemoryReadException(0x00048DB4, "Memory session is not connected.")
        }

        val importantItemsLoader = ImportantItemsLoader(addressesRepository, ImportantItemsReader(memoryReader))

        assertThrows(MemoryReadException::class.java) { importantItemsLoader.load() }
    }
}
