package dev.cricamfe.digivice.core.memory

import dev.cricamfe.digivice.core.memory.repositories.AddressesRepository
import org.junit.jupiter.api.Assumptions
import java.io.File
import java.io.FileNotFoundException

/**
 * Filesystem-backed `readText` / `listFiles` pair, with the contract [AddressesRepository] expects:
 * both throw [FileNotFoundException] when the file or folder does not exist.
 */
class DirectoryDefinitions(private val root: File) {
    fun readText(relativePath: String): String {
        val file = File(root, relativePath)
        if (!file.isFile) throw FileNotFoundException("Database file not found: $file")
        return file.readText()
    }

    fun listFiles(relativeFolder: String): List<String> {
        val folder = File(root, relativeFolder)
        if (!folder.isDirectory) throw FileNotFoundException("Quest addresses folder not found: $folder")
        return folder.listFiles { file -> file.isFile }!!.map { it.name }
    }

    fun repository() = AddressesRepository(::readText, ::listFiles)
}

/** Resolves the repo-level resources the tests read in place (never copied into this module). */
object TestEnvironment {
    val repoRoot: File = File(
        System.getProperty("digivice.repoRoot") ?: error("System property digivice.repoRoot is not set"),
    )

    val definitions = DirectoryDefinitions(File(repoRoot, "Backend/Memory/Definitions"))

    fun createAddressesRepository(): AddressesRepository = definitions.repository()

    /** RAM dumps of the game are not in the public repository; a test that reads one is skipped, saying why, without it. */
    const val SNAPSHOTS = "Tools/MemoryScanner/Snapshots"

    private fun skipWithout(file: File, present: Boolean) = Assumptions.assumeTrue(present) {
        "Skipped: ${file.relativeTo(repoRoot)} is not here. RAM dumps of the game are kept out of the repository; " +
            "put your own dumps there to run this test."
    }

    /** A RAM dump under the repository, or the test is skipped when it is absent. */
    fun ramDump(relativePath: String): File = File(repoRoot, relativePath).also { skipWithout(it, it.isFile) }

    /** Loads a 2 MiB RAM snapshot from `Tools/MemoryScanner/Snapshots` by file name (without `.bin`). */
    fun snapshot(name: String): SnapshotAccessor = SnapshotAccessor(ramDump("$SNAPSHOTS/$name.bin").readBytes())

    /** The names of the snapshots; the test is skipped when there are none (the folder is absent, or empty). */
    fun snapshotNames(): List<String> {
        val folder = File(repoRoot, SNAPSHOTS)
        val names = folder.listFiles { f -> f.extension == "bin" }.orEmpty().map { it.nameWithoutExtension }.sorted()
        skipWithout(folder, names.isNotEmpty())
        return names
    }
}
