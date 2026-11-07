package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.data.ExtractorData
import dev.cricamfe.digivice.extractor.disc.DiscFiles
import dev.cricamfe.digivice.extractor.disc.GameFileTable
import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.*

class MapRenderer(private val files: DiscFiles, private val table: GameFileTable) {
    private val packPattern = Regex("AAA/DAT/Z_STAGE/S([0-9]+)PACK\\.BIN")
    /** One stage per ordinary tile archive, for reference parity. */
    val maps: List<Int>
    /** Stage overlays with their own objects, tint or custom background. */
    val additionalMaps: List<Int>

    init {
        val stages = ExtractorData.stages.entries.sortedBy { it.key }
        for ((_, stage) in stages) stage.mapFile?.let { index ->
            if (index !in 0 until 2382 || files.atLba(table.lbaOf(index)) == null) invalidDisc("Map file missing from the disc")
        }
        val packs = (0 until 2382).mapNotNull { files.atLba(table.lbaOf(it))?.path }
            .distinct().mapNotNull { path -> packPattern.matchEntire(path)?.let { it.groupValues[1].toInt() to path } }.sortedBy { it.first }
        maps = packs.map { (pack, path) ->
            val candidates = stages.filter { (_, stage) -> stage.mapFile?.let { files.atLba(table.lbaOf(it))?.path } == path }.map { it.key }
            candidates.firstOrNull { it == pack } ?: candidates.firstOrNull() ?: invalidDisc("Map has no stage setup: $path")
        }
        additionalMaps = stages.filter { (id, stage) -> stage.mapFile != null || id in customMapStages }
            .map { it.key } - maps.toSet()
    }

    private fun file(index: Int): ByteArray? {
        if (index !in 0 until 2382) invalidDisc("Resource file index outside the game table")
        val file = files.atLba(table.lbaOf(index)) ?: return null
        if (file.size > MAX_DATA) invalidDisc("Oversized map resource")
        return files.read(file)
    }

    private fun entry(value: Int): ByteArray? = file(value ushr 16)?.let { Pack.entry(it, value and 65535) }

    fun render(mode: Int): Picture {
        val stage = ExtractorData.stage(mode) ?: throw IllegalArgumentException("Unknown stage $mode")
        val map = stage.mapFile?.let { MapFile.parse(file(it) ?: invalidDisc("Map file missing")) }
        val canvas = when {
            map != null -> MapCanvas(map.width, map.height)
            mode == 415 -> MapCanvas(512, 768)
            mode == 815 -> {
                val floor = stage.floor0?.let(::entry) ?: invalidDisc("Gunslinger floor missing")
                val grid = floor.intAt(0)
                val dimensions = floor.wordAt(grid)
                val columns = dimensions and 255
                val rows = dimensions ushr 8
                if (columns !in 1..32 || rows !in 1..32 || columns * rows * 128 * 128 > MAX_PIXELS) {
                    invalidDisc("Invalid Gunslinger floor size")
                }
                MapCanvas(columns * 128, rows * 128)
            }
            else -> throw IllegalArgumentException("Stage $mode has no map")
        }
        if (map == null) drawCustomBackground(mode, canvas)
        val vram = Vram()
        stage.imageEntry?.takeIf { it != 0 }?.let { value -> vram.loadArchive(entry(value) ?: invalidDisc("Image archive entry missing")) }
        stage.imageFile?.takeIf { it != 0 }?.let { vram.loadArchive(file(it) ?: invalidDisc("Stage image file missing")) }
        val sheet = stage.sheetEntry?.takeIf { it != 0 }?.let { StageSprites(entry(it) ?: invalidDisc("Stage sprite sheet missing")) }
        val color = stage.color
        val tint = if (color != null && color[3] != 0) color else null
        data class Command(val depth: Int, val seq: Int, val draw: () -> Unit)
        val commands = ArrayList<Command>()
        var texturePixels = 0L
        var drawPixels = 0L
        fun budget(pixels: Long) {
            drawPixels += pixels
            if (drawPixels > MAX_PIXELS * 8L) invalidDisc("Map drawing budget exceeded")
        }
        fun add(depth: Int, draw: () -> Unit) {
            if (commands.size >= 4096) invalidDisc("Too many map primitives")
            commands.add(Command(depth, commands.size, draw))
        }
        val depths = intArrayOf(7, 3, 11)
        for ((tile, packed) in map?.entries.orEmpty().withIndex()) {
            val bytes = Rlen.unpack(packed)
            if (bytes.size < 8) continue
            val header = bytes.intAt(0)
            if (header < 16 || header > bytes.size) invalidDisc("Invalid map tile header")
            val tim = Tim.parse(bytes.copyOfRange(header, bytes.size))
            texturePixels += tim.width.toLong() * tim.height
            if (texturePixels > MAX_PIXELS) invalidDisc("Map texture budget exceeded")
            val pixels = tim.pixels16()
            var offset = 4
            for (layer in 0..2) {
                val count = bytes.intAt(offset)
                offset += 4
                if (count > 4096 || count.toLong() * 12 > header - offset) invalidDisc("Invalid tile layer size")
                repeat(count) {
                    val sx = bytes.shortAt(offset)
                    val sy = bytes.shortAt(offset + 2)
                    val u = bytes.shortAt(offset + 4)
                    val v = bytes.shortAt(offset + 6)
                    val w = bytes.shortAt(offset + 8)
                    val h = bytes.shortAt(offset + 10)
                    offset += 12
                    if (u < 0 || v < 0 || w < 0 || h < 0) invalidDisc("Invalid tile rectangle")
                    val width = minOf(w, (tim.width - u).coerceAtLeast(0))
                    val height = minOf(h, (tim.height - v).coerceAtLeast(0))
                    val x = (tile % (canvas.width / 128)) * 128 + sx
                    val y = (tile / (canvas.width / 128)) * 128 + sy
                    val tileTint = if (layer == 2) color ?: listOf(0, 0, 0, 0) else tint
                    budget(width.toLong() * height)
                    add(depths[layer]) { canvas.blit(pixels, tim.width, x, y, u, v, width, height, tileTint, -1) }
                }
            }
        }
        if (sheet != null) {
            val objects = stage.objects.filter { it.visible != 0 && it.anim != 255 }
            val ordered = objects.filter { it.sortY == 0 } + objects.filter { it.sortY != 0 }.sortedByDescending { it.sortY }
            for (obj in ordered) add(obj.depth) {
                val frame = sheet.frame(obj.frame)
                if (frame != null) for (placement in frame.parts) {
                    val part = sheet.parts[placement.part]
                    budget(part.width.toLong() * part.height)
                    val pixels = vram.part(part, frame.row, obj.clutRow)
                    canvas.blit(pixels, part.width, obj.x + placement.x, obj.y + placement.y, 0, 0, part.width, part.height, tint, frame.abr)
                }
            }
        }
        commands.sortWith(compareByDescending<Command> { it.depth }.thenByDescending { it.seq })
        commands.forEach { it.draw() }
        return canvas.picture()
    }

