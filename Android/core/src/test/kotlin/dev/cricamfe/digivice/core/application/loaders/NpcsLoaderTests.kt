package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.readers.NpcsReader
import dev.cricamfe.digivice.core.memory.resources.NpcResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpcsLoaderTests : LoaderIntegrationTestBase() {
    @Test
    fun `load should integrate npcs addresses and reader`() {
        val addressesRepository = createAddressesRepository()
        val memoryReader = FakeMemoryReader().apply {
            bridgeMaskedBytes()
            bytes[0x0004B3DFL] = 0x20u
            bytes[0x0004B39AL] = 0xFFu
            bytes[0x0004B39BL] = 0xFFu
            bytes[0x0004B39CL] = 0x01u
            bytes[0x00048DB9L] = 0x01u
            bytes[0x00048DBAL] = 0x01u
            bytes[0x00048DBBL] = 0x01u
            bytes[0x0004B3E0L] = 0x40u
            bytes[0x0004B3E1L] = 0x40u
            bytes[0x0004B3E5L] = 0x08u
            bytes[0x0004B392L] = 0xFFu
            bytes[0x0004B393L] = 0xFFu
            bytes[0x0004B394L] = 0xFFu
            bytes[0x0004B395L] = 0xFFu
        }

        val npcsLoader = NpcsLoader(addressesRepository, NpcsReader(memoryReader))

        val resource = npcsLoader.load()

        fun NpcResource.single() = battles.single().value.toInt()

        assertEquals(0x20, resource.genji.battles.single { it.id == "first" }.value.toInt())
        assertEquals(0x01, resource.genji.battles.single { it.id == "second" }.value.toInt())
        assertEquals(0x02, resource.natsumi.single())
        assertEquals(0x08, resource.catherine.single())
        assertEquals(0x20, resource.robert.single())
        assertEquals(0x02, resource.chris.single())
        assertEquals(0x01, resource.tomomi.single())
        assertEquals(0x04, resource.mitch.single())
        assertEquals(0x80, resource.bob.single())
        assertEquals(0x04, resource.andy.single())
        assertEquals(0x08, resource.george.single())
        assertEquals(0x10, resource.meiLin.single())
        assertEquals(0x20, resource.jessica.single())
        assertEquals(0x40, resource.gordon.single())
        assertEquals(0x80, resource.alice.single())
        assertEquals(0x01, resource.nakano.single())
        assertEquals(0x01, resource.seiryuLeader.single())
        assertEquals(0x40, resource.keith.single())
        assertEquals(0x01, resource.suzakuLeader.single())
        assertEquals(0x40, resource.fakeByakkoLeader.single())
        assertEquals(0x01, resource.byakkoLeader.single())
        assertEquals(0x08, resource.aoaAttacker.single())
        assertEquals(0x01, resource.optionalTrooper1AdminCenterB1F.single())
        assertEquals(0x40, resource.optionalTrooper2AdminCenterB1F.single())
        assertEquals(0x40, resource.optionalTrooperAdminFloor1.single())
        assertEquals(0x10, resource.optionalTrooper1AdminFloor2.single())
        assertEquals(0x01, resource.optionalSoldier1AdminFloor2.single())
        assertEquals(0x20, resource.optionalTrooper2AdminFloor2.single())
    }
}
