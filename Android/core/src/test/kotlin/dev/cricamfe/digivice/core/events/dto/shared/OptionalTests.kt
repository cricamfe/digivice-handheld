package dev.cricamfe.digivice.core.events.dto.shared

import dev.cricamfe.digivice.core.events.dto.PlayerDto
import dev.cricamfe.digivice.core.events.dto.extensions.isEmpty
import dev.cricamfe.digivice.core.events.dto.extensions.isNotEmpty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Port of `Tests/Unit/Events/DTO/Shared/OptionalTests.cs`. */
class OptionalTests {
    @Test
    fun `empty has no value`() {
        val optional: Optional<Int> = Optional.Empty

        assertFalse(optional.hasValue)
        assertNull(optional.valueOrNull())
        // The C# struct returned default(T) here; Kotlin has no default(T), so reading the value fails instead.
        assertThrows(NoSuchElementException::class.java) { optional.value }
    }

    @Test
    fun `a present optional holds its value`() {
        val optional: Optional<String> = Optional.Present("Agumon")

        assertTrue(optional.hasValue)
        assertEquals("Agumon", optional.value)
    }

    @Test
    fun `an explicit null is kept as a value`() {
        val optional: Optional<String?> = Optional.Present(null)

        assertTrue(optional.hasValue)
        assertNull(optional.value)
    }

    @Test
    fun `two empties are equal`() {
        assertEquals(Optional.Empty, Optional.Empty)
        assertTrue(Optional.Empty == Optional.Empty)
    }

    @Test
    fun `two optionals with the same value are equal`() {
        val left: Optional<Int> = Optional.Present(200)
        val right: Optional<Int> = Optional.Present(200)

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
    }

    @Test
    fun `empty is not equal to a present default`() {
        val presentZero: Optional<Int> = Optional.Present(0)

        assertNotEquals(Optional.Empty, presentZero)
        assertNotEquals(presentZero, Optional.Empty)
    }

    @Test
    fun `empty is not equal to a present null`() {
        val presentNull: Optional<String?> = Optional.Present(null)

        assertNotEquals(Optional.Empty, presentNull)
        assertNotEquals(presentNull, Optional.Empty)
    }

    @Test
    fun `a dto is empty only while none of its optionals is present`() {
        assertTrue(PlayerDto().isEmpty(PlayerDto()))
        assertTrue(PlayerDto(bits = Optional.Present(200)).isNotEmpty(PlayerDto()))
    }
}
