package dev.cricamfe.digivice.core.domain.assemblers

import dev.cricamfe.digivice.core.memory.resources.PlayerResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PlayerAssemblerTests {
    @Test
    fun `assemble should map all fields correctly when resource is valid`() {
        val resource = PlayerResource(
            bits = 1500,
            mapId = 255,
            previousMapId = 0x023E,
            seabedRoute = 0x08u,
            mapVariant = 0x01u,
            positionX = 832,
            positionY = -640,
        )

        val result = PlayerAssembler.assemble(resource)

        assertEquals(1500, result.bits)
        assertEquals("00FF", result.mapId)
        assertEquals("023E", result.previousMapId)
        assertEquals(0x08.toUByte(), result.seabedRoute)
        assertEquals(0x01.toUByte(), result.mapVariant)
        assertEquals(832, result.positionX)
        assertEquals(-640, result.positionY)
    }

    @Test
    fun `assemble should normalize seabed route and map variant to null when fields are zero`() {
        val resource = PlayerResource(
            bits = 0,
            mapId = 0,
            previousMapId = 0,
            seabedRoute = 0u,
            mapVariant = 0u,
        )

        val result = PlayerAssembler.assemble(resource)

        assertEquals(0, result.bits)
        assertEquals("0000", result.mapId)
        assertEquals("0000", result.previousMapId)
        assertNull(result.seabedRoute)
        assertNull(result.mapVariant)
    }

    // Not in the C# suite: a C# short formats as its 16-bit pattern ("X4"), never with a sign or more than 4 digits.
    @Test
    fun `assemble should format negative map ids as their 16 bit pattern`() {
        val resource = PlayerResource(mapId = (-1).toShort(), previousMapId = Short.MIN_VALUE)

        val result = PlayerAssembler.assemble(resource)

        assertEquals("FFFF", result.mapId)
        assertEquals("8000", result.previousMapId)
    }
}
