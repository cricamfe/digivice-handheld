package dev.cricamfe.digivice.extractor.image

import dev.cricamfe.digivice.extractor.disc.invalidDisc

object Rlen {
    fun decode(bytes: ByteArray): ByteArray {
        if (bytes.size < 8 || !bytes.copyOfRange(0, 4).contentEquals("RLEN".toByteArray())) invalidDisc("Invalid RLEN signature")
        val size = bytes.intAt(4)
        if (size > MAX_DATA) invalidDisc("Oversized RLEN output")
        val out = ByteArray(size)
        var input = 8
        var output = 0
        while (output < size) {
            bytes.range(input, 1)
            val op = bytes[input++].toInt() and 255
            val count = if (op < 128) op else op - 128
            if (count > size - output) invalidDisc("RLEN output exceeds its size")
            if (op < 128) {
                bytes.range(input, count)
                bytes.copyInto(out, output, input, input + count)
                input += count
            } else {
                bytes.range(input, 1)
                out.fill(bytes[input++], output, output + count)
            }
            output += count
        }
        return out
    }

    internal fun unpack(bytes: ByteArray): ByteArray =
        if (bytes.size >= 4 && bytes[0] == 82.toByte() && bytes[1] == 76.toByte() && bytes[2] == 69.toByte() && bytes[3] == 78.toByte()) decode(bytes) else bytes
}
