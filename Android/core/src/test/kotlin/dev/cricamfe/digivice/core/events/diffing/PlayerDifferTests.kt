package dev.cricamfe.digivice.core.events.diffing

import dev.cricamfe.digivice.core.domain.models.Player
import dev.cricamfe.digivice.core.events.assertAbsent
import dev.cricamfe.digivice.core.events.assertPresent
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/Diffing/PlayerDifferTests.cs`. */
class PlayerDifferTests {
    @Test
    fun `diff returns an empty dto when there are no changes`() {
        val previous = Player(bits = 100, mapId = "0001", previousMapId = "023E", seabedRoute = 0x08u, mapVariant = 0x01u)
        val new = Player(bits = 100, mapId = "0001", previousMapId = "023E", seabedRoute = 0x08u, mapVariant = 0x01u)

        val result = PlayerDiffer.diff(previous, new)

        assertAbsent(result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.previousMapId)
        assertAbsent(result.seabedRoute)
        assertAbsent(result.mapVariant)
        assertAbsent(result.positionX)
        assertAbsent(result.positionY)
    }

    @Test
    fun `diff returns the full dto when the previous player is null`() {
        val new = Player(bits = 100, mapId = "0001", previousMapId = "023E", seabedRoute = 0x08u, mapVariant = 0x01u)

        val result = PlayerDiffer.diff(null, new)

        assertPresent(100, result.bits)
        assertPresent("0001", result.mapId)
        assertPresent("023E", result.previousMapId)
        assertPresent(0x08.toUByte(), result.seabedRoute)
        assertPresent(0x01.toUByte(), result.mapVariant)
        assertPresent(0, result.positionX)
        assertPresent(0, result.positionY)
    }

    @Test
    fun `diff returns the bits delta when only bits changed`() {
        val previous = Player(bits = 100, mapId = "0001", previousMapId = "023E")
        val new = Player(bits = 200, mapId = "0001", previousMapId = "023E")

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(200, result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.previousMapId)
        assertAbsent(result.seabedRoute)
        assertAbsent(result.mapVariant)
    }

    @Test
    fun `diff returns the map id delta when only the map id changed`() {
        val previous = Player(bits = 100, mapId = "0001", previousMapId = "023E")
        val new = Player(bits = 100, mapId = "0002", previousMapId = "023E")

        val result = PlayerDiffer.diff(previous, new)

        assertAbsent(result.bits)
        assertPresent("0002", result.mapId)
        assertAbsent(result.previousMapId)
        assertAbsent(result.seabedRoute)
        assertAbsent(result.mapVariant)
    }

    @Test
    fun `diff returns the previous map id delta when only the previous map id changed`() {
        val previous = Player(bits = 100, mapId = "02E2", previousMapId = "023E")
        val new = Player(bits = 100, mapId = "02E2", previousMapId = "02E0")

        val result = PlayerDiffer.diff(previous, new)

        assertPresent("02E0", result.previousMapId)
        assertAbsent(result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.seabedRoute)
        assertAbsent(result.mapVariant)
    }

    @Test
    fun `diff returns multiple deltas when bits and map id changed`() {
        val previous = Player(bits = 100, mapId = "0001")
        val new = Player(bits = 200, mapId = "0002")

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(200, result.bits)
        assertPresent("0002", result.mapId)
    }

    @Test
    fun `diff returns the seabed route delta when only the seabed route changed`() {
        val previous = Player(bits = 100, mapId = "0001", seabedRoute = null, mapVariant = null)
        val new = Player(bits = 100, mapId = "0001", seabedRoute = 0x08u, mapVariant = null)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(0x08.toUByte(), result.seabedRoute)
        assertAbsent(result.mapVariant)
        assertAbsent(result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.previousMapId)
    }

    @Test
    fun `diff returns the map variant delta when only the map variant changed`() {
        val previous = Player(bits = 100, mapId = "0001", seabedRoute = 0x08u, mapVariant = null)
        val new = Player(bits = 100, mapId = "0001", seabedRoute = 0x08u, mapVariant = 0x01u)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(0x01.toUByte(), result.mapVariant)
        assertAbsent(result.seabedRoute)
        assertAbsent(result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.previousMapId)
    }

    @Test
    fun `diff returns both seabed deltas when the seabed route and the map variant changed`() {
        val previous = Player(bits = 100, mapId = "023E", seabedRoute = null, mapVariant = null)
        val new = Player(bits = 100, mapId = "02E2", seabedRoute = 0x08u, mapVariant = 0x01u)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent("02E2", result.mapId)
        assertPresent(0x08.toUByte(), result.seabedRoute)
        assertPresent(0x01.toUByte(), result.mapVariant)
    }

    @Test
    fun `diff returns explicit null deltas when the seabed route and the map variant become null`() {
        val previous = Player(bits = 100, mapId = "02E2", seabedRoute = 0x08u, mapVariant = 0x01u)
        val new = Player(bits = 100, mapId = "02E2", seabedRoute = null, mapVariant = null)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(null, result.seabedRoute)
        assertPresent(null, result.mapVariant)
        assertAbsent(result.bits)
        assertAbsent(result.mapId)
    }

    @Test
    fun `diff returns only the position delta when the player walked`() {
        val previous = Player(bits = 100, mapId = "0001", positionX = 832, positionY = 640)
        val new = Player(bits = 100, mapId = "0001", positionX = 848, positionY = 640)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(848, result.positionX)
        assertAbsent(result.positionY)
        assertAbsent(result.bits)
        assertAbsent(result.mapId)
        assertAbsent(result.previousMapId)
    }

    @Test
    fun `diff returns both position deltas when the player moved on both axes`() {
        val previous = Player(bits = 100, mapId = "0001", positionX = 832, positionY = 640)
        val new = Player(bits = 100, mapId = "0001", positionX = 0, positionY = 655)

        val result = PlayerDiffer.diff(previous, new)

        assertPresent(0, result.positionX)
        assertPresent(655, result.positionY)
        assertAbsent(result.mapId)
    }
}
