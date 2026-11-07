package dev.cricamfe.digivice.extractor.models

import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.MAX_DATA
import dev.cricamfe.digivice.extractor.image.Rlen
import dev.cricamfe.digivice.extractor.image.Tim
import dev.cricamfe.digivice.extractor.image.intAt
import dev.cricamfe.digivice.extractor.image.range
import dev.cricamfe.digivice.extractor.image.shortAt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal data class Vec2(val x: Double, val y: Double)
internal data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(v: Vec3) = Vec3(x + v.x, y + v.y, z + v.z)
    operator fun minus(v: Vec3) = Vec3(x - v.x, y - v.y, z - v.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    fun dot(v: Vec3): Double = Math.fma(z, v.z, Math.fma(y, v.y, x * v.x))
    fun dotSeparate(v: Vec3): Double = x * v.x + y * v.y + z * v.z
    fun cross(v: Vec3) = Vec3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)
    fun unit(): Vec3 { val n = sqrt(dot(this)); return if (n == 0.0) this else Vec3(x / n, y / n, z / n) }
    operator fun get(i: Int): Double = when (i) { 0 -> x; 1 -> y; else -> z }
}

internal class Matrix(private val values: DoubleArray) {
    // FMA preserves the reference renderer's BLAS rounding, including nearly edge-on polygons.
    operator fun times(v: Vec3) = Vec3(
        Math.fma(values[2], v.z, Math.fma(values[1], v.y, values[0] * v.x)),
        Math.fma(values[5], v.z, Math.fma(values[4], v.y, values[3] * v.x)),
        Math.fma(values[8], v.z, Math.fma(values[7], v.y, values[6] * v.x)),
    )
    // A single position uses separate rounding in the reference's matrix-vector operation.
    fun transformPosition(v: Vec3) = Vec3(
        values[0] * v.x + values[1] * v.y + values[2] * v.z,
        values[3] * v.x + values[4] * v.y + values[5] * v.z,
        values[6] * v.x + values[7] * v.y + values[8] * v.z,
    )
    operator fun times(b: Matrix): Matrix = Matrix(DoubleArray(9) { index ->
        val r = index / 3 * 3; val c = index % 3
        Math.fma(values[r + 2], b.values[c + 6], Math.fma(values[r + 1], b.values[c + 3], values[r] * b.values[c]))
    })
    fun scaled(scale: Vec3) = Matrix(DoubleArray(9) { values[it] * (scale[it % 3] / 4096.0) })

    companion object {
        fun rotation(r: Vec3): Matrix {
            val ax = r.x * 2 * PI / 4096.0; val ay = r.y * 2 * PI / 4096.0; val az = r.z * 2 * PI / 4096.0
            val cx = cos(ax); val sx = sin(ax); val cy = cos(ay); val sy = sin(ay); val cz = cos(az); val sz = sin(az)
            val rx = Matrix(doubleArrayOf(1.0, 0.0, 0.0, 0.0, cx, -sx, 0.0, sx, cx))
            val ry = Matrix(doubleArrayOf(cy, 0.0, sy, 0.0, 1.0, 0.0, -sy, 0.0, cy))
            val rz = Matrix(doubleArrayOf(cz, -sz, 0.0, sz, cz, 0.0, 0.0, 0.0, 1.0))
            return rz * ry * rx
        }
    }
}

internal fun modelResource(files: DiscFiles, table: GameFileTable, index: Int): ByteArray {
    if (index !in 0 until 2382) invalidDisc("Model file index is invalid")
    val file = files.atLba(table.lbaOf(index)) ?: invalidDisc("Model resource is missing")
    if (file.size !in 4..MAX_DATA) invalidDisc("Model resource is too large")
    return files.read(file)
}

internal fun ByteArray.signedModelInt(offset: Int): Int {
    range(offset, 4)
    return (this[offset].toInt() and 255) or ((this[offset + 1].toInt() and 255) shl 8) or
        ((this[offset + 2].toInt() and 255) shl 16) or (this[offset + 3].toInt() shl 24)
}

/** Model archives retain null entries, unlike sprite archives whose first null terminates the table. */
internal class ModelArchive(private val bytes: ByteArray) {
    val offsets: IntArray
    init {
        val first = bytes.intAt(0)
        if (first < 4 || first % 4 != 0 || first > bytes.size || first / 4 > 4096) invalidDisc("Invalid model archive")
        offsets = IntArray(first / 4) { bytes.intAt(it * 4) }
        if (offsets.any { it != 0 && it !in first..bytes.size }) invalidDisc("Model archive entry outside the file")
    }
    fun entry(index: Int): ByteArray {
        if (index !in offsets.indices) invalidDisc("Invalid model entry index")
        val start = offsets[index]
        if (start == 0) return byteArrayOf()
        val end = offsets.filter { it > start }.minOrNull() ?: bytes.size
        return bytes.copyOfRange(start, end)
    }
}

