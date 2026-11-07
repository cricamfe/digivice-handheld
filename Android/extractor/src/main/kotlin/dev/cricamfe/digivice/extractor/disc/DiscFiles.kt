package dev.cricamfe.digivice.extractor.disc

import java.util.Locale
import java.io.EOFException

data class DiscFile(val path: String, val lba: Int, val size: Int)

class DiscFiles(private val disc: Disc) {
    private val paths = LinkedHashMap<String, DiscFile>()
    private val extents = HashMap<Int, DiscFile>()

    init {
        val pvd = disc.read(16, 2048)
        if (pvd[0].toInt() != 1 || pvd.copyOfRange(1, 6).decodeToString() != "CD001" || pvd[6].toInt() != 1 || pvd.u16(128) != 2048) {
            invalidDisc("Expected an ISO9660 primary volume descriptor")
        }
        val volumeSectors = pvd.discInt(80)
        if (volumeSectors <= 16) invalidDisc("Invalid volume size")
        disc.read(volumeSectors - 1, 1)
        fun checkExtent(lba: Int, size: Int) {
            if (lba.toLong() + (size.toLong() + 2047) / 2048 > volumeSectors) throw EOFException("Extent exceeds the volume")
        }
        val pathSize = pvd.discInt(132)
        if (pathSize !in 10..131072) invalidDisc("Invalid path table size")
        val root = directoryRecord(pvd, 156)
        if (!root.directory || root.size !in 34..524288) invalidDisc("Invalid root directory")
        checkExtent(root.lba, root.size)
        checkExtent(pvd.discInt(140), pathSize)
        val table = disc.read(pvd.discInt(140), pathSize)
        val directories = ArrayList<Pair<String, Int>>()
        val directoryPaths = HashSet<String>()
        val directoryExtents = HashSet<Int>()
        var offset = 0
        while (offset < table.size) {
            if (table.size - offset < 8) invalidDisc("Short path table entry")
            val length = table[offset].toInt() and 255
            val entrySize = 8 + length + (length and 1)
            if (length == 0 || entrySize > table.size - offset || directories.size >= 4096) invalidDisc("Invalid path table entry")
            val lba = table.discInt(offset + 2)
            val parent = table.u16(offset + 6)
            val name = table.copyOfRange(offset + 8, offset + 8 + length)
            val path = if (directories.isEmpty()) {
                if (!name.contentEquals(byteArrayOf(0)) || parent != 1 || lba != root.lba) invalidDisc("Invalid path table root")
                ""
            } else {
                if (parent !in 1..directories.size) invalidDisc("Invalid or cyclic path table parent")
                val component = fileName(name)
                (directories[parent - 1].first + "/" + component).trimStart('/')
            }
            if (path.length > 255 || !directoryPaths.add(path) || !directoryExtents.add(lba)) invalidDisc("Invalid or repeated directory path")
            checkExtent(lba, 2048)
            directories.add(path to lba)
            offset += entrySize
        }
        var recordCount = 0
        for ((path, lba) in directories) {
            val first = disc.read(lba, 2048)
            val self = directoryRecord(first, 0)
            if (!self.directory || self.lba != lba || self.size !in 34..524288) invalidDisc("Invalid directory extent")
            checkExtent(lba, self.size)
            val data = disc.read(lba, self.size)
            offset = 0
            while (offset < data.size) {
                if (data[offset] == 0.toByte()) {
                    offset = (offset / 2048 + 1) * 2048
                    continue
                }
                if (++recordCount > 32768) invalidDisc("Too many directory entries")
                val record = directoryRecord(data, offset)
                offset += record.length
                if (record.directory) continue
                checkExtent(record.lba, record.size)
                disc.read(record.lba, if (record.size == 0) 0 else 1)
                if (record.size > 0) disc.read((record.lba.toLong() + (record.size - 1L) / 2048).toInt(), 1)
                val file = DiscFile((path + "/" + fileName(record.name)).trimStart('/'), record.lba, record.size)
                if (file.path.length > 255) invalidDisc("File path exceeds ISO9660 limits")
                if (paths.put(file.path, file) != null) invalidDisc("Duplicate file path")
                extents.putIfAbsent(file.lba, file)
            }
        }
    }

    fun byPath(path: String): DiscFile? = paths[path.trimStart('/').substringBefore(';').uppercase(Locale.ROOT)]
    fun atLba(lba: Int): DiscFile? = extents[lba]
    fun read(file: DiscFile): ByteArray = disc.read(file.lba, file.size)

    private data class Record(val length: Int, val lba: Int, val size: Int, val directory: Boolean, val name: ByteArray)
    private fun directoryRecord(bytes: ByteArray, offset: Int): Record {
        val length = bytes[offset].toInt() and 255
        if (length < 34 || length > bytes.size - offset || length > 2048 - offset % 2048) invalidDisc("Invalid directory record length")
        val nameLength = bytes[offset + 32].toInt() and 255
        if (nameLength == 0 || nameLength > length - 33 || bytes[offset + 1] != 0.toByte() || bytes[offset + 25].toInt() and 0x80 != 0) {
            invalidDisc("Unsupported directory record")
        }
        return Record(length, bytes.discInt(offset + 2), bytes.discInt(offset + 10), bytes[offset + 25].toInt() and 2 != 0,
            bytes.copyOfRange(offset + 33, offset + 33 + nameLength))
    }

    private fun fileName(bytes: ByteArray): String {
        if (bytes.any { (it.toInt() and 255) !in 32..126 || it == '/'.code.toByte() || it == '\\'.code.toByte() }) invalidDisc("Invalid file name")
        return bytes.decodeToString().substringBefore(';').uppercase(Locale.ROOT).also {
            if (it.isEmpty() || it == "." || it == "..") invalidDisc("Invalid file name")
        }
    }
}
