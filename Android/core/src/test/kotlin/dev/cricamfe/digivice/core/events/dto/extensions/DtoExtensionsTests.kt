package dev.cricamfe.digivice.core.events.dto.extensions

import dev.cricamfe.digivice.core.events.dto.battles.EnemyDto
import dev.cricamfe.digivice.core.events.dto.parties.digimons.VitalDto
import dev.cricamfe.digivice.core.events.dto.shared.Optional
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/DTO/Extensions/DTOExtensionsTests.cs`. */
class DtoExtensionsTests {
    @Test
    fun `isEmpty is true when no property is set`() {
        val dto = VitalDto()

        assertTrue(dto.isEmpty(VitalDto()))
        assertFalse(dto.isNotEmpty(VitalDto()))
    }

    @Test
    fun `isEmpty is false when a property is set to zero`() {
        val dto = VitalDto(current = Optional.Present(0))

        assertFalse(dto.isEmpty(VitalDto()))
        assertTrue(dto.isNotEmpty(VitalDto()))
    }

    @Test
    fun `isNotEmpty is true when an optional holds a nested dto`() {
        val dto = EnemyDto(hp = Optional.Present(VitalDto()))

        assertTrue(dto.isNotEmpty(EnemyDto()))
    }
}
