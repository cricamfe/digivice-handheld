package dev.cricamfe.digivice.core.memory.repositories

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.InputStream

class AssetDefinitionsTests {
    /** Behaves like `AssetManager`: `open` fails on a missing file, `list` answers an empty array for a missing folder. */
    private class FakeAssets(files: Set<String>) {
        private val files = files.toSortedSet()

        fun open(path: String): InputStream =
            if (path in files) "content of $path".byteInputStream() else throw FileNotFoundException(path)

        fun list(path: String): Array<String> =
            files.filter { it.startsWith("$path/") }.map { it.removePrefix("$path/").substringBefore('/') }.distinct().toTypedArray()
    }

    private val assets = FakeAssets(
        setOf(
            "definitions/PlayerAddresses.json",
            "definitions/Quests/SideQuests/a.json",
            "definitions/Quests/SideQuests/b.json",
        ),
    )
    private val definitions = AssetDefinitions(assets::open, assets::list)

    @Test
    fun `reads a file under the definitions folder`() {
        assertEquals("content of definitions/PlayerAddresses.json", definitions.readText("PlayerAddresses.json"))
    }

    @Test
    fun `a missing file is FileNotFoundException`() {
        assertThrows(FileNotFoundException::class.java) { definitions.readText("Nope.json") }
    }

    @Test
    fun `lists the files of a folder`() {
        assertEquals(listOf("a.json", "b.json"), definitions.listFiles("Quests/SideQuests").sorted())
    }

    @Test
    fun `a missing folder is FileNotFoundException, not an empty list`() {
        // AssetManager.list answers an empty array here; letting that through would drop the quests without a word.
        assertEquals(0, assets.list("definitions/Quests/Missing").size)

        assertThrows(FileNotFoundException::class.java) { definitions.listFiles("Quests/Missing") }
    }

    @Test
    fun `a null listing is a missing folder too`() {
        val nullable = AssetDefinitions(assets::open, { null })

        assertThrows(FileNotFoundException::class.java) { nullable.listFiles("Quests/SideQuests") }
    }
}
