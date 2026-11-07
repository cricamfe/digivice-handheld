package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.cards.loadUiTexture
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpriteResourceTests {
    @Test fun `field sprite slots keep holes and use the next nonzero offset`() {
        val bytes = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0).putInt(16).putInt(0).putInt(20).putInt(123).putInt(456).array()
        assertArrayEquals(byteArrayOf(123, 0, 0, 0), spriteEntry(bytes, 1))
        assertArrayEquals(byteArrayOf(0xc8.toByte(), 1, 0, 0), spriteEntry(bytes, 3))
        assertThrows(IOException::class.java) { spriteEntry(bytes, 0) }
        assertThrows(IOException::class.java) { spriteEntry(bytes, 2) }
        assertThrows(IOException::class.java) { spriteEntry(bytes, -1) }
        assertThrows(IOException::class.java) { spriteEntry(bytes, 4) }
    }

    @Test fun `invalid field sprite offsets cannot escape the resource`() {
        for (offsets in listOf(listOf(8, 7), listOf(8, 99), listOf(5, 8), listOf(0, 0))) {
            val bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            offsets.forEach { bytes.putInt(it) }
            assertThrows(IOException::class.java) { spriteEntry(bytes.array(), 1) }
        }
    }

    @Test fun `menu textures cannot be empty or exceed VRAM`() {
        assertThrows(IOException::class.java) { loadUiTexture(ByteArray(4), 0x140) }
        val tim = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x10).putInt(2).putInt(14).putShort(0).putShort(0).putShort(1).putShort(1).putShort(31).array()
        val archive = ByteBuffer.allocate(tim.size + 4).order(ByteOrder.LITTLE_ENDIAN).putInt(4).put(tim).array()
        assertThrows(IOException::class.java) { loadUiTexture(archive, 1024) }
        assertEquals(31, loadUiTexture(archive, 0x140)[0x140])
    }

    @Test fun `character resource has only factual references and asset identifiers`() {
        val data = CharacterTables.data
        assertEquals(309, data.actors.size)
        assertEquals(24, data.items.size)
        assertTrue(data.actors.values.all { it.size == 2 && it[0] in 0 until 2382 && it[1] in 0..16383 })
        assertTrue(data.items.all { it in 0..255 })
    }
}
