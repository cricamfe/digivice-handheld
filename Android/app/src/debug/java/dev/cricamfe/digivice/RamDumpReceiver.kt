package dev.cricamfe.digivice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/**
 * Debug builds only: writes the console RAM to files, for finding addresses by comparing dumps.
 * `am broadcast -n dev.cricamfe.digivice/.RamDumpReceiver --ei count 10 --ei pause 2000`
 * It only sends `READ_CORE_MEMORY`.
 */
class RamDumpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val count = intent.getIntExtra("count", 1)
        val pause = intent.getIntExtra("pause", 0).toLong()
        val folder = File(context.getExternalFilesDir(null), "ram").apply { mkdirs() }
        thread(name = "ram-dump") {
            DatagramSocket().use { socket ->
                socket.soTimeout = 200
                socket.connect(InetAddress.getByName("127.0.0.1"), 55355)
                repeat(count) { index ->
                    val started = System.currentTimeMillis()
                    val ram = dump(socket)
                    File(folder, "$started.bin").writeBytes(ram)
                    Log.i("RamDump", "$index took ${System.currentTimeMillis() - started} ms")
                    Thread.sleep(pause)
                }
            }
            Log.i("RamDump", "done")
        }
    }

    private fun dump(socket: DatagramSocket): ByteArray {
        val ram = ByteArray(RamSize)
        val reply = ByteArray(ChunkSize * 3 + 64)
        for (offset in 0 until RamSize step ChunkSize) {
            val request = "READ_CORE_MEMORY ${offset.toString(16)} $ChunkSize".toByteArray()
            socket.send(DatagramPacket(request, request.size))
            val packet = DatagramPacket(reply, reply.size)
            try {
                socket.receive(packet)
            } catch (_: SocketTimeoutException) {
                continue
            }
            val parts = String(reply, 0, packet.length, Charsets.US_ASCII).trim().split(' ')
            parts.drop(2).forEachIndexed { index, part ->
                part.toIntOrNull(16)?.let { ram[offset + index] = it.toByte() }
            }
        }
        return ram
    }

    private companion object {
        const val RamSize = 0x200000
        const val ChunkSize = 16384
    }
}
