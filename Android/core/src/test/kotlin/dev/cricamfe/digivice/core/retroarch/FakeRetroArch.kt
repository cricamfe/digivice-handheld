package dev.cricamfe.digivice.core.retroarch

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * A UDP server that answers RetroArch's network commands from a RAM image, with the reply formats of
 * `Tools/RetroArchProbe/fake_retroarch.py` and the ones measured on the real device (lowercase hex address, uppercase
 * bytes, no byte count). It binds an ephemeral port on 127.0.0.1 and logs every command it receives.
 *
 * The behaviour switches are volatile and can be flipped between ticks of a test.
 */
class FakeRetroArch(
    @Volatile var ram: ByteArray = ByteArray(RAM_SIZE),
    @Volatile var title: String = DEFAULT_TITLE,
) : AutoCloseable {
    class Received(val command: String, val nanos: Long)

    /** `PLAYING`, `PAUSED` or `CONTENTLESS`. */
    @Volatile var state: String = "PLAYING"

    /** False answers `-1 no memory map defined` to every read, like a build without the memory map fallback. */
    @Volatile var memoryMap: Boolean = true

    /** True: the server receives and logs, but never answers. */
    @Volatile var silent: Boolean = false

    /**
     * The time of the test, when it is not the real one: [Received.nanos] is read from it, and [latencyMs] and
     * [statusDelayMs] advance it instead of sleeping. Set it before the first command.
     */
    @Volatile var clock: TestClock? = null

    /** Delay before answering any command, like RetroArch's one request per frame (about 20 ms). */
    @Volatile var latencyMs: Long = 0

    /**
     * Called, in the server's thread, for each read with its address, once its reply is made and before it is sent: to
     * change the RAM (or the [clock]) mid-cycle, after that read and before any later one.
     */
    @Volatile var afterRead: ((Long) -> Unit)? = null

    /** Delay before answering `GET_STATUS`, to make a tick slow. */
    @Volatile var statusDelayMs: Long = 0

    /** Before every read reply, send a reply to a read of another address (a late reply of an earlier request). */
    @Volatile var staleReplyBeforeRead: Boolean = false

    /** Send every read reply twice. */
    @Volatile var duplicateReadReplies: Boolean = false

    /** Answer reads with half of the bytes and a `TRUNCATED` tail. */
    @Volatile var truncateReads: Boolean = false

    private val dropsLeft = AtomicInteger(0)
    private val answersBeforeRefusing = AtomicInteger(Int.MAX_VALUE)
    private val log = CopyOnWriteArrayList<Received>()

    private val socket = DatagramSocket(0, InetAddress.getByName("127.0.0.1")).apply {
        // macOS caps a datagram at 9 KB unless the send buffer is larger; a 16 KB read is a 49 KB reply.
        sendBufferSize = 65535
        soTimeout = 20
    }

    @Volatile private var running = true
    private val thread = Thread(::serve, "FakeRetroArch").apply { isDaemon = true }

    val port: Int get() = socket.localPort

    /** Every command received, in order. */
    val received: List<Received> get() = log.toList()

    val commands: List<String> get() = log.map { it.command }

    /** Ignores the next [count] `READ_CORE_MEMORY` requests (they are logged but not answered). */
    fun dropNextReads(count: Int) = dropsLeft.set(count)

    /** Answers the next [count] reads normally and then refuses the rest with `no descriptor for address`. */
    fun refuseReadsAfter(count: Int) = answersBeforeRefusing.set(count)

    fun clearLog() = log.clear()

    init {
        thread.start()
    }

    private fun serve() {
        val buffer = ByteArray(4096)
        while (running) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: java.net.SocketException) {
                break
            }
            val command = String(packet.data, 0, packet.length, Charsets.US_ASCII).trim()
            log.add(Received(command, clock?.nanoTime() ?: System.nanoTime()))
            if (silent) continue
            delayFor(latencyMs)
            val replies = repliesTo(command)
            if (command.startsWith("READ_CORE_MEMORY ")) {
                afterRead?.invoke(command.split(' ')[1].toLong(16))
            }
            for (reply in replies) {
                socket.send(DatagramPacket(reply, reply.size, packet.socketAddress))
            }
        }
    }

    private fun repliesTo(command: String): List<ByteArray> {
        if (command == "VERSION") return listOf(text("1.22.2\n"))
        if (command == "GET_STATUS") {
            delayFor(statusDelayMs)
            return listOf(text(statusLine()))
        }
        val parts = command.split(' ')
        if (parts.size != 3 || parts[0] != "READ_CORE_MEMORY") return emptyList()
        if (dropsLeft.getAndUpdate { if (it > 0) it - 1 else it } > 0) return emptyList()

        val address = parts[1].toLong(16)
        val size = parts[2].toInt()
        val prefix = "READ_CORE_MEMORY ${address.toString(16)}"
        val reply = when {
            !memoryMap -> "$prefix -1 no memory map defined\n"
            answersBeforeRefusing.getAndUpdate { if (it > 0) it - 1 else it } <= 0 ->
                "$prefix -1 no descriptor for address\n"
            size > READ_LIMIT -> "$prefix -1 too many bytes requested\n"
            address >= ram.size -> "$prefix -1 no descriptor for address\n"
            truncateReads -> {
                val shortSize = size / 2
                "$prefix ${hex(address, shortSize)}\nTRUNCATED ${size - shortSize}\n"
            }
            else -> "$prefix ${hex(address, size)}\n"
        }
        val replies = mutableListOf<ByteArray>()
        if (staleReplyBeforeRead) {
            val other = address + 0x10
            replies += text("READ_CORE_MEMORY ${other.toString(16)} ${hex(other, 4)}\n")
        }
        replies += text(reply)
        if (duplicateReadReplies) replies += text(reply)
        return replies
    }

    private fun delayFor(ms: Long) {
        if (ms <= 0) return
        clock?.advance(ms) ?: Thread.sleep(ms)
    }

    private fun statusLine() = when (state) {
        "CONTENTLESS" -> "GET_STATUS CONTENTLESS\n"
        else -> "GET_STATUS $state $title\n"
    }

    private fun hex(address: Long, size: Int): String {
        val start = address.toInt()
        val end = minOf(start + size, ram.size)
        return buildString {
            for (index in start until end) {
                if (index > start) append(' ')
                val value = ram[index].toInt() and 0xFF
                append(DIGITS[value shr 4]).append(DIGITS[value and 0xF])
            }
        }
    }

    private fun text(value: String) = value.toByteArray(Charsets.US_ASCII)

    override fun close() {
        running = false
        thread.join(1000)
        socket.close()
    }

    companion object {
        private const val DIGITS = "0123456789ABCDEF"
        const val RAM_SIZE = 0x200000
        const val READ_LIMIT = 16384
        const val DEFAULT_TITLE = "playstation,Digimon World 2003 (Europe) (En,Fr,De,Es,It)"
    }
}
