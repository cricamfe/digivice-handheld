package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.image.Picture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RasterizerTests {
    @Test fun `triangle fills its interior and leaves exterior untouched`() {
        val picture = Picture(5, 5, IntArray(25))
        Rasterizer(picture).triangle(
            arrayOf(Vec2(1.0, 1.0), Vec2(4.0, 1.0), Vec2(1.0, 4.0)),
            null, null, Array(3) { Vec3(255.0, 50.0, 10.0) }, false, 0,
        )
        assertEquals(0xffff320a.toInt(), picture.argb[2 * 5 + 1])
        assertEquals(0, picture.argb[0])
        assertEquals(0, picture.argb[4 * 5 + 4])
    }

    @Test fun `degenerate triangle does not paint`() {
        val picture = Picture(2, 2, IntArray(4))
        Rasterizer(picture).triangle(Array(3) { Vec2(1.0, 1.0) }, null, null,
            Array(3) { Vec3(255.0, 255.0, 255.0) }, false, 0)
        assertEquals(0, picture.argb.sum())
    }

    @Test fun `invalid render size is rejected before loading a model`() {
        assertThrows(IllegalArgumentException::class.java) { modelOutputSize(0) }
        assertThrows(IllegalArgumentException::class.java) { modelOutputSize(2048) }
    }

    @Test fun `model archives preserve entries after a null offset`() {
        val bytes = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(16).putInt(0).putInt(20).putInt(24).putInt(111).putInt(222).putInt(333).array()
        val archive = ModelArchive(bytes)
        assertEquals(0, archive.entry(1).size)
        assertEquals(222, ByteBuffer.wrap(archive.entry(2)).order(ByteOrder.LITTLE_ENDIAN).int)
        assertThrows(IOException::class.java) { archive.entry(-1) }
        assertThrows(IOException::class.java) { ModelArchive(bytes.copyOf(12)) }
    }

    @Test fun `texture coordinates outside VRAM are rejected`() {
        val texture = ModelTexture(IntArray(1024 * 512), 832, TexturePage(4, 0, 0, 0))
        assertThrows(IOException::class.java) { texture.texel(0, 0) }
    }

    @Test fun `a semitransparent red texel preserves alpha on an empty fighter image`() {
        val picture = Picture(2, 2, IntArray(4))
        val vram = IntArray(1024 * 512).also { it[16] = 0x801f }
        Rasterizer(picture).triangle(arrayOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0)),
            Array(3) { Vec2(0.0, 0.0) }, ModelTexture(vram, 0, TexturePage(0, 16, 0, 0)),
            Array(3) { Vec3(128.0, 128.0, 128.0) }, false, 1)
        assertEquals(0x7fff0000, picture.argb[0])
    }

    @Test fun `supersampling filters premultiplied color instead of darkening edges`() {
        val small = downsample(Picture(2, 2, intArrayOf(0xffff0000.toInt(), 0, 0, 0)))
        assertEquals(0x40ff0000, small.argb.single())
    }

    @Test fun `fourfold supersampling retains color at transparent edges`() {
        val pixels = IntArray(16).also { it[0] = 0xffff0000.toInt() }
        assertEquals(0x10ff0000, downsampleFour(Picture(4, 4, pixels)).argb.single())
    }

    @Test fun `matrix accumulation keeps low bits through cancellation`() {
        val a = Vec3(-1.0000000000000004, 1.0000000000000002, 0.0)
        val b = Vec3(1.0, 1.0000000000000002, 0.0)
        val expected = Math.scalb(1.0, -104)
        assertEquals(expected, a.dot(b))
        val matrix = Matrix(doubleArrayOf(a.x, a.y, a.z, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))
        assertEquals(expected, (matrix * b).x)
        assertEquals(0.0, a.dotSeparate(b))
    }

    @Test fun `nearly edge-on fighter faces cannot change the image bounds`() {
        val a = Vec3(0.0, 0.0, 0.0); val b = Vec3(1.0, 1.0, 0.0)
        assertFalse(fighterFrontFace(arrayOf(a, b, Vec3(2.0, 2.0 + 1e-9, 0.0))))
        assertTrue(fighterFrontFace(arrayOf(a, b, Vec3(2.0, 3.0, 0.0))))
    }

    @Test fun `aliased key entries share a cumulative allocation budget`() {
        val model = ModelData(aliasedModel(6, vectors = false), 0, 2, 0)
        val error = assertThrows(IOException::class.java) { model.polygons { it } }
        assertEquals("Model key budget exceeded", error.message)
    }

    @Test fun `vertices and normals share a cumulative allocation budget across meshes`() {
        val model = ModelData(aliasedModel(9, vectors = true), 0, 2, 0)
        val error = assertThrows(IOException::class.java) { model.polygons { it } }
        assertEquals("Model vector budget exceeded", error.message)
    }

    private fun aliasedModel(aliases: Int, vectors: Boolean): ByteArray {
        fun buffer(size: Int) = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        fun archive(entries: List<ByteArray>, indices: IntArray = entries.indices.toList().toIntArray()): ByteArray {
            val start = indices.size * 4
            val offsets = IntArray(entries.size)
            var offset = start
            entries.forEachIndexed { i, bytes -> offsets[i] = offset; offset += bytes.size }
            return buffer(offset).apply {
                indices.forEach { putInt(offsets[it]) }; entries.forEach { put(it) }
            }.array()
        }
        val keyCount = if (vectors) 1 else 4096
        val tracks = List(3) { track -> buffer(keyCount * 8).apply {
            repeat(keyCount) {
                repeat(3) { putShort(if (track == 2) 4096 else 0) }; putShort(0)
            }
        }.array() }
        val keys = archive(tracks)
        val model = buffer(8 + aliases * 12).apply {
            putInt(1); putInt(aliases)
            repeat(aliases) { i ->
                putInt(0); putInt(if (vectors) 3 + i else 0); putInt(if (vectors) 3 + aliases else 3 + i)
            }
        }.array()
        val tim = buffer(22).putInt(0x10).putInt(2).putInt(14)
            .putShort(0).putShort(0).putShort(1).putShort(1).putShort(0).array()
        val textures = archive(listOf(tim))
        val motion = buffer(10).putInt(4).putInt(0).putShort(0).array()
        if (!vectors) return archive(listOf(model, textures, motion, keys), intArrayOf(0, 1, 2) + IntArray(aliases) { 3 })
        val block = 6 + 4096 * 6
        val mesh = buffer(24 + block * 2 + 1).apply {
            putInt(24); putInt(24 + block); putInt(24 + block * 2); repeat(3) { putInt(0) }
            repeat(2) { putShort(4096); putInt(0); repeat(4096 * 3) { putShort(0) } }
            put(255.toByte())
        }.array()
        return archive(listOf(model, textures, motion, mesh, keys),
            intArrayOf(0, 1, 2) + IntArray(aliases) { 3 } + intArrayOf(4))
    }
}
