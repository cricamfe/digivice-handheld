package dev.cricamfe.digivice.core.retroarch

import dev.cricamfe.digivice.core.memory.MemoryReadException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.PortUnreachableException
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/** What `GET_STATUS` says; [title] is the text after the state (`playstation,<content name>`), opaque to the core. */
sealed interface RetroArchStatus {
    /** No usable answer: nothing arrived in time, or it was not a status line. */
    data object Unreachable : RetroArchStatus

    /** RetroArch is open with no content loaded (`CONTENTLESS`). */
    data object Contentless : RetroArchStatus

    data class Paused(val title: String) : RetroArchStatus

    data class Playing(val title: String) : RetroArchStatus
}

/** RetroArch answered the read of [address] with an error, or with something that is not the bytes asked for. */
class MemoryReadRefused(address: Long, val reason: String) :
    MemoryReadException(address, "RetroArch refused to read 0x${address.toString(16).uppercase()}: $reason")

/** No usable reply to the read of [address] arrived, even after the retry. It says nothing about the memory. */
class MemoryReadLost(address: Long) :
    MemoryReadException(address, "No reply from RetroArch to the read of 0x${address.toString(16).uppercase()}.")

/**
 * A client of RetroArch's UDP network commands. It sends exactly three commands: `VERSION`, `GET_STATUS` and
 * `READ_CORE_MEMORY`. Nothing here can write to the emulator.
 *
 * RetroArch handles one command per frame and a read is limited to [MAX_READ_BYTES]. Each request goes out from its own
 * socket, closed as soon as it is answered or times out, so a late reply to an earlier request lands on a port nobody
 * listens to and can never be taken as the answer to a later one. A read reply must also carry the address asked for.
 *
 * A request with no reply in [timeoutMs] is lost; so is one the network refuses (a port with nothing listening raises
 * `PortUnreachableException` on localhost). [version] and [read] retry once before giving up. One thread at a time may
 * ask; [close] may be called from any thread and interrupts a request that is waiting.
 */
