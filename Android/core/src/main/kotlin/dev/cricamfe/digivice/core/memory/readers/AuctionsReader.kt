package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.addresses.AuctionsAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IAuctionsReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.resources.AuctionsResource

/** Port of `Backend/Memory/Readers/AuctionsReader.cs`. */
class AuctionsReader(private val memoryReader: IMemoryReader) : IAuctionsReader {
    override fun read(addresses: AuctionsAddresses): AuctionsResource {
        return AuctionsResource(
            divineBarrier = memoryReader.readByte(addresses.divineBarrier.address, addresses.divineBarrier.bitMask),
            hazardShield = memoryReader.readByte(addresses.hazardShield.address, addresses.hazardShield.bitMask),
            sniperShield = memoryReader.readByte(addresses.sniperShield.address, addresses.sniperShield.bitMask),
            dramonShield = memoryReader.readByte(addresses.dramonShield.address, addresses.dramonShield.bitMask),
            yinYangWand = memoryReader.readByte(addresses.yinYangWand.address, addresses.yinYangWand.bitMask),
            hazardFang = memoryReader.readByte(addresses.hazardFang.address, addresses.hazardFang.bitMask),
        )
    }
}
