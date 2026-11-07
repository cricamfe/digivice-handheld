package dev.cricamfe.digivice.core.events

import dev.cricamfe.digivice.core.events.dto.shared.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/** The C# `Assert.False(x.HasValue)`: the field is left out of the diff. */
fun <T> assertAbsent(actual: Optional<T>) = assertFalse(actual.hasValue, "expected the field to be absent but it was $actual")

/** The C# `Assert.True(x.HasValue); Assert.Equal(expected, x.Value)`; [expected] may be `null` for a present `null`. */
fun <T> assertPresent(expected: T, actual: Optional<T>) {
    assertTrue(actual.hasValue, "expected the field to be present but it was absent")
    assertEquals(expected, actual.value)
}

/** The value of a field that must be present (and not `null`), to look into the nested DTO. */
fun <T : Any> presentValue(actual: Optional<T?>): T {
    assertTrue(actual.hasValue, "expected the field to be present but it was absent")
    return checkNotNull(actual.value) { "expected a present value but it was a present null" }
}
