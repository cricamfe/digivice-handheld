package dev.cricamfe.digivice.extractor

import dev.cricamfe.digivice.extractor.image.Picture
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.imageio.ImageIO

class ExtractorTests {
    @TempDir lateinit var directory: File
    private val pixel = Picture(1, 1, intArrayOf(0xff123456.toInt()))

    @Test fun `a run that fails halfway keeps what it wrote and the next run only does the rest`() {
        val sink = RecordingSink()
        val renders = mutableListOf<Int>()
        var fail = true
        val work = listOf(ExtractionPiece(Piece.Cards) {
            (0..2).map { id -> ExtractionAsset.Image("cards/$id") {
                renders.add(id)
                if (id == 1 && fail) throw IOException("Injected interruption")
                pixel
            } }
        })
        assertThrows(IOException::class.java) { Extractor(sink, work).run() }
        assertEquals(listOf("cards/0.png"), sink.outputs.toList())
        assertNull(sink.manifest)
        fail = false
        val manifest = Extractor(sink, work).run()
        assertEquals(listOf(0, 1, 1, 2), renders)
        assertEquals(listOf("cards/0", "cards/1", "cards/2"), sink.writes)
        assertEquals(setOf("cards/0", "cards/1", "cards/2"), manifest.assets.keys)
    }

    @Test fun `nothing is committed until all pieces finished`() {
        val sink = RecordingSink()
        val work = listOf(
            ExtractionPiece(Piece.Text) { listOf(ExtractionAsset.Text("texts/en-US") { "{}" }) },
            ExtractionPiece(Piece.Cards) { listOf(ExtractionAsset.Image("cards/1") { pixel }) },
        )
        val callbacks = mutableListOf<Triple<Piece, Int, Int>>()
        Extractor(sink, work).run(progress = { piece, done, total ->
            assertNull(sink.manifest)
            callbacks.add(Triple(piece, done, total))
        })
        assertEquals(listOf("texts/en-US", "cards/1"), sink.writes)
        assertNotNull(sink.manifest)
        assertEquals(setOf(Piece.Text, Piece.Cards), callbacks.map { it.first }.toSet())
    }

    @Test fun `pause is called before each item and progress reaches each total including resumed items`() {
        val sink = RecordingSink().apply { outputs.add("cards/0.png") }
        val events = mutableListOf<String>()
        val work = listOf(ExtractionPiece(Piece.Cards) {
            (0..2).map { id -> ExtractionAsset.Image("cards/$id") { events.add("render$id"); pixel } }
        }, ExtractionPiece(Piece.Text) { emptyList() })
        val progress = mutableListOf<Triple<Piece, Int, Int>>()
        val extractor = Extractor(sink, work)
        assertEquals(listOf(Piece.Cards, Piece.Text), extractor.pieces)
        extractor.run(pause = { events.add("pause") }, progress = { piece, done, total -> progress.add(Triple(piece, done, total)) })
        assertEquals(listOf("pause", "pause", "render1", "pause", "render2"), events)
        assertEquals(listOf(Triple(Piece.Cards, 0, 3), Triple(Piece.Cards, 1, 3), Triple(Piece.Cards, 2, 3),
            Triple(Piece.Cards, 3, 3), Triple(Piece.Text, 0, 0)), progress)
    }

    @Test fun `manifest uses actual sink extensions and aliases without duplicate images`() {
        val sink = RecordingSink("webp")
        val work = listOf(ExtractionPiece(Piece.World) { listOf(ExtractionAsset.Image("portraits/0") { pixel }) })
        val result = Extractor(sink, work, mapOf("sprites/0" to "portraits/0")).run()
        assertEquals(FORMAT_VERSION, result.version)
        assertEquals(mapOf("portraits/0" to "portraits/0.webp", "sprites/0" to "portraits/0.webp"), result.assets)
        assertEquals(listOf("portraits/0"), sink.writes)
    }

    @Test fun `missing alias targets and duplicate keys never commit a manifest`() {
        val sink = RecordingSink()
        val asset = ExtractionAsset.Image("cards/0") { pixel }
        val one = ExtractionPiece(Piece.Cards) { listOf(asset) }
        assertThrows(IllegalArgumentException::class.java) { Extractor(sink, listOf(one), mapOf("cards/alias" to "cards/missing")).run() }
        assertNull(sink.manifest)
        assertThrows(IllegalArgumentException::class.java) { Extractor(sink, listOf(one, ExtractionPiece(Piece.Sprites) { listOf(asset) })).run() }
        assertNull(sink.manifest)
    }

