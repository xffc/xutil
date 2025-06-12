package `fun`.xffc.xutil.minecraft.packets

import `fun`.xffc.xutil.minecraft.State
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.reflect.full.companionObjectInstance

sealed interface Packet {
    fun getClass(): PacketClass =
        this::class.companionObjectInstance as PacketClass

    companion object {
        val clientbound = PacketClass.Clientbound::class.sealedSubclasses.map { it.objectInstance as PacketClass.Clientbound }
        val serverbound = PacketClass.Serverbound::class.sealedSubclasses.map { it.objectInstance as PacketClass.Serverbound }
    }
}

sealed class PacketClass(val id: Int, val state: State) {
    sealed class Clientbound(id: Int, state: State) : PacketClass(id, state) {
        abstract fun read(buffer: Source): Packet
    }

    sealed class Serverbound(id: Int, state: State) : PacketClass(id, state) {
        open fun write(buffer: Sink, packet: Packet) {}
    }
}