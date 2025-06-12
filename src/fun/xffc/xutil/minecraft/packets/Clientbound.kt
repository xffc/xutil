package `fun`.xffc.xutil.minecraft.packets

import `fun`.xffc.xutil.minecraft.ServerStatus
import `fun`.xffc.xutil.minecraft.State
import kotlinx.io.Source
import kotlinx.serialization.json.Json

class StatusResponse private constructor(
    val status: ServerStatus
) : Packet {
    companion object : PacketClass.Clientbound(0, State.STATUS) {
        override fun read(buffer: Source): StatusResponse =
            StatusResponse(Json.decodeFromString(buffer.readMCString()))
    }
}

class PingResponse private constructor(
    val timestamp: Long
) : Packet {
    companion object : PacketClass.Clientbound(1, State.STATUS) {
        override fun read(buffer: Source): PingResponse =
            PingResponse(buffer.readLong())
    }
}
