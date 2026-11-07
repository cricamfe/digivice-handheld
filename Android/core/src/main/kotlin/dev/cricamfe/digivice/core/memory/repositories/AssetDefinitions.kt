package dev.cricamfe.digivice.core.memory.repositories

import java.io.FileNotFoundException
import java.io.InputStream

/**
 * The `readText` / `listFiles` pair that [AddressesRepository] expects, over an asset store such as Android's
 * `AssetManager`, whose definitions live under [root]. The store is reached through two functions so the core does not
 * depend on Android: `AssetManager::open` and `AssetManager::list`.
 *
 * `AssetManager.list` answers an empty array for a folder that does not exist, where the repository's contract is a
 * [FileNotFoundException]: without the translation here, a missing quests folder would load as "no quests" and the
 * journal would silently lose them. A folder that is there always has files in it, because the build does not package
 * an empty one.
 */
class AssetDefinitions(
    private val openAsset: (path: String) -> InputStream,
    private val listAssets: (path: String) -> Array<String>?,
    private val root: String = "definitions",
) {
    /** Throws [FileNotFoundException] if there is no such file (the store's `open` does already). */
    fun readText(relativePath: String): String =
        openAsset("$root/$relativePath").use { it.readBytes().toString(Charsets.UTF_8) }

    /** Throws [FileNotFoundException] if there is no such folder, or nothing in it. */
    fun listFiles(relativeFolder: String): List<String> {
        val names = listAssets("$root/$relativeFolder")
        if (names.isNullOrEmpty()) throw FileNotFoundException("Definitions folder not found: $root/$relativeFolder")
        return names.toList()
    }
}