    @Test fun `a picture half written is not taken as done and a successful write is a valid PNG`() {
        val sink = PngSink(directory)
        File(directory, "cards").mkdirs()
        File(directory, "cards/0.png.tmp").writeBytes(byteArrayOf(1, 2))
        File(directory, "cards/0.png").createNewFile()
        assertFalse(sink.has("cards/0.png"))
        assertFalse(sink.has("cards/0.png.tmp"))
        sink.picture("cards/0", pixel)
        assertTrue(sink.has("cards/0.png"))
        assertEquals(pixel.argb[0], ImageIO.read(File(directory, "cards/0.png")).getRGB(0, 0))
        sink.text("texts/es-ES", "{\"prueba\":\"á\"}")
        val manifest = Manifest(FORMAT_VERSION, mapOf("cards/0" to "cards/0.png", "texts/es-ES" to "texts/es-ES.json"))
        sink.commit(manifest)
        assertEquals(manifest, Json.decodeFromString<Manifest>(File(directory, "manifest.json").readText()))
        assertEquals("{\"prueba\":\"á\"}", File(directory, "texts/es-ES.json").readText())
    }

    @Test fun `failed atomic promotion preserves the previous manifest and removes its temporary file`() {
        val sink = PngSink(directory)
        sink.text("texts/en-US", "{}")
        val previous = Manifest(FORMAT_VERSION, mapOf("texts/en-US" to "texts/en-US.json"))
        sink.commit(previous)
        File(directory, "cards/blocked.png").mkdirs()
        assertThrows(IOException::class.java) { sink.picture("cards/blocked", pixel) }
        assertFalse(sink.has("cards/blocked.png"))
        assertEquals(previous, Json.decodeFromString<Manifest>(File(directory, "manifest.json").readText()))
        assertTrue(File(directory, "cards").listFiles()!!.none { it.isFile })
        assertThrows(IllegalArgumentException::class.java) {
            sink.commit(Manifest(FORMAT_VERSION, mapOf("cards/missing" to "cards/missing.png")))
        }
        assertEquals(previous, Json.decodeFromString<Manifest>(File(directory, "manifest.json").readText()))
    }

    @Test fun `sink rejects paths and symlinks that escape the output directory`() {
        val sink = PngSink(directory)
        for (key in listOf("../outside", "/absolute", "a/../../outside", "a\\outside", "a//b", ".", "a/..")) {
            assertThrows(IllegalArgumentException::class.java, { sink.picture(key, pixel) }, key)
        }
        val outside = Files.createTempDirectory("extractor-outside").toFile()
        try {
            Files.createSymbolicLink(File(directory, "link").toPath(), outside.toPath())
            assertThrows(IllegalArgumentException::class.java) { sink.text("link/escape", "{}") }
            assertTrue(outside.listFiles()!!.isEmpty())
        } finally { outside.deleteRecursively() }
    }

    @Test fun `CLI rejects a source inside the canonical output tree before creating files`() {
        val output = File(directory, "output")
        for (input in listOf(output, File(output, "manifest.json"), File(output, "maps/200.png"))) {
            assertThrows(IllegalArgumentException::class.java) { validateExtractionPaths(input, output) }
        }
        validateExtractionPaths(File(directory, "output-original.iso"), output)
        assertFalse(output.exists())
        output.mkdir()
        val link = File(directory, "alias")
        Files.createSymbolicLink(link.toPath(), output.toPath())
        assertThrows(IllegalArgumentException::class.java) {
            validateExtractionPaths(File(link, "disc.bin"), output)
        }
    }

    private class RecordingSink(override val pictureExtension: String = "png") : AssetSink {
        val outputs = linkedSetOf<String>()
        val writes = mutableListOf<String>()
        var manifest: Manifest? = null
        override fun has(path: String) = path in outputs
        override fun picture(path: String, picture: Picture) { writes.add(path); outputs.add("$path.$pictureExtension") }
        override fun text(path: String, content: String) { writes.add(path); outputs.add("$path.json") }
        override fun commit(manifest: Manifest) { this.manifest = manifest }
    }
}
