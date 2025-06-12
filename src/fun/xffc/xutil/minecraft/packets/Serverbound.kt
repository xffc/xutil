package `fun`.xffc.xutil.minecraft.packets

import `fun`.xffc.xutil.minecraft.State
import kotlinx.io.Sink
import kotlinx.io.writeUShort

class Handshake(
    val protocolVersion: Int,
    val serverAddress: String,
    val serverPort: UShort,
    val intent: State
) : Packet {
    companion object : PacketClass.Serverbound(0, State.HANDSHAKING) {
        override fun write(buffer: Sink, packet: Packet) = (packet as Handshake).run {
            buffer.writeVarint(protocolVersion)
            buffer.writeMCString(serverAddress)
            buffer.writeUShort(serverPort)
            buffer.writeVarint(intent.ordinal + 1)
        }
    }
}

class StatusRequest : Packet {
    companion object : PacketClass.Serverbound(0, State.STATUS)
}

class PingRequest(
    val timestamp: Long
) : Packet {
    companion object : PacketClass.Serverbound(1, State.STATUS) {
        override fun write(buffer: Sink, packet: Packet) = (packet as PingRequest).run {
            buffer.writeLong(timestamp)
        }
    }
}