internal data class TexturePage(val page: Int, val cx: Int, val cy: Int, val tp: Int)
internal data class Polygon(
    val indices: IntArray, val normals: IntArray?, val uv: Array<Vec2>?, val texture: TexturePage?,
    val lit: Boolean, val gouraud: Boolean, val abr: Int, val color: Vec3,
)
internal data class Mesh(val vertices: Array<Vec3>, val normals: Array<Vec3>, val polygons: List<Polygon>)
internal data class Bone(val parent: Int, val mesh: Int, val keys: Int)
internal data class Transform(val rotation: Matrix, val translation: Vec3, val visible: Boolean)
internal data class ModelPolygon(val points: Array<Vec3>, val colors: Array<Vec3>, val polygon: Polygon)

internal class ModelData(data: ByteArray, modelEntry: Int, private val motionEntry: Int, val textureX: Int) {
    private val archive = ModelArchive(data)
    private val bones: List<Bone>
    val vram = IntArray(1024 * 512)
    private val meshes = HashMap<Int, Mesh>()
    private val keys = HashMap<Int, List<Array<IntArray>>>()
    // Distinct entry indices may alias bytes, so resource size alone cannot bound cached objects.
    private var remainingKeys = 65536
    private var remainingVectors = 65536

    init {
        val model = archive.entry(modelEntry)
        val textures = model.intAt(0)
        val count = model.intAt(4)
        if (count !in 1..255) invalidDisc("Invalid model bone count")
        model.range(8, count * 12)
        bones = listOf(Bone(0, 0, 0)) + List(count) { i ->
            val offset = 8 + i * 12
            val parent = model.intAt(offset)
            if (parent !in 0..i) invalidDisc("Invalid model bone hierarchy")
            Bone(parent, model.intAt(offset + 4), model.intAt(offset + 8))
        }
        val textureArchive = ModelArchive(archive.entry(textures))
        var x = textureX
        for (i in textureArchive.offsets.indices) {
            if (textureArchive.offsets[i] == 0) break
            val tim = Tim.parse(Rlen.unpack(textureArchive.entry(i)))
            if (x < 0 || x + tim.wordWidth > 1024 || tim.height > 512) invalidDisc("Model texture exceeds VRAM")
            repeat(tim.height) { y -> tim.words.copyInto(vram, y * 1024 + x, y * tim.wordWidth, (y + 1) * tim.wordWidth) }
            x += 0x40
        }
    }

    fun idleFrame(): Int {
        val motion = archive.entry(motionEntry)
        return motion.shortAt(motion.intAt(0) + 4)
    }

    fun idleFrames(samples: Int): IntArray {
        val motion = archive.entry(motionEntry)
        val start = motion.intAt(0)
        motion.range(start, 8)
        val count = motion.shortAt(start + 2)
        val first = motion.shortAt(start + 4)
        val last = motion.shortAt(start + 6)
        // shortcut: PAL enemies use one contiguous idle run; use the first pose if another layout appears.
        if (count <= 0 || first < 0 || last - first + 1 != count) return IntArray(samples) { first }
        return IntArray(samples) { first + count * it / samples }
    }

    fun polygons(colors: (Array<Vec3>) -> Array<Vec3>): List<ModelPolygon> = polygons(null, colors = colors)

    fun polygons(view: Matrix?, frame: Int = idleFrame(), colors: (Array<Vec3>) -> Array<Vec3>): List<ModelPolygon> {
        val worlds = pose(frame)
        val result = ArrayList<ModelPolygon>()
        bones.forEachIndexed { index, bone ->
            val world = worlds[index]
            val transform = if (view == null) world else Transform(
                view * world.rotation, view.transformPosition(world.translation), world.visible,
            )
            if (index != 0 && transform.visible && bone.mesh != 0) {
                val mesh = meshes.getOrPut(bone.mesh) { mesh(bone.mesh) }
                val vertices = Array(mesh.vertices.size) { transform.rotation * mesh.vertices[it] + transform.translation }
                val shades = colors(Array(mesh.normals.size) { transform.rotation * mesh.normals[it] })
                for (polygon in mesh.polygons) {
                    if (result.size >= 16384) invalidDisc("Too many model polygons")
                    val color = if (polygon.lit && polygon.normals != null) {
                        Array(polygon.indices.size) { shades[polygon.normals[if (polygon.gouraud) it else 0]] }
                    } else Array(polygon.indices.size) { polygon.color }
                    result.add(ModelPolygon(Array(polygon.indices.size) { vertices[polygon.indices[it]] }, color, polygon))
                }
            }
        }
        return result
    }

    private fun pose(frame: Int): List<Transform> {
        val worlds = ArrayList<Transform>()
        bones.forEachIndexed { index, bone ->
            val zero = Vec3(0.0, 0.0, 0.0)
            val tracks = if (index == 0) null else keys.getOrPut(bone.keys) { tracks(bone.keys) }
            val pos = if (tracks == null) zero else interpolate(tracks[0], frame)
            val rot = if (tracks == null) zero else interpolate(tracks[1], frame)
            val scale = if (tracks == null) Vec3(4096.0, 4096.0, 4096.0) else interpolate(tracks[2], frame)
            val rotation = Matrix.rotation(rot).scaled(scale)
            val visible = (scale.x.toInt() or scale.y.toInt() or scale.z.toInt()) > 48
            if (index == 0) worlds.add(Transform(rotation, pos, visible)) else {
                val parent = worlds[bone.parent]
                worlds.add(Transform(parent.rotation * rotation, parent.rotation.transformPosition(pos) + parent.translation, visible))
            }
        }
        return worlds
    }

