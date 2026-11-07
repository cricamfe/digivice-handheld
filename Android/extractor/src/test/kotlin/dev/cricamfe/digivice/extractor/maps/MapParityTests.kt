package dev.cricamfe.digivice.extractor.maps

import dev.cricamfe.digivice.extractor.data.ExtractorData
import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.image.writePng
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import javax.imageio.ImageIO

@EnabledIfEnvironmentVariable(named = "DW3_ISO", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DW3_REFERENCE", matches = ".+")
class MapParityTests {
    @Test fun `S331PACK and S206PACK equal the reference pixel by pixel`() = withRenderer { renderer, files, table ->
        for (mode in listOf(331, 206)) compare(renderer, files, table, mode)
    }

    @Test fun `all 237 maps equal the reference pixel by pixel`() = withRenderer { renderer, files, table ->
        assertEquals(237, renderer.maps.size)
        var pixels = 0L
        val start = System.nanoTime()
        for (mode in renderer.maps) pixels += compare(renderer, files, table, mode)
        assertEquals(197738496L, pixels)
        println("237 map parity: $pixels pixels in ${(System.nanoTime() - start) / 1_000_000} ms")
    }

    @Test fun `custom stages render their own sprite scenery and retain the stage dimensions`() = withRenderer { renderer, _, _ ->
        assertTrue(renderer.additionalMaps.containsAll(listOf(415, 815)))
        val crevice = renderer.render(415)
        val gunslinger = renderer.render(815)
        assertEquals(512 to 768, crevice.width to crevice.height)
        assertEquals(1280 to 1024, gunslinger.width to gunslinger.height)
        for (image in listOf(crevice, gunslinger)) {
            assertTrue(image.argb.count { it != 0xff000000.toInt() } > image.argb.size / 2)
        }
    }

    @Test fun `chapter overlays with different stage art have independent assets`() = withRenderer { renderer, _, _ ->
        assertTrue((renderer.maps + renderer.additionalMaps).containsAll(listOf(927, 934)))
        for ((main, extra) in listOf(233 to 927, 261 to 934, 220 to 924)) {
            assertFalse(renderer.render(main).argb.contentEquals(renderer.render(extra).argb), "$main and $extra")
        }
    }

    @Test fun `all alternate overlays render independently of their canonical tile archives`() = withRenderer { renderer, _, _ ->
        assertEquals(293, renderer.maps.size + renderer.additionalMaps.size)
        for (stage in renderer.additionalMaps) assertDoesNotThrow({ renderer.render(stage) }, "stage $stage")
    }

    private fun compare(renderer: MapRenderer, files: DiscFiles, table: GameFileTable, mode: Int): Long {
        val path = files.atLba(table.lbaOf(ExtractorData.stage(mode)!!.mapFile!!))!!.path
        val name = path.substringAfterLast('/').substringBeforeLast('.')
        val reference = ImageIO.read(File(System.getenv("DW3_REFERENCE"), "maps/$name.png"))
        val actual = renderer.render(mode)
        assertEquals(reference.width, actual.width, name)
        assertEquals(reference.height, actual.height, name)
        val expected = reference.getRGB(0, 0, actual.width, actual.height, null, 0, actual.width)
        var different = 0
        val diff = IntArray(expected.size) { i -> if (expected[i] == actual.argb[i]) 0xff000000.toInt() else { different++; 0xffff00ff.toInt() } }
        if (different != 0) {
            val out = File("build/parity/$name-diff.png").apply { parentFile.mkdirs() }
            out.outputStream().use { Picture(actual.width, actual.height, diff).writePng(it) }
        }
        assertEquals(0, different, "$name (mode $mode)")
        return expected.size.toLong()
    }

    private fun withRenderer(block: (MapRenderer, DiscFiles, GameFileTable) -> Unit) {
        openDisc(FileByteSource(File(System.getenv("DW3_ISO"))))!!.use { disc ->
            val files = DiscFiles(disc)
            val table = GameFileTable(files)
            block(MapRenderer(files, table), files, table)
        }
    }
}
