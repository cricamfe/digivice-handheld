package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.FakeRequisiteReader
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAddresses
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.StepAlternative
import dev.cricamfe.digivice.core.memory.resources.journals.quests.RequisiteResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StepReaderTests {
    @Test
    fun `read should map step resource correctly`() {
        val reqAddresses = RequisiteAddresses(id = "Req2", address = 0x6000)
        val addresses = StepAddresses(
            number = 3,
            address = 0x5500,
            bitMasks = listOf(0x0F),
            requisites = listOf(reqAddresses),
        )
        val expectedRequisiteResource = RequisiteResource(id = "Req2", value = 5u)
        val memoryReader = FakeMemoryReader().apply { maskedBytes[0x5500L to 0x0FL] = 2u }
        val requisiteReader = FakeRequisiteReader(mapOf(reqAddresses to expectedRequisiteResource))

        val result = StepReader(memoryReader, requisiteReader).read(addresses)

        assertEquals(3, result.number)
        assertEquals(2.toUByte(), result.value)
        assertEquals(listOf(expectedRequisiteResource), result.requisites)
    }

    @Test
    fun `read should answer one when the byte has reached at least`() {
        val addresses = StepAddresses(number = 6, address = 0x4B370, atLeast = 3)
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x04u }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(1.toUByte(), result.value)
    }

    @Test
    fun `read should answer zero when the byte has not reached at least`() {
        val addresses = StepAddresses(number = 6, address = 0x4B370, atLeast = 3)
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x02u }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(0.toUByte(), result.value)
    }

    @Test
    fun `read should answer an alternative when the step's own mark is not set`() {
        val addresses = StepAddresses(
            number = 1,
            address = 0x4B38E,
            bitMasks = listOf(0x04),
            alternatives = listOf(StepAlternative(address = 0x4B387, bitMasks = listOf(0x08))),
        )
        val memoryReader = FakeMemoryReader().apply {
            maskedBytes[0x4B38EL to 0x04L] = 0u
            maskedBytes[0x4B387L to 0x08L] = 0x08u
        }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(0x08.toUByte(), result.value)
    }

    @Test
    fun `read should answer zero when neither the mark nor the alternatives are set`() {
        val addresses = StepAddresses(
            number = 1,
            address = 0x4B38E,
            bitMasks = listOf(0x04),
            alternatives = listOf(StepAlternative(address = 0x4B387, bitMasks = listOf(0x08))),
        )
        val memoryReader = FakeMemoryReader().apply {
            maskedBytes[0x4B38EL to 0x04L] = 0u
            maskedBytes[0x4B387L to 0x08L] = 0u
        }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(0.toUByte(), result.value)
    }

    @Test
    fun `read should return zero when not all bit masks are set`() {
        val addresses = StepAddresses(number = 21, address = 0x4B370, bitMasks = listOf(0x01, 0x08))
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x01u }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(0.toUByte(), result.value)
    }

    @Test
    fun `read should return first bit mask value when all bit masks are set`() {
        val addresses = StepAddresses(number = 21, address = 0x4B370, bitMasks = listOf(0x01, 0x08))
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x09u }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(0x01.toUByte(), result.value)
    }

    @Test
    fun `read should return raw byte when bit masks is empty`() {
        val addresses = StepAddresses(number = 1, address = 0x48F42, bitMasks = emptyList())
        val memoryReader = FakeMemoryReader().apply { bytes[0x48F42] = 1u }

        val result = StepReader(memoryReader, FakeRequisiteReader(emptyMap())).read(addresses)

        assertEquals(1.toUByte(), result.value)
    }
}
