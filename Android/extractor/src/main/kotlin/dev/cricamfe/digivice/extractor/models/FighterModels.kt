package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.MAX_PIXELS
import dev.cricamfe.digivice.extractor.image.intAt
import dev.cricamfe.digivice.extractor.image.range
import dev.cricamfe.digivice.extractor.image.shortAt
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** IDs are the global game fighter IDs; enemies and partner forms do not overlap. */
class FighterModels(private val files: DiscFiles, private val table: GameFileTable) {
    private val data = modelResource(files, table, 0x1cc)
    private val entries = LinkedHashMap<Int, Pair<Int, Int>>()
    private val partnerOffset = data.intAt(8)
    private val enemyOffset = data.intAt(12)
    val enemies: List<Int> = enemyIds.toList()
    val partners: List<Int> = partnerIds.toList()

    init {
        var p = data.intAt(4)
        while (true) {
            data.range(p, 4)
            val id = data.shortAt(p)
            if (id == 0) break
            if (id < 0 || entries.size >= 512) invalidDisc("Invalid fighter table")
            val index = data[p + 2].toInt() and 255
            val kind = data[p + 3].toInt() and 255
            if (entries.put(id, index to kind) != null) invalidDisc("Duplicate fighter id")
            p += 4
        }
        if (enemies.any { (entries[it]?.second ?: 0) < 0x3a } || partners.any { (entries[it]?.second ?: 255) >= 0x3a }) {
            invalidDisc("Expected fighter entries are missing")
        }
    }

    fun render(id: Int, longSide: Int): Picture = renderModel(id, longSide, false)

    /** Original idle pose, +25° yaw/+6° pitch, balanced warm key/cool rim and fourfold SSAA. */
    fun renderEvolution(id: Int, longSide: Int): Picture {
        // Fourfold SSAA keeps the same raster budget as the RAW renderer's twofold 960px limit.
        require(longSide in 1..480) { "Evolution output size must be between 1 and 480" }
        require(id in partners) { "Unknown evolution id" }
        return renderModel(id, longSide, true)
    }

    /** Original idle poses in an 8 by 6 grid of 176px frames. */
    fun renderBattleAnimation(id: Int): Picture {
        require(id in enemies) { "Unknown enemy id" }
        val model = loadModel(id)
        val frames = model.idleFrames(BATTLE_COLUMNS * BATTLE_ROWS)
        val side = BATTLE_FRAME_SIDE
        val rasterSide = side * 2
        val width = side * BATTLE_COLUMNS
        val height = side * BATTLE_ROWS
        fun polygons(frame: Int) = model.polygons(battleView, frame) { normals ->
            Array(normals.size) { battleLight(normals[it]) }
        }.filter { fighterFrontFace(it.points) }
        var x0 = Double.POSITIVE_INFINITY
        var y0 = Double.POSITIVE_INFINITY
        var x1 = Double.NEGATIVE_INFINITY
        var y1 = Double.NEGATIVE_INFINITY
        // All poses share one crop so the image never changes size as a limb moves.
        for (frame in frames) for (polygon in polygons(frame)) for (point in polygon.points) {
            x0 = minOf(x0, point.x)
            y0 = minOf(y0, point.y)
            x1 = maxOf(x1, point.x)
            y1 = maxOf(y1, point.y)
        }
        val span = maxOf(x1 - x0, y1 - y0)
        if (span <= 0 || !span.isFinite()) invalidDisc("Fighter animation has no visible extent")
        val scale = (rasterSide - 16) / span
        val padX = (rasterSide - (x1 - x0) * scale) / 2
        val padY = (rasterSide - (y1 - y0) * scale) / 2
        val atlas = Picture(width, height, IntArray(width * height))
        for ((index, frame) in frames.withIndex()) {
            val picture = Picture(rasterSide, rasterSide, IntArray(rasterSide * rasterSide))
            rasterize(model, picture, polygons(frame)) { point ->
                Vec2((point.x - x0) * scale + padX, (point.y - y0) * scale + padY)
            }
            val sampled = downsample(picture)
            val frameX = index % BATTLE_COLUMNS * side
            val frameY = index / BATTLE_COLUMNS * side
            repeat(side) { y ->
                sampled.argb.copyInto(atlas.argb, (frameY + y) * width + frameX, y * side, (y + 1) * side)
            }
        }
        return atlas
    }

