package dev.cricamfe.digivice.core.memory

import dev.cricamfe.digivice.core.memory.addresses.EnemyAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.InBattleAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.SlotAddresses
import dev.cricamfe.digivice.core.memory.addresses.parties.digimons.DigievolutionsAddresses
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigievolutionSlotReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IDigimonSlotReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IEnemyReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IMemoryReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IInBattleReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IRequisiteReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStepReader
import dev.cricamfe.digivice.core.memory.readers.interfaces.IStoredDigievolutionReader
import dev.cricamfe.digivice.core.memory.resources.battles.EnemyResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import dev.cricamfe.digivice.core.memory.resources.journals.quests.StepResource
import dev.cricamfe.digivice.core.memory.resources.parties.DigimonSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.InBattleResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.DigievolutionSlotResource
import dev.cricamfe.digivice.core.memory.resources.parties.digimons.StoredDigievolutionResource

/**
 * Hand-written stand-in for the Moq `Mock<IMemoryReader>` of the C# tests. Like a loose Moq mock, anything that was
 * not set up answers with zero / an empty array.
 */
class FakeMemoryReader : IMemoryReader {
    val int32 = mutableMapOf<Long, Int>()
    val int16 = mutableMapOf<Long, Short>()
    val bytes = mutableMapOf<Long, UByte>()
    val maskedBytes = mutableMapOf<Pair<Long, Long>, UByte>()
    val blocks = mutableMapOf<Pair<Long, Int>, ByteArray>()

    /** Computes a masked read for any (address, mask) when there is no explicit entry in [maskedBytes]. */
    var maskedByte: ((address: Long, bitMask: Long) -> UByte)? = null

    /** When set, every `readByte` call throws it. */
    var byteFailure: Throwable? = null

    val maskedByteCalls = mutableListOf<Pair<Long, Long>>()

    /** Per-address failures, like `Setup(...).Throws(...)` on one address of a Moq mock. */
    val byteFailures = mutableMapOf<Long, Throwable>()
    val int32Failures = mutableMapOf<Long, Throwable>()
    val int16Failures = mutableMapOf<Long, Throwable>()
    val blockFailures = mutableMapOf<Pair<Long, Int>, Throwable>()

    /** Every `readBytes` call, in order, like `Verify(m => m.ReadBytes(...), Times.X)`. */
    val readBytesCalls = mutableListOf<Pair<Long, Int>>()

    /**
     * Port of `LoaderIntegrationTestBase.SetupReadByteBitMaskBridge`: a masked read answers the plain byte at that
     * address ANDed with the mask, truncated to a byte. Whatever is set in [bytes] (before or after) is honoured.
     */
    fun bridgeMaskedBytes() {
        maskedByte = { address, bitMask -> (readByte(address).toLong() and bitMask).toUByte() }
    }

    /** Models a shared RAM byte: `readByte(address, mask)` returns `(rawValue & mask)` truncated to a byte. */
    fun setMaskedByte(address: Long, rawValue: Int) {
        val previous = maskedByte
        maskedByte = { a, mask ->
            if (a == address) (rawValue.toLong() and mask).toUByte() else previous?.invoke(a, mask) ?: 0u
        }
    }

    override fun readBytes(address: Long, length: Int): ByteArray {
        readBytesCalls += address to length
        blockFailures[address to length]?.let { throw it }
        return blocks[address to length] ?: ByteArray(0)
    }

    override fun readByte(address: Long): UByte {
        byteFailure?.let { throw it }
        byteFailures[address]?.let { throw it }
        return bytes[address] ?: 0u
    }

    override fun readByte(address: Long, bitMask: Long): UByte {
        byteFailure?.let { throw it }
        maskedByteCalls += address to bitMask
        return maskedBytes[address to bitMask] ?: maskedByte?.invoke(address, bitMask) ?: 0u
    }

    override fun readInt32(address: Long): Int {
        int32Failures[address]?.let { throw it }
        return int32[address] ?: 0
    }

    override fun readInt16(address: Long): Short {
        int16Failures[address]?.let { throw it }
        return int16[address] ?: 0
    }
}

/** Hand-written stand-in for a `Mock<IMemoryAccessor>`: each operation is a replaceable lambda. */
class FakeAccessor : MemoryAccessor {
    var onReadInt32: (Long) -> Int = { 0 }
    var onReadInt16: (Long) -> Short = { 0 }
    var onReadBytes: (Long, Int) -> ByteArray = { _, count -> ByteArray(count) }

    override fun readInt32(address: Long): Int = onReadInt32(address)
    override fun readInt16(address: Long): Short = onReadInt16(address)
    override fun readBytes(address: Long, count: Int): ByteArray = onReadBytes(address, count)
}

class FakeEnemyReader(private val result: EnemyResource) : IEnemyReader {
    val calls = mutableListOf<EnemyAddresses>()

    override fun read(addresses: EnemyAddresses): EnemyResource {
        calls += addresses
        return result
    }
}

class FakeRequisiteReader(private val results: Map<RequisiteAddresses, RequisiteResource>) : IRequisiteReader {
    override fun read(addresses: RequisiteAddresses): RequisiteResource = results.getValue(addresses)
}

class FakeStepReader(private val results: Map<StepAddresses, StepResource>) : IStepReader {
    override fun read(addresses: StepAddresses): StepResource = results.getValue(addresses)
}

class FakeDigimonSlotReader(private val results: Map<SlotAddresses, DigimonSlotResource>) : IDigimonSlotReader {
    override fun read(addresses: SlotAddresses): DigimonSlotResource = results.getValue(addresses)
}

class FakeDigievolutionSlotReader(
    private val results: Map<SlotAddresses, DigievolutionSlotResource> = emptyMap(),
) : IDigievolutionSlotReader {
    override fun read(memoryBlockReader: MemoryBlockReader, slotAddresses: SlotAddresses): DigievolutionSlotResource =
        results.getValue(slotAddresses)
}

class FakeStoredDigievolutionReader(
    private val result: List<StoredDigievolutionResource> = emptyList(),
) : IStoredDigievolutionReader {
    override fun read(
        memoryBlockReader: MemoryBlockReader,
        digievolutionsAddresses: DigievolutionsAddresses,
    ): List<StoredDigievolutionResource> = result
}

class FakeInBattleReader(private val result: InBattleResource = InBattleResource()) : IInBattleReader {
    val calls = mutableListOf<Pair<InBattleAddresses, Int>>()

    override fun read(addresses: InBattleAddresses, partySlotIndex: Int): InBattleResource {
        calls += addresses to partySlotIndex
        return result
    }
}