    private fun tracks(index: Int): List<Array<IntArray>> {
        val data = archive.entry(index)
        val entries = ModelArchive(data)
        if (entries.offsets.size < 3) invalidDisc("Invalid model key tracks")
        return List(3) { track ->
            val bytes = entries.entry(track)
            if (bytes.isEmpty() || bytes.size % 8 != 0 || bytes.size / 8 > 4096) invalidDisc("Invalid model key frames")
            val count = bytes.size / 8
            if (count > remainingKeys) invalidDisc("Model key budget exceeded")
            remainingKeys -= count
            val keys = Array(count) { i -> IntArray(4) { bytes.shortAt(i * 8 + it * 2) } }
            if (keys.asSequence().zipWithNext().any { (a, b) -> b[3] < a[3] }) invalidDisc("Unsorted model key frames")
            keys
        }
    }

    private fun mesh(index: Int): Mesh {
        val data = archive.entry(index)
        data.range(0, 24)
        fun vectors(offset: Int, normal: Boolean): Array<Vec3> {
            val count = data.shortAt(offset)
            if (count !in 0..4096 || (!normal && count == 0)) invalidDisc("Invalid mesh vector count")
            data.range(offset + 6, count * 6)
            if (count > remainingVectors) invalidDisc("Model vector budget exceeded")
            remainingVectors -= count
            return Array(count) { i ->
                val p = offset + 6 + i * 6; val scale = if (normal) 4096.0 else 1.0
                Vec3(data.shortAt(p) / scale, data.shortAt(p + 2) / scale, data.shortAt(p + 4) / scale)
            }
        }
        val vertices = vectors(data.intAt(0), false)
        val normals = vectors(data.intAt(4), true)
        var p = data.intAt(8)
        var quad = false; var textured = false; var lit = false; var gouraud = false; var abr = 0
        var texture: TexturePage? = null; var u = 0; var v = 0
        var color = Vec3(128.0, 128.0, 128.0)
        val polygons = ArrayList<Polygon>()
        fun byte(): Int { data.range(p, 1); return data[p++].toInt() and 255 }
        while (true) {
            val command = byte()
            if (command == 255) break
            val hi = command ushr 4; val lo = command and 15
            if (hi != 0) {
                when (hi) {
                    8 -> quad = lo != 0; 9 -> textured = lo != 0
                    10, 11 -> Unit
                    12 -> lit = lo != 0; 13 -> gouraud = lo != 0
                    14 -> { if (lo !in 0..4) invalidDisc("Invalid mesh blending mode"); abr = lo }
                    else -> invalidDisc("Unknown mesh flag")
                }
            } else when (lo) {
                1 -> {
                    val lowerU = byte(); val page = byte(); u = lowerU + (page shl 8); v = byte()
                    val cx = byte(); val cy = byte(); val tp = byte()
                    if (tp !in 0..1) invalidDisc("Unsupported model texture depth")
                    texture = TexturePage(page, cx, cy, tp)
                }
                in 2..5 -> { val r = byte(); val g = byte(); val b = byte(); if (lo == 2) color = Vec3(r.toDouble(), g.toDouble(), b.toDouble()) }
                0 -> {
                    val n = if (quad) 4 else 3
                    val indices = IntArray(n) { byte() }
                    if (indices.any { it !in vertices.indices }) invalidDisc("Invalid mesh vertex index")
                    val ni = if (lit) IntArray(n) { byte() }.also { if (it.any { x -> x !in normals.indices }) invalidDisc("Invalid mesh normal index") } else null
                    val uv = if (textured) Array(n) { Vec2(((byte() + u) and 255).toDouble(), ((byte() + v) and 255).toDouble()) } else null
                    if (textured && texture == null) invalidDisc("Mesh texture is missing")
                    if (polygons.size >= 16384) invalidDisc("Too many mesh polygons")
                    polygons.add(Polygon(indices, ni, uv, if (textured) texture else null, lit, gouraud, abr, color))
                }
                else -> invalidDisc("Unknown mesh command")
            }
        }
        return Mesh(vertices, normals, polygons)
    }
}

private fun interpolate(keys: Array<IntArray>, frame: Int): Vec3 {
    val i = keys.indexOfFirst { it[3] >= frame }
    fun vector(k: IntArray) = Vec3(k[0].toDouble(), k[1].toDouble(), k[2].toDouble())
    if (i == 0) return vector(keys[0])
    if (i < 0) return vector(keys.last())
    if (keys[i][3] == frame) return vector(keys[i])
    val prev = keys[i - 1]; val cur = keys[i]
    val t = ((frame - prev[3]) shl 12) / (cur[3] - prev[3])
    return vector(prev) + (vector(cur) - vector(prev)) * (t / 4096.0)
}
