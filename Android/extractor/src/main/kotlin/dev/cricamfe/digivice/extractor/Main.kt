package dev.cricamfe.digivice.extractor

import dev.cricamfe.digivice.extractor.disc.*
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("Usage: extractor <game.iso|game.bin> <output-directory>")
        exitProcess(2)
    }
    val start = System.nanoTime()
    try {
        val input = File(args[0])
        val output = File(args[1])
        validateExtractionPaths(input, output)
        FileByteSource(input).use { source ->
            when (checkGame(source)) {
                GameCheck.Valid -> Unit
                GameCheck.NotADisc -> throw IOException("Unsupported disc format; provide an ISO or single-track MODE2/2352 BIN")
                GameCheck.WrongGame -> throw IOException("Expected an unmodified European Digimon World 2003 (SLES-03936)")
                GameCheck.Truncated -> throw IOException("The disc image is incomplete")
            }
            val disc = openDisc(source) ?: throw IOException("Cannot open validated disc")
            disc.use {
                val manifest = Extractor(it, PngSink(output)).run(progress = { piece, done, total ->
                    if (done == 0 || done == total || done % 100 == 0) println("$piece: $done/$total")
                })
                val seconds = (System.nanoTime() - start) / 1_000_000_000.0
                println("Complete: ${manifest.assets.size} asset keys in ${"%.2f".format(java.util.Locale.ROOT, seconds)} s")
            }
        }
    } catch (error: IOException) {
        System.err.println("Extraction failed: ${error.message}")
        exitProcess(1)
    } catch (error: IllegalArgumentException) {
        System.err.println("Extraction failed: ${error.message}")
        exitProcess(1)
    }
}

/** An extracted filename must never replace the user's source image, regardless of its extension. */
internal fun validateExtractionPaths(input: File, output: File) {
    require(!input.canonicalFile.toPath().startsWith(output.canonicalFile.toPath())) {
        "The source disc must be outside the output directory"
    }
}
