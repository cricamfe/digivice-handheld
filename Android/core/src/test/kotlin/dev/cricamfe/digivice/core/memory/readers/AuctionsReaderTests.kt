package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.AuctionAddresses
import dev.cricamfe.digivice.core.memory.addresses.AuctionsAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuctionsReaderTests {
    @Test
    fun `read should map all auction flags from shared byte`() {
        val addresses = createAddresses()
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B38A, 0x25) }

        val result = AuctionsReader(memoryReader).read(addresses)

        assertEquals(0x01.toUByte(), result.divineBarrier)
        assertEquals(0x00.toUByte(), result.hazardShield)
        assertEquals(0x04.toUByte(), result.sniperShield)
        assertEquals(0x00.toUByte(), result.dramonShield)
        assertEquals(0x00.toUByte(), result.yinYangWand)
        assertEquals(0x20.toUByte(), result.hazardFang)
    }

    @Test
    fun `read should return zero flags when memory byte is zero`() {
        val addresses = createAddresses()
        val memoryReader = FakeMemoryReader().apply { setMaskedByte(0x0004B38A, 0x00) }

        val result = AuctionsReader(memoryReader).read(addresses)

        assertEquals(0x00.toUByte(), result.divineBarrier)
        assertEquals(0x00.toUByte(), result.hazardShield)
        assertEquals(0x00.toUByte(), result.sniperShield)
        assertEquals(0x00.toUByte(), result.dramonShield)
        assertEquals(0x00.toUByte(), result.yinYangWand)
        assertEquals(0x00.toUByte(), result.hazardFang)
    }

    private fun createAddresses() = AuctionsAddresses(
        divineBarrier = AuctionAddresses(address = 0x0004B38A, bitMask = 0x01),
        hazardShield = AuctionAddresses(address = 0x0004B38A, bitMask = 0x02),
        sniperShield = AuctionAddresses(address = 0x0004B38A, bitMask = 0x04),
        dramonShield = AuctionAddresses(address = 0x0004B38A, bitMask = 0x08),
        yinYangWand = AuctionAddresses(address = 0x0004B38A, bitMask = 0x10),
        hazardFang = AuctionAddresses(address = 0x0004B38A, bitMask = 0x20),
    )
}
