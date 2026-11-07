package dev.cricamfe.digivice.core.memory.converters

/**
 * Number parsing shared by the converters, with the semantics of the C# `Convert.ToInt64/ToInt32(str, 16)` and
 * `long.Parse/int.Parse` pairs: a "0x" prefix selects hexadecimal (read as two's complement, so "0xFFFFFFFF" is -1
 * for an int), anything else is a signed decimal. Both throw [NumberFormatException] when the text is not a number.
 */
internal fun parseHexOrDecimalLong(text: String): Long =
    if (text.startsWith("0x", ignoreCase = true)) {
        java.lang.Long.parseUnsignedLong(text.substring(2), 16)
    } else {
        text.trim().toLong()
    }

internal fun parseHexOrDecimalInt(text: String): Int =
    if (text.startsWith("0x", ignoreCase = true)) {
        Integer.parseUnsignedInt(text.substring(2), 16)
    } else {
        text.trim().toInt()
    }
