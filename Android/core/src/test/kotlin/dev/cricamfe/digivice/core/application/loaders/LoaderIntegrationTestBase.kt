package dev.cricamfe.digivice.core.application.loaders

import dev.cricamfe.digivice.core.memory.TestEnvironment
import dev.cricamfe.digivice.core.memory.repositories.AddressesRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Port of `LoaderIntegrationTestBase`: the loaders run against the real definitions in `Backend/Memory/Definitions`.
 * The bit-mask bridge lives on `FakeMemoryReader.bridgeMaskedBytes()`.
 */
abstract class LoaderIntegrationTestBase {
    protected fun createAddressesRepository(): AddressesRepository = TestEnvironment.createAddressesRepository()

    protected fun writeInt16(block: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putShort(offset, value.toShort())
    }

    protected fun writeInt32(block: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
    }
}
