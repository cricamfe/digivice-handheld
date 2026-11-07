package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.MemoryReadException
import dev.cricamfe.digivice.core.memory.readers.PlayerReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlayerLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate player addresses and reader`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            int32[0x00048DA0L] = 15000
            int16[0x0004B3F8L] = 4
            int16[0x0004B400L] = 0x023E
            bytes[0x00048D78L] = 0x08u
            bytes[0x00048D7AL] = 0x01u
            // The player's actor, at the third entry of the task registry.
            int32[0x0004B618L] = 0x800ACD14.toInt()
            int32[0x000ACD14L] = 5
            int32[0x000ACD18L] = 2
            int32[0x000ACD1CL] = 0
            int32[0x000ACD6CL] = 832
            int32[0x000ACD70L] = 640
        }

        val playerLoader = PlayerLoader(addressesRepository, PlayerReader(memoryReader))

        val playerResource = playerLoader.load()

        assertEquals(15000, playerResource.bits)
        assertEquals(4.toShort(), playerResource.mapId)
        assertEquals(0x023E.toShort(), playerResource.previousMapId)
        assertEquals(0x08.toUByte(), playerResource.seabedRoute)
        assertEquals(0x01.toUByte(), playerResource.mapVariant)
        assertEquals(832, playerResource.positionX)
        assertEquals(640, playerResource.positionY)
    }

    @Test
    fun `load should throw memory read exception when memory reader cannot read player data`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            int32Failures[0x00048DA0L] = MemoryReadException(0x00048DA0, "Memory session is not connected.")
        }

        val playerLoader = PlayerLoader(addressesRepository, PlayerReader(memoryReader))

        assertThrows(MemoryReadException::class.java) { playerLoader.load() }
    }
}
