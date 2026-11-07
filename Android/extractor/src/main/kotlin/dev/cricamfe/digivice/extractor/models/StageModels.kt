package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.intAt
import dev.cricamfe.digivice.extractor.image.range

class StageModels(private val files: DiscFiles, private val table: GameFileTable) {
    private val data = modelResource(files, table, 0x1cb)
    val stages: List<Int> = (1..27).toList() + 47

    fun render(stage: Int): Picture {
        require(stage in stages) { "Unknown battle stage" }
        val info = StageInfo(data, stage)
        val model = ModelData(modelResource(files, table, info.model ushr 16), info.model and 0xffff,
            info.motions and 0xffff, 0x280)
        val camera = Camera()
        val polygons = model.polygons(info::colors).mapNotNull { item ->
            val points = Array(item.points.size) { i -> camera.transform(item.points[i], item.points.size == 3 && i == 2) }
            if (points.any { !it.x.isFinite() || !it.y.isFinite() || !it.z.isFinite() }) invalidDisc("Invalid stage transformation")
            if (points[0].dot(points[1].cross(points[2])) <= 0) null else {
                val colors = if (item.polygon.lit) item.colors else Array(points.size) { Vec3(128.0, 128.0, 128.0) }
                ModelPolygon(points, colors, item.polygon)
            }
        }.sortedByDescending { it.points.sumOf { point -> point.z } / it.points.size }
        val width = 640; val height = 480
        val picture = Picture(width, height, IntArray(width * height) { info.background })
        val rasterizer = Rasterizer(picture, stage = true)
        for (item in polygons) {
            val texture = item.polygon.texture?.let { ModelTexture(model.vram, model.textureX, it) }
            val vertices = Array(item.points.size) { i ->
                ClipVertex(item.points[i], item.polygon.uv?.get(i) ?: Vec2(0.0, 0.0), item.colors[i])
            }
            val triangles = if (vertices.size == 3) listOf(intArrayOf(0, 1, 2)) else listOf(intArrayOf(0, 1, 2), intArrayOf(1, 3, 2))
            for (tri in triangles) {
                val clipped = clipNear(tri.map { vertices[it] })
                if (clipped.size < 3) continue
                val projected = clipped.map { Vec2((it.point.x * 280.0 / it.point.z + 160) * 2,
                    (it.point.y * 280.0 / it.point.z + 120) * 2) }
                for (i in 1 until clipped.size - 1) {
                    val indices = intArrayOf(0, i, i + 1)
                    rasterizer.triangle(Array(3) { projected[indices[it]] }, Array(3) { clipped[indices[it]].uv },
                        texture, Array(3) { clipped[indices[it]].color }, true, item.polygon.abr)
                }
            }
        }
        return picture
    }
}

private class StageInfo(bytes: ByteArray, stage: Int) {
    private val offset = stage * 0x58
    val model: Int
    val motions: Int
    val background: Int
    private val rays: List<Pair<Vec3, Vec3>>
    private val ambient: Vec3
    init {
        bytes.range(offset, 0x58)
        model = bytes.intAt(offset); motions = bytes.intAt(offset + 4)
        fun byte(at: Int) = bytes[offset + at].toInt() and 255
        background = (255 shl 24) or (byte(12) shl 16) or (byte(13) shl 8) or byte(14)
        rays = List(3) { i ->
            val p = offset + 0x18 + 16 * i
            val ray = Vec3(bytes.signedModelInt(p).toDouble(), bytes.signedModelInt(p + 4).toDouble(), bytes.signedModelInt(p + 8).toDouble()).unit()
            ray to Vec3((bytes[p + 12].toInt() and 255).toDouble(), (bytes[p + 13].toInt() and 255).toDouble(),
                (bytes[p + 14].toInt() and 255).toDouble())
        }
        ambient = Vec3(bytes.signedModelInt(offset + 0x48).toDouble(), bytes.signedModelInt(offset + 0x4c).toDouble(), bytes.signedModelInt(offset + 0x50).toDouble())
    }

    fun colors(normals: Array<Vec3>): Array<Vec3> = Array(normals.size) { index ->
        var color = ambient
        for ((ray, light) in rays) color += light * maxOf(0.0, normals[index].dot(ray * -1.0))
        Vec3(color.x.coerceIn(0.0, 255.0), color.y.coerceIn(0.0, 255.0), color.z.coerceIn(0.0, 255.0))
    }
}

private class Camera {
    private val view = Vec3(-2432.0, -2496.0, -11008.0)
    private val z = (Vec3(1408.0, -1408.0, 4864.0) - view).unit()
    private val x = Vec3(0.0, 1.0, 0.0).cross(z).unit()
    private val y = z.cross(x)
    fun transform(point: Vec3, separate: Boolean): Vec3 {
        val relative = point - view
        // The reference transforms pairs with FMA and an odd trailing vertex with separate sums.
        if (separate) return Vec3(relative.dotSeparate(x), relative.dotSeparate(y), relative.dotSeparate(z))
        return Vec3(relative.dot(x), relative.dot(y), relative.dot(z))
    }
}

private data class ClipVertex(val point: Vec3, val uv: Vec2, val color: Vec3)

private fun clipNear(vertices: List<ClipVertex>): List<ClipVertex> {
    val result = ArrayList<ClipVertex>()
    vertices.forEachIndexed { i, a ->
        val b = vertices[(i + 1) % vertices.size]
        val insideA = a.point.z >= 64.0; val insideB = b.point.z >= 64.0
        if (insideA) result.add(a)
        if (insideA != insideB) {
            val t = (64.0 - a.point.z) / (b.point.z - a.point.z)
            result.add(ClipVertex(a.point + (b.point - a.point) * t,
                Vec2(a.uv.x + (b.uv.x - a.uv.x) * t, a.uv.y + (b.uv.y - a.uv.y) * t),
                a.color + (b.color - a.color) * t))
        }
    }
    return result
}
