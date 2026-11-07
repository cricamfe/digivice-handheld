package dev.cricamfe.digivice.core.events

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

/** The golden comparison is only worth trusting if it is strict where the wire format is. */
class JsonAssertionsTests {
    private fun failureOf(expected: String, actual: String): String =
        assertThrows(AssertionFailedError::class.java) { assertJsonEquals(expected, actual) }.message!!

    @Test
    fun `key order does not matter`() {
        assertJsonEquals("""{"a":1,"b":[1,2]}""", """{ "b": [1, 2], "a": 1 }""")
    }

    @Test
    fun `null is not the same as an absent key`() {
        assertTrue(failureOf("""{"a":null}""", "{}").startsWith("$.a:"))
        assertTrue(failureOf("{}", """{"a":null}""").contains("unexpected keys [a]"))
    }

    @Test
    fun `a number is not a string`() {
        assertTrue(failureOf("""{"a":1}""", """{"a":"1"}""").startsWith("$.a:"))
    }

    @Test
    fun `the failure names the path of the first difference`() {
        val expected = """{"party":{"slots":[{"hp":{"max":1}},{"hp":{"max":2}}]}}"""
        val actual = """{"party":{"slots":[{"hp":{"max":1}},{"hp":{"max":3}}]}}"""

        assertTrue(failureOf(expected, actual).startsWith("$.party.slots[1].hp.max:"))
    }

    @Test
    fun `array length differences are reported`() {
        assertTrue(failureOf("""{"a":[1,2]}""", """{"a":[1]}""").contains("expected 2 items but was 1"))
    }
}