    private fun loadModel(id: Int): ModelData {
        val (index, kind) = entries[id] ?: throw IllegalArgumentException("Unknown fighter id")
        val offset = if (kind >= 0x3a) enemyOffset + index * 0x48 else partnerOffset + index * 0xc4
        val reference = data.intAt(offset)
        val motions = data.intAt(offset + 4)
        return ModelData(modelResource(files, table, reference ushr 16), reference and 0xffff, motions and 0xffff, 832)
    }

    private fun renderModel(id: Int, longSide: Int, evolution: Boolean): Picture {
        modelOutputSize(longSide)
        val model = loadModel(id)
        val light = Vec3(-0.35, -0.55, -0.75).unit()
        val polygons = model.polygons(if (evolution) evolutionView else null) { normals -> Array(normals.size) {
            if (evolution) evolutionLight(normals[it]) else {
                val value = 128 * (0.62 + 0.62 * maxOf(0.0, normals[it].dot(light)))
                Vec3(value, value, value)
            }
        } }.filter { fighterFrontFace(it.points) }
        if (polygons.isEmpty()) invalidDisc("Fighter model has no visible polygons")
        val points = polygons.flatMap { it.points.asList() }
        if (points.any { !it.x.isFinite() || !it.y.isFinite() || !it.z.isFinite() }) invalidDisc("Invalid model transformation")
        val x0 = points.minOf { it.x }; val y0 = points.minOf { it.y }
        val x1 = points.maxOf { it.x }; val y1 = points.maxOf { it.y }
        val span = maxOf(x1 - x0, y1 - y0)
        if (span <= 0 || !span.isFinite()) invalidDisc("Fighter model has no extent")
        val supersample = if (evolution) 4 else 2
        val pad = 2 * supersample
        val scale = longSide * supersample / span
        val width = ceil((x1 - x0) * scale).toInt() + pad * 2
        val height = ceil((y1 - y0) * scale).toInt() + pad * 2
        require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= MAX_PIXELS) {
            "Fighter raster exceeds the allocation budget"
        }
        val picture = Picture(width, height, IntArray(width * height))
        rasterize(model, picture, polygons) { point -> Vec2((point.x - x0) * scale + pad, (point.y - y0) * scale + pad) }
        if (!evolution) return downsample(picture)
        val sampled = downsampleFour(picture)
        val result = Picture(sampled.width + 16, sampled.height + 16, IntArray((sampled.width + 16) * (sampled.height + 16)))
        repeat(sampled.height) { y -> sampled.argb.copyInto(result.argb, (y + 8) * result.width + 8,
            y * sampled.width, (y + 1) * sampled.width) }
        return result
    }

    private fun rasterize(model: ModelData, picture: Picture, polygons: List<ModelPolygon>, project: (Vec3) -> Vec2) {
        val rasterizer = Rasterizer(picture)
        for (item in polygons.sortedByDescending { polygon -> polygon.points.sumOf { it.z } / polygon.points.size }) {
            val polygon = item.polygon
            val screen = Array(item.points.size) { project(item.points[it]) }
            val texture = polygon.texture?.let { ModelTexture(model.vram, model.textureX, it) }
            val triangles = if (screen.size == 3) listOf(intArrayOf(0, 1, 2)) else listOf(intArrayOf(0, 1, 2), intArrayOf(1, 3, 2))
            for (tri in triangles) rasterizer.triangle(Array(3) { screen[tri[it]] },
                polygon.uv?.let { uv -> Array(3) { uv[tri[it]] } }, texture,
                Array(3) { item.colors[tri[it]] }, polygon.gouraud, polygon.abr)
        }
    }

    private companion object {
        const val BATTLE_COLUMNS = 8
        const val BATTLE_ROWS = 6
        const val BATTLE_FRAME_SIDE = 176
        val evolutionView = camera(25.0, 6.0)
        val battleView = camera(-35.0, 5.0)

        fun camera(yawDegrees: Double, pitchDegrees: Double): Matrix {
            val yaw = Math.toRadians(yawDegrees); val pitch = Math.toRadians(pitchDegrees)
            return Matrix(doubleArrayOf(1.0, 0.0, 0.0, 0.0, cos(pitch), -sin(pitch), 0.0, sin(pitch), cos(pitch))) *
                Matrix(doubleArrayOf(cos(yaw), 0.0, sin(yaw), 0.0, 1.0, 0.0, -sin(yaw), 0.0, cos(yaw)))
        }
        val battleKey = Vec3(-0.68, -0.67, -0.66).unit()
        val battleRim = Vec3(0.90, -0.30, 0.32).unit()

        fun battleLight(normal: Vec3): Vec3 {
            val n = normal.unit()
            val k = maxOf(0.0, n.dot(battleKey)).pow(1.16)
            val edge = 1.0 - minOf(1.0, abs(n.z))
            val r = edge * edge * maxOf(0.0, n.dot(battleRim))
            val red = 0.42 + k * 0.83 + r * 0.12
            val green = 0.44 + k * 0.74 + r * 0.29
            val blue = 0.49 + k * 0.62 + r * 0.52
            val limit = 128.0 * minOf(1.0, 1.18 / maxOf(red, green, blue))
            return Vec3(red * limit, green * limit, blue * limit)
        }
        val key = Vec3(-0.55, -0.65, -1.0).unit()
        val fill = Vec3(0.55, -0.15, -1.0).unit()
        val rim = Vec3(0.96, -0.25, 0.06).unit()

        fun evolutionLight(normal: Vec3): Vec3 {
            // NumPy's single-vector norm/dot uses separate operations, unlike its batched mesh transform.
            val length = sqrt(normal.dotSeparate(normal))
            val n = if (length == 0.0) normal else Vec3(normal.x / length, normal.y / length, normal.z / length)
            val edge = (1.0 - minOf(1.0, abs(n.z))).pow(3.2)
            val k = 0.62 * maxOf(0.0, n.dotSeparate(key))
            val f = 0.28 * maxOf(0.0, n.dotSeparate(fill))
            val r = 0.65 * edge * maxOf(0.0, n.dotSeparate(rim))
            val red = maxOf(0.50, 0.56 + k * 1.18 + f * 0.76 + r * 0.36)
            val green = maxOf(0.50, 0.56 + k + f * 0.91 + r * 0.82)
            val blue = maxOf(0.50, 0.56 + k * 0.78 + f * 1.14 + r * 1.52)
            // A shared ceiling retains the warm/cool light ratios on bright surfaces.
            val limit = minOf(1.0, 1.0 / maxOf(red, green, blue))
            return Vec3(128 * (red * limit), 128 * (green * limit), 128 * (blue * limit))
        }

        // Numeric IDs selected by the current enemy and digievolution databases.
        val enemyIds = intArrayOf(
            4, 7, 8, 9, 10, 11, 13, 14, 23, 24, 25, 28, 32, 34, 35, 37,
            38, 39, 40, 41, 42, 49, 50, 51, 52, 53, 54, 60, 61, 65, 67, 69,
            70, 76, 77, 80, 89, 94, 104, 108, 110, 115, 119, 121, 122, 124, 126, 132,
            134, 135, 136, 137, 138, 139, 140, 141, 143, 165, 166, 170, 171, 172, 173, 175,
            176, 177, 178, 189, 190, 193, 197, 198, 200, 202, 203, 204, 206, 207, 208, 210,
            212, 215, 217, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 231, 236, 237,
            238, 241, 244, 245, 247, 250, 251, 269, 270, 272, 273, 277, 281, 302, 312, 327,
            334, 337, 356, 358, 360, 364, 365, 382, 395, 396, 397, 398, 399, 400, 401, 402,
            403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418,
            419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431, 432, 433, 434,
            435, 436, 437, 444, 457, 458, 459, 460, 461, 462, 463, 464, 465,
        )
        val partnerIds = intArrayOf(
            5, 6, 12, 19, 20, 26, 27, 56, 59, 66, 144, 148, 150, 151, 196, 211,
            213, 214, 230, 234, 254, 259, 260, 267, 359, 367, 368, 369, 372, 374, 375, 376,
            377, 378, 381, 386, 387, 388, 389, 390, 391, 392, 393, 394,
        )
    }
}

internal fun fighterFrontFace(points: Array<Vec3>): Boolean {
    val a = points[0]; val b = points[1]; val c = points[2]
    // Ignore edge-on faces whose roundoff could change both culling and the image bounds.
    return a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y) > 1e-8
}
