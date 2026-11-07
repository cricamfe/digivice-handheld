package dev.cricamfe.digivice.extractor.image

import dev.cricamfe.digivice.extractor.disc.invalidDisc

object Pack {
    private fun offsets(bytes: ByteArray, first: Int): List<Int> {
        if (first == 0) return emptyList()
        if (first < 4 || first % 4 != 0 || first > bytes.size || first / 4 > 16384) invalidDisc("Invalid archive offsets")
        return (0 until first step 4).map { bytes.intAt(it) }
    }

    /** An explicitly addressed entry may follow a zero slot in a stage archive. */
    internal fun entry(bytes: ByteArray, index: Int): ByteArray? {
        if (index !in 0 until minOf(16384, bytes.size / 4)) return null
        // A stage pack may also start with holes. Only inspect slots up to the one requested.
        val first = (0..index).asSequence().map { bytes.intAt(it * 4) }.firstOrNull { it != 0 } ?: return null
        val offsets = offsets(bytes, first)
        val start = offsets.getOrNull(index)?.takeIf { it != 0 } ?: return null
        val end = offsets.asSequence().drop(index + 1).firstOrNull { it != 0 } ?: bytes.size
        if (start < offsets.size * 4 || end < start || end > bytes.size) invalidDisc("Invalid archive entry size")
        return bytes.copyOfRange(start, end)
    }

    fun entries(bytes: ByteArray): List<ByteArray> {
        val offsets = offsets(bytes, bytes.intAt(0))
        val first = offsets.firstOrNull() ?: return emptyList()
        val active = offsets.takeWhile { it != 0 }
        if (active.any { it < first || it > bytes.size } || active.zipWithNext().any { (a, b) -> b < a }) invalidDisc("Archive entry outside the file")
        val tailEnd = offsets.asSequence().drop(active.size).firstOrNull { it != 0 } ?: bytes.size
        return active.mapIndexed { index, start ->
            val end = active.getOrNull(index + 1) ?: tailEnd
            if (end < start || end > bytes.size) invalidDisc("Invalid archive entry size")
            bytes.copyOfRange(start, end)
        }
    }
}
