package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.MAX_PIXELS
import dev.cricamfe.digivice.extractor.image.intAt
import dev.cricamfe.digivice.extractor.image.range
import dev.cricamfe.digivice.extractor.image.wordAt

class MapFile private constructor(val width: Int, val height: Int, val entries: List<ByteArray>) {
    companion object {
        fun parse(bytes: ByteArray): MapFile {
            val n = bytes.intAt(0)
            val w = bytes.intAt(4)
            val h = bytes.intAt(8)
            val step = bytes.intAt(12)
            if (n <= 0 || n > 4096 || w !in 1..32 || h !in 1..32 || w.toLong() * h * 128 * 128 > MAX_PIXELS || step <= 0) invalidDisc("Invalid map dimensions")
            val first = ((4 * n + 2047) / 2048) * 2048
            if (first > bytes.size) invalidDisc("Missing map entries")
            val count = (bytes.size - first) / step
            if (count != w * h || (bytes.size - first) % step != 0 || 16L + count * 2L > first) invalidDisc("Invalid map size table")
            val entries = (0 until count).map { index ->
                val length = bytes.wordAt(16 + index * 2)
                if (length > step) invalidDisc("Map entry exceeds its slot")
                val start = first + step * index
                bytes.range(start, length)
                bytes.copyOfRange(start, start + length)
            }
            return MapFile(w * 128, h * 128, entries)
        }
    }
}
