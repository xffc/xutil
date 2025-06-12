package `fun`.xffc.xutil.minecraft.packets

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString

private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

fun Source.readVarint(): Int {
    var value = 0
    var position = 0

    while (true) {
        val byte = readByte().toInt()

        value = value or ((byte and SEGMENT_BITS) shl position)
        if ((byte and CONTINUE_BIT) == 0) return value

        position += 7
        if (position >= 32) throw RuntimeException("Varint is too big")
    }
}

fun Sink.writeVarint(value: Int) {
    var current = value

    while (true) {
        if ((current and SEGMENT_BITS.inv()) == 0) {
            writeByte(current.toByte())
            return
        }

        writeByte(((current and SEGMENT_BITS) or CONTINUE_BIT).toByte())
        current = current ushr 7
    }
}

fun Source.readMCString(): String =
    readString(readVarint().toLong())

fun Sink.writeMCString(value: String) {
    writeVarint(value.length)
    writeString(value)
}