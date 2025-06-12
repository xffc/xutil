package `fun`.xffc.xutil.minecraft

import `fun`.xffc.xutil.minecraft.packets.Handshake
import `fun`.xffc.xutil.minecraft.packets.Packet
import `fun`.xffc.xutil.minecraft.packets.PacketClass
import `fun`.xffc.xutil.minecraft.packets.readVarint
import `fun`.xffc.xutil.minecraft.packets.writeVarint
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.writeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.io.Buffer

class MinecraftClient(
    val socket: Socket,
    private val packetChunkSize: Int,
    private var state: State,
    protocolVersion: Int,
    action: suspend (MinecraftClient) -> Unit
) {
    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel(autoFlush = true)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val listeners = mutableListOf<suspend Packet.() -> Unit>()

    init {
        scope.launch {
            val address = socket.remoteAddress as InetSocketAddress

            sendPacket(
                Handshake(
                    protocolVersion,
                    address.hostname,
                    address.port.toUShort(),
                    state
                )
            )

            launch { action(this@MinecraftClient) }

            while (!socket.isClosed) {
                receivePacket()?.also { packet ->
                    listeners
                        .asFlow()
                        .map {
                            CoroutineScope(Dispatchers.Default).async {
                                it.invoke(packet)
                            }
                        }
                        .toList()
                        .awaitAll()
                }
            }
        }
    }

    fun addListener(listener: suspend Packet.() -> Unit) {
        listeners.add(listener)
    }

    suspend fun receivePacket(): Packet? {
        var packetId = 0
        var packetLength = 0L
        var header = true

        val data = Buffer()

        while (!socket.isClosed) {
            if (readChannel.availableForRead < 1) continue

            val readLength = readChannel.availableForRead.coerceIn(0, packetChunkSize)

            val input = readChannel.readBuffer(readLength)

            if (header) {
                packetLength = input.readVarint().toLong()
                val tempLength = input.size
                packetId = input.readVarint()
                packetLength -= tempLength - input.size


                header = false
            }

            data.write(input, input.size)

            if (data.size == packetLength)
                break
        }

        return Packet.clientbound
            .find { it.state == state && it.id == packetId }
            ?.read(data)
    }

    fun close() {
        socket.close()
    }

    suspend fun sendPacket(packet: Packet) {
        val packetClass = packet.getClass() as? PacketClass.Serverbound
            ?: throw IllegalArgumentException("Packet is not serverbound")

        val data = Buffer()
        data.writeVarint(packetClass.id)
        packetClass.write(data, packet)

        val output = Buffer()
        output.writeVarint(data.size.toInt())
        output.write(data, data.size)

        writeChannel.writeSource(output)

    }

    companion object {
        suspend fun create(
            address: InetSocketAddress,
            packetChunkSize: Int,
            protocolVersion: Int,
            state: State,
            action: suspend (MinecraftClient) -> Unit = {}
        ): MinecraftClient = MinecraftClient(
            aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(address),
            packetChunkSize,
            state,
            protocolVersion,
            action
        )
    }
}