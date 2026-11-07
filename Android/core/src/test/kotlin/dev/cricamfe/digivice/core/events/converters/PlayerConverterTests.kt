package dev.cricamfe.digivice.core.events.converters

import dev.cricamfe.digivice.core.domain.models.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Converters/PlayerConverterTests.cs`. */
class PlayerConverterTests {
    @Test
    fun `toDto maps all player fields`() {
        val player = Player(
            bits = 12345,
            mapId = "00AF",
            previousMapId = "023E",
            seabedRoute = 0x08u,
            mapVariant = 0x01u,
            positionX = 832,
            positionY = 640,
        )

        val dto = PlayerConverter.toDto(player)

        assertTrue(dto.bits.hasValue)
        assertEquals(12345, dto.bits.value)
        assertTrue(dto.mapId.hasValue)
        assertEquals("00AF", dto.mapId.value)
        assertTrue(dto.previousMapId.hasValue)
        assertEquals("023E", dto.previousMapId.value)
        assertTrue(dto.seabedRoute.hasValue)
        assertEquals(0x08.toUByte(), dto.seabedRoute.value)
        assertTrue(dto.mapVariant.hasValue)
        assertEquals(0x01.toUByte(), dto.mapVariant.value)
        assertTrue(dto.positionX.hasValue)
        assertEquals(832, dto.positionX.value)
        assertTrue(dto.positionY.hasValue)
        assertEquals(640, dto.positionY.value)
    }

    @Test
    fun `toDto sends the position even when it is zero`() {
        val dto = PlayerConverter.toDto(Player(bits = 12345, mapId = "00AF"))

        assertTrue(dto.positionX.hasValue)
        assertEquals(0, dto.positionX.value)
        assertTrue(dto.positionY.hasValue)
        assertEquals(0, dto.positionY.value)
    }

    @Test
    fun `toDto sends an explicit null when seabed route and map variant are null`() {
        val player = Player(bits = 12345, mapId = "00AF", previousMapId = "023E", seabedRoute = null, mapVariant = null)

        val dto = PlayerConverter.toDto(player)

        assertTrue(dto.seabedRoute.hasValue)
        assertNull(dto.seabedRoute.value)
        assertTrue(dto.mapVariant.hasValue)
        assertNull(dto.mapVariant.value)
    }
}