    private fun drawCustomBackground(mode: Int, canvas: MapCanvas) {
        val vram = Vram()
        fun draw(target: MapCanvas, fileIndex: Int, sheetKey: Int, x: Int, y: Int, imageY: Int, paletteY: Int) {
            val archive = file(fileIndex) ?: invalidDisc("Stage background image missing")
            vram.loadArchive(archive, 0x280, imageY, paletteY)
            val sheet = StageSprites(entry(sheetKey) ?: invalidDisc("Stage background sheet missing"))
            val frame = sheet.frame(0) ?: invalidDisc("Stage background frame missing")
            for (placement in frame.parts) {
                val part = sheet.parts[placement.part]
                val pixels = vram.part(part, frame.row, 0, 0x280, imageY, paletteY)
                target.blit(pixels, part.width, x + placement.x, y + placement.y, 0, 0, part.width, part.height, null, frame.abr)
            }
        }
        if (mode == 415) {
            // The gondola ride has no tile grid. Use the opening phase of the scrolling backdrop.
            draw(canvas, 0x769, 0x75E0002, 0, 0x100, 0, 0xf0)
            draw(canvas, 0x76a, 0x75E0004, 0x100, 0x100, 0x100, 0xf8)
        } else {
            // WSTAG815 draws two sheets behind the walkable floor, at two thirds of camera scroll.
            val background = MapCanvas(640, 512)
            draw(background, 0x775, 0x7760002, 0, 0, 0, 0xf0)
            draw(background, 0x898, 0x7760003, 0, 0x100, 0x100, 0x1f0)
            canvas.projectParallax(background)
        }
    }

    private companion object {
        val customMapStages = setOf(415, 815)
    }
}

private class MapCanvas(val width: Int, private val height: Int) {
    private val pixels = IntArray(width * height)
    fun blit(source: IntArray, pitch: Int, x: Int, y: Int, u: Int, v: Int, w: Int, h: Int, tint: List<Int>?, abr: Int) {
        val x0 = maxOf(0, x)
        val y0 = maxOf(0, y)
        val x1 = minOf(width, x + w)
        val y1 = minOf(height, y + h)
        for (dy in y0 until y1) for (dx in x0 until x1) {
            val color = source[(v + dy - y) * pitch + u + dx - x]
            if (color != 0) {
                val offset = dy * width + dx
                pixels[offset] = blendColor(pixels[offset], color, tint, abr)
            }
        }
    }

    /** Sample each world pixel as seen at the centre of the game's 320 × 240 viewport. */
    fun projectParallax(background: MapCanvas) {
        if ((width + 319) / 3 >= background.width || (height + 239) / 3 >= background.height) {
            invalidDisc("Gunslinger floor exceeds the stage background")
        }
        for (y in 0 until height) {
            val row = (y + 240) / 3 * background.width
            for (x in 0 until width) pixels[y * width + x] = background.pixels[row + (x + 320) / 3]
        }
    }

    fun picture(): Picture {
        pixels.indices.forEach { pixels[it] = colorArgb(pixels[it]) }
        return Picture(width, height, pixels)
    }
}

internal fun blendColor(back: Int, front: Int, tint: List<Int>?, abr: Int): Int {
    if (front == 0) return back
    var color = 0
    for (channel in 0..2) {
        val shift = channel * 5
        val source = (front shr shift) and 31
        val c = if (tint == null) source else (source * tint[channel] / 128).coerceIn(0, 31)
        val b = (back shr shift) and 31
        val value = if (abr < 0 || front and 0x8000 == 0) c else when (abr) {
            0 -> (b + c) / 2
            1 -> b + c
            2 -> b - c
            else -> b + c / 4
        }
        color = color or (value.coerceIn(0, 31) shl shift)
    }
    return color
}
