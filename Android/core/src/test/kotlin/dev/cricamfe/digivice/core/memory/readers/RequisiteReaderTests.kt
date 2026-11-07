package dev.cricamfe.digivice.core.memory.readers

import dev.cricamfe.digivice.core.memory.FakeMemoryReader
import dev.cricamfe.digivice.core.memory.addresses.journals.quests.RequisiteAddresses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RequisiteReaderTests {
    @Test
    fun `read should map requisite resource correctly`() {
        val addresses = RequisiteAddresses(id = "ReqX", address = 0x7500)
        val memoryReader = FakeMemoryReader().apply { bytes[0x7500] = 7u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals("ReqX", result.id)
        assertEquals(7.toUByte(), result.value)
    }

    @Test
    fun `read should return raw byte when bit masks is empty`() {
        val addresses = RequisiteAddresses(id = "folderBag", address = 0x48F42, bitMasks = emptyList())
        val memoryReader = FakeMemoryReader().apply { bytes[0x48F42] = 1u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals(1.toUByte(), result.value)
    }

    @Test
    fun `read should apply bit mask when single bit mask is provided`() {
        val addresses = RequisiteAddresses(id = "byakkoLeader", address = 0x4B3E5, bitMasks = listOf(0x40))
        val memoryReader = FakeMemoryReader().apply { maskedBytes[0x4B3E5L to 0x40L] = 0x40u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals(0x40.toUByte(), result.value)
    }

    @Test
    fun `read should return zero when not all bit masks are set`() {
        val addresses = RequisiteAddresses(id = "ReqMulti", address = 0x4B370, bitMasks = listOf(0x01, 0x08))
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x01u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals(0.toUByte(), result.value)
    }

    @Test
    fun `read should return first bit mask value when all bit masks are set`() {
        val addresses = RequisiteAddresses(id = "ReqMulti", address = 0x4B370, bitMasks = listOf(0x01, 0x08))
        val memoryReader = FakeMemoryReader().apply { bytes[0x4B370] = 0x09u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals(0x01.toUByte(), result.value)
    }

    @Test
    fun `read should treat the raw byte as unsigned when testing masks`() {
        val addresses = RequisiteAddresses(id = "high", address = 0x10, bitMasks = listOf(0x80, 0x01))
        val memoryReader = FakeMemoryReader().apply { bytes[0x10] = 0x81u }

        val result = RequisiteReader(memoryReader).read(addresses)

        assertEquals(0x80.toUByte(), result.value)
    }
}