class RetroArchClient(
    private val host: String = "127.0.0.1",
    private val port: Int = 55355,
    private val timeoutMs: Int = 500,
) {
    private val lock = Any()
    private val sockets: MutableSet<DatagramSocket> = ConcurrentHashMap.newKeySet()

    /**
     * What the last request found of the port: true with nothing listening on it, false with something there, answering or
     * not (RetroArch in the background keeps it and does not answer). Null until a request could tell.
     */
    @Volatile
    var portState: Boolean? = null
        private set
    private var closed = false

    @Volatile private var resolved: InetAddress? = null

    private val buffer = ByteArray(MAX_DATAGRAM)

    /** Where the commands go, for messages. */
    val endpoint: String get() = "$host:$port"

    /** Sockets with a request in flight; zero whenever the client is idle or closed. */
    val openSockets: Int get() = sockets.size

    /** RetroArch's version (`1.22.2`), or null if nothing answered. */
    fun version(): String? = exchange("VERSION", ::isVersion) ?: exchange("VERSION", ::isVersion)

    fun status(): RetroArchStatus {
        val reply = exchange("GET_STATUS") { it.startsWith("GET_STATUS") } ?: return RetroArchStatus.Unreachable
        val body = reply.removePrefix("GET_STATUS").trim()
        val title = body.substringAfter(' ', "").trim()
        return when (body.substringBefore(' ')) {
            "PLAYING" -> RetroArchStatus.Playing(title)
            "PAUSED" -> RetroArchStatus.Paused(title)
            "CONTENTLESS" -> RetroArchStatus.Contentless
            else -> RetroArchStatus.Unreachable
        }
    }

    /**
     * The [size] bytes at [address] of the emulated RAM.
     *
     * @throws MemoryReadRefused RetroArch answered with an error, a truncated reply, or fewer bytes than asked.
     * @throws MemoryReadLost nothing usable arrived, twice.
     */
    fun read(address: Long, size: Int): ByteArray {
        require(address >= 0) { "address must not be negative" }
        require(size in 1..MAX_READ_BYTES) { "size must be 1..$MAX_READ_BYTES" }
        val command = "READ_CORE_MEMORY ${address.toString(16).uppercase()} $size"
        val answersThisRead = { reply: String -> replyAddress(reply) == address }
        val reply = exchange(command, answersThisRead) ?: exchange(command, answersThisRead)
            ?: throw MemoryReadLost(address)
        return decode(address, size, reply)
    }

    /**
     * The [size] bytes at [address], asked once and with the patience of [timeoutMs]: null when no usable reply arrives
     * in time or RetroArch refuses. For a read that is repeated soon anyway (the player's position between cycles),
     * where a lost reply is not worth a retry or a wait.
     */
    fun tryRead(address: Long, size: Int, timeoutMs: Int): ByteArray? {
        require(address >= 0) { "address must not be negative" }
        require(size in 1..MAX_READ_BYTES) { "size must be 1..$MAX_READ_BYTES" }
        val command = "READ_CORE_MEMORY ${address.toString(16).uppercase()} $size"
        val reply = exchange(command, timeoutMs) { replyAddress(it) == address } ?: return null
        return try {
            decode(address, size, reply)
        } catch (_: MemoryReadRefused) {
            null
        }
    }

    /** Closes the request in flight, if any, and refuses new ones (they get no reply) until [reopen]. */
    fun close() {
        synchronized(lock) {
            closed = true
            sockets.forEach { it.close() }
            sockets.clear()
        }
    }

    fun reopen() {
        synchronized(lock) { closed = false }
    }

    /** Sends [command] and returns the first reply [accepts], trimmed, or null when none arrives in time. */
    private fun exchange(command: String, accepts: (String) -> Boolean): String? = exchange(command, timeoutMs, accepts)

    private fun exchange(command: String, timeoutMs: Int, accepts: (String) -> Boolean): String? {
        val socket = open() ?: return null
        try {
            val payload = command.toByteArray(Charsets.US_ASCII)
            val deadline = System.nanoTime() + timeoutMs * NANOS_PER_MS
            socket.send(DatagramPacket(payload, payload.size))
            while (true) {
                val remainingMs = (deadline - System.nanoTime() + NANOS_PER_MS - 1) / NANOS_PER_MS
                if (remainingMs <= 0) return null
                socket.soTimeout = remainingMs.toInt()
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val reply = String(packet.data, 0, packet.length, Charsets.US_ASCII).trim()
                portState = false
                if (accepts(reply)) return reply
            }
        } catch (_: SocketTimeoutException) {
            portState = false
            return null
        } catch (_: PortUnreachableException) {
            portState = true
            return null
        } catch (e: IOException) {
            // Port unreachable, network down, or closed by close(): no reply either way.
            logger.log(Level.FINE, "No reply to $command: ${e.javaClass.simpleName}: ${e.message}")
            return null
        } finally {
            socket.close()
            sockets.remove(socket)
        }
    }

    /** A new socket connected to RetroArch, or null if the client is closed or the host cannot be reached. */
    private fun open(): DatagramSocket? {
        val target = try {
            resolved ?: InetAddress.getByName(host).also { resolved = it }
        } catch (e: IOException) {
            logger.log(Level.FINE, "Cannot resolve $host: ${e.message}")
            return null
        }
        synchronized(lock) {
            if (closed) return null
            val socket = try {
                DatagramSocket().also { it.connect(target, port) }
            } catch (e: IOException) {
                logger.log(Level.FINE, "Cannot open a socket to $endpoint: ${e.message}")
                return null
            }
            sockets += socket
            return socket
        }
    }

    private fun isVersion(reply: String) = VERSION.matches(reply)

    /** The address a `READ_CORE_MEMORY` reply is for, or null if it is any other reply. */
    private fun replyAddress(reply: String): Long? {
        if (!reply.startsWith(READ_PREFIX)) return null
        val end = addressEnd(reply)
        return reply.substring(READ_PREFIX.length, end).toLongOrNull(16)
    }

    /** Where the address of a read reply ends. A reply has up to 16 KB as text: nothing here looks further than this. */
    private fun addressEnd(reply: String): Int {
        var end = READ_PREFIX.length
        while (end < reply.length && reply[end] != ' ' && reply[end] != '\n') end++
        return end
    }

    /**
     * The bytes of a whole, well formed reply, read in place: reads are most of what this app does, several of them a
     * second, and one split into a list of strings kept a core busy. Null for any other reply, which [decode] explains.
     */
    private fun decodeQuickly(size: Int, reply: String): ByteArray? {
        val bytes = ByteArray(size)
        var position = addressEnd(reply)
        for (index in 0 until size) {
            if (position + 3 > reply.length || reply[position] != ' ') return null
            val high = Character.digit(reply[position + 1], 16)
            val low = Character.digit(reply[position + 2], 16)
            if (high < 0 || low < 0) return null
            bytes[index] = (high shl 4 or low).toByte()
            position += 3
        }
        return if (position == reply.length) bytes else null
    }

    private fun decode(address: Long, size: Int, reply: String): ByteArray {
        decodeQuickly(size, reply)?.let { return it }
        val lines = reply.lines()
        val fields = lines[0].split(' ').drop(2).filter { it.isNotEmpty() }
        if (fields.firstOrNull() == "-1") throw MemoryReadRefused(address, fields.drop(1).joinToString(" "))
        lines.drop(1).firstOrNull { it.startsWith("TRUNCATED") }?.let {
            throw MemoryReadRefused(address, "reply cut short by RetroArch ($it)")
        }
        val bytes = ByteArray(fields.size)
        for ((index, field) in fields.withIndex()) {
            val value = if (field.length == 2) field.toIntOrNull(16)?.takeIf { it in 0..0xFF } else null
            bytes[index] = (value ?: throw MemoryReadRefused(address, "not a byte in the reply: '$field'")).toByte()
        }
        if (bytes.size != size) throw MemoryReadRefused(address, "asked for $size bytes, got ${bytes.size}")
        return bytes
    }

    companion object {
        /** The most RetroArch hands out in one `READ_CORE_MEMORY`. */
        const val MAX_READ_BYTES = 16384
        private const val READ_PREFIX = "READ_CORE_MEMORY "

        /** A 16 KB read is a reply of about 49 KB of text; a UDP datagram holds at most 65507. */
        private const val MAX_DATAGRAM = 65535
        private const val NANOS_PER_MS = 1_000_000L
        private val VERSION = Regex("""\d[\w.+-]*""")
        private val logger = Logger.getLogger(RetroArchClient::class.java.name)
    }
}
