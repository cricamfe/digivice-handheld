package dev.cricamfe.digivice.extractor.disc

class GameFileTable(files: DiscFiles) {
    private val sectors: IntArray

    init {
        val exe = files.byPath("SLES_039.36") ?: invalidDisc("Game executable is missing")
        if (exe.size < 0x3576c + 2382 * 4 || exe.size > 2 * 1024 * 1024) invalidDisc("Invalid executable size")
        val bytes = files.read(exe)
        sectors = IntArray(2382) { bytes.discInt(0x3576c + it * 4) }
    }

    fun lbaOf(index: Int): Int = sectors[index]
}
