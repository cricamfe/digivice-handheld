package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.Picture
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

internal data class ModelTexture(val vram: IntArray, val x: Int, val page: TexturePage) {
    fun texel(u: Int, v: Int): Int {
        val px = x + (page.page shl 6)
        val wordX = px + if (page.tp == 0) u / 4 else u / 2
        if (wordX !in 0..1023 || v !in 0..511) invalidDisc("Model texel outside VRAM")
        val word = vram[v * 1024 + wordX]
        val index = if (page.tp == 0) (word shr ((u and 3) * 4)) and 15 else (word shr ((u and 1) * 8)) and 255
        val paletteX = x + page.cx + index
        if (paletteX !in 0..1023 || page.cy !in 0..511) invalidDisc("Model palette outside VRAM")
        return vram[page.cy * 1024 + paletteX]
    }
}

internal class Rasterizer(private val picture: Picture, private val stage: Boolean = false) {
    private var budget = picture.width.toLong() * picture.height * 64

    fun triangle(points: Array<Vec2>, uv: Array<Vec2>?, texture: ModelTexture?, colors: Array<Vec3>, gouraud: Boolean, abr: Int) {
        val x0 = maxOf(floor(points.minOf { it.x }).toInt(), 0)
        val x1 = minOf(ceil(points.maxOf { it.x }).toInt(), picture.width - 1)
        val y0 = maxOf(floor(points.minOf { it.y }).toInt(), 0)
        val y1 = minOf(ceil(points.maxOf { it.y }).toInt(), picture.height - 1)
        if (x0 > x1 || y0 > y1) return
        budget -= (x1 - x0 + 1L) * (y1 - y0 + 1)
        if (budget < 0) invalidDisc("Model drawing budget exceeded")
        val a = points[0]; val b = points[1]; val c = points[2]
        val den = (b.y - c.y) * (a.x - c.x) + (c.x - b.x) * (a.y - c.y)
        if (abs(den) < 1e-9) return
        val epsilon = if (stage) 0.0 else -1e-6
        for (y in y0..y1) for (x in x0..x1) {
            val gx = x + 0.5; val gy = y + 0.5
            val w0 = ((b.y - c.y) * (gx - c.x) + (c.x - b.x) * (gy - c.y)) / den
            val w1 = ((c.y - a.y) * (gx - c.x) + (a.x - c.x) * (gy - c.y)) / den
            val w2 = 1 - w0 - w1
            if (w0 < epsilon || w1 < epsilon || w2 < epsilon) continue
            val index = y * picture.width + x
            if (texture == null) {
                val color = if (stage) interpolate(colors, w0, w1, w2) else colors[0]
                picture.argb[index] = argb(255, channel(color.x), channel(color.y), channel(color.z))
                continue
            }
            checkNotNull(uv)
            val u = floor(w0 * uv[0].x + w1 * uv[1].x + w2 * uv[2].x + 1e-4).toInt().coerceIn(0, 255)
            val v = floor(w0 * uv[0].y + w1 * uv[1].y + w2 * uv[2].y + 1e-4).toInt().coerceIn(0, 255)
            val texel = texture.texel(u, v)
            if (texel == 0) continue
            val shade = if (stage || gouraud) interpolate(colors, w0, w1, w2) else colors[0]
            fun expand(value: Int): Int { val s = value shl 3; return s or (s shr 5) }
            val r = (expand(texel and 31) * shade.x / 128.0).coerceIn(0.0, 255.0)
            val g = (expand((texel shr 5) and 31) * shade.y / 128.0).coerceIn(0.0, 255.0)
            val b = (expand((texel shr 10) and 31) * shade.z / 128.0).coerceIn(0.0, 255.0)
            if (abr == 0 || texel and 0x8000 == 0) {
                picture.argb[index] = argb(255, r.toInt(), g.toInt(), b.toInt())
            } else {
                val dest = picture.argb[index]
                val dr = (dest ushr 16) and 255; val dg = (dest ushr 8) and 255; val db = dest and 255
                if (stage) {
                    fun blend(src: Double, dst: Int): Int = channel(when (abr) {
                        1 -> dst / 2.0 + src / 2.0
                        2 -> dst + src
                        3 -> dst - src
                        else -> dst + src / 4.0
                    })
                    picture.argb[index] = argb(255, blend(r, dr), blend(g, dg), blend(b, db))
                } else {
                    val alpha = if (abr == 1) 0.5 else 0.75
                    val da = (dest ushr 24) / 255.0
                    val oa = alpha + da * (1 - alpha)
                    fun blend(src: Double, dst: Int): Int = channel((src * alpha + dst * da * (1 - alpha)) / maxOf(oa, 1e-6))
                    picture.argb[index] = argb((oa * 255).toInt(), blend(r, dr), blend(g, dg), blend(b, db))
                }
            }
        }
    }
}

private fun interpolate(c: Array<Vec3>, a: Double, b: Double, d: Double) = Vec3(
    a * c[0].x + b * c[1].x + d * c[2].x,
    a * c[0].y + b * c[1].y + d * c[2].y,
    a * c[0].z + b * c[1].z + d * c[2].z,
)
private fun channel(value: Double): Int = value.coerceIn(0.0, 255.0).toInt()
private fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

internal fun modelOutputSize(longSide: Int): Int {
    // shortcut: the handheld needs at most 960px models; revisit the allocation budget before larger exports.
    require(longSide in 1..960) { "Model size must be between 1 and 960 pixels" }
    return longSide
}

internal fun downsample(picture: Picture): Picture {
    val width = picture.width / 2; val height = picture.height / 2
    val result = IntArray(width * height)
    val sums = DoubleArray(4)
    for (y in 0 until height) for (x in 0 until width) {
        sums.fill(0.0)
        for (dy in 0..1) for (dx in 0..1) {
            val pixel = picture.argb[(y * 2 + dy) * picture.width + x * 2 + dx]
            val a = (pixel ushr 24).toDouble()
            sums[0] += ((pixel ushr 16) and 255) * (a / 255.0)
            sums[1] += ((pixel ushr 8) and 255) * (a / 255.0)
            sums[2] += (pixel and 255) * (a / 255.0)
            sums[3] += a
        }
        val alpha = sums[3] / 4
        fun color(c: Int): Int = if (alpha > 0) channel(sums[c] / 4 * 255.0 / maxOf(alpha, 1e-6) + 0.5) else 0
        result[y * width + x] = argb(channel(alpha + 0.5), color(0), color(1), color(2))
    }
    return Picture(width, height, result)
}

/** Fourfold box filter in premultiplied color, preserving transparent edge colors. */
internal fun downsampleFour(picture: Picture): Picture {
    val width = picture.width / 4; val height = picture.height / 4
    val result = IntArray(width * height)
    val sums = DoubleArray(4)
    for (y in 0 until height) for (x in 0 until width) {
        sums.fill(0.0)
        for (dy in 0..3) for (dx in 0..3) {
            val pixel = picture.argb[(y * 4 + dy) * picture.width + x * 4 + dx]
            val a = (pixel ushr 24).toDouble()
            sums[0] += ((pixel ushr 16) and 255) * (a / 255.0)
            sums[1] += ((pixel ushr 8) and 255) * (a / 255.0)
            sums[2] += (pixel and 255) * (a / 255.0)
            sums[3] += a
        }
        val alpha = sums[3] / 16
        fun color(c: Int): Int = if (alpha > 0) channel(sums[c] / 16 * 255.0 / maxOf(alpha, 1e-6) + 0.5) else 0
        result[y * width + x] = argb(channel(alpha + 0.5), color(0), color(1), color(2))
    }
    return Picture(width, height, result)
}
