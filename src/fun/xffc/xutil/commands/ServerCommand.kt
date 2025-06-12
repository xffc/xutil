package `fun`.xffc.xutil.commands

import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.model.request.ReplyParameters
import com.pengrad.telegrambot.request.SendPhoto
import `fun`.xffc.xutil.bot
import `fun`.xffc.xutil.minecraft.MinecraftClient
import `fun`.xffc.xutil.minecraft.ServerStatus
import `fun`.xffc.xutil.minecraft.State
import `fun`.xffc.xutil.minecraft.packets.PingRequest
import `fun`.xffc.xutil.minecraft.packets.PingResponse
import `fun`.xffc.xutil.minecraft.packets.StatusRequest
import `fun`.xffc.xutil.minecraft.packets.StatusResponse
import `fun`.xffc.xutil.replyTo
import `fun`.xffc.xutil.asKeyboard
import `fun`.xffc.xutil.callbacks.CallbackHandler
import `fun`.xffc.xutil.util.censysButton
import `fun`.xffc.xutil.util.pingButton
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.net.InetAddress
import java.util.Base64

object ServerCommand : Command {
    override val name = "server"
    override val args = listOf(
        Command.Argument.AddressArgument("address", true),
        Command.Argument.PortArgument("port", false),
        Command.Argument.NumberArgument("protocol", false)
    )

    override suspend fun onCommand(context: Command.Context) {
        val address = context.getArgument("address") as String
        val port = context.getArgument("port") as? Int ?: 25565
        val protocol = context.getArgument("protocol") as? Int ?: 770

        val ip = InetAddress.getByName(address).hostAddress

        val info = withContext(Dispatchers.IO) {
            val defer = CompletableDeferred<ServerInfo>()

            var status: ServerStatus? = null

            val client = MinecraftClient.create(
                InetSocketAddress(address, port),
                1024, // todo: add in config
                protocol,
                State.STATUS
            ) { client ->
                client.sendPacket(StatusRequest())
            }

            client.addListener {
                when (this) {
                    is StatusResponse -> {
                        status = this.status
                        client.sendPacket(PingRequest(System.currentTimeMillis()))
                    }

                    is PingResponse -> defer.complete(ServerInfo(status!!, System.currentTimeMillis() - timestamp))

                    else -> {}
                }
            }

            defer.await()
        }

        val text = StringBuilder().apply {
            ip.also { appendLine("🪪 <b>IP</b>: <code>$ip</code>") }
            info.ping.also { appendLine("⏱️ <b>Ping</b>: <code>${it}ms</code>") }
            info.status.version.also { appendLine("🏷️ <b>Version</b>: <code>${it.name}</code> (<i>${it.protocol}</i>)") }
            info.status.enforcesSecureChat?.also { appendLine("⚠️ <b>Enforces secure chat</b>: <b>${if (it) "yes" else "no"}</b>") }
            info.status.description?.also {
                appendLine(
                    "📄 <b>MOTD</b>:\n<pre code=\"raw\">${
                        PlainTextComponentSerializer.plainText().serialize(it)
                    }</pre>"
                )
            }
            info.status.players?.also {
                appendLine(
                    "👥 <b>Players</b>: <code>${it.online}/${it.max}</code>\n${
                        it.sample.joinToString(
                            ", "
                        ) { p -> " - <b>${p.name}</b>" }
                    }"
                )
            }
        }.toString()

        val keyboard = setOf(censysButton(ip), pingButton(address)).asKeyboard()

        val id =
            if (info.status.favicon == null) {
                bot.replyTo(
                    context.chatId,
                    context.messageId,
                    text
                ) {
                    parseMode(ParseMode.HTML)
                    replyMarkup(keyboard)
                }.message().messageId()
            } else {
                bot.execute(
                    SendPhoto(
                        context.chatId,
                        Base64.getDecoder().decode(info.status.favicon.split("data:image/png;base64,")[1]),
                    )
                        .caption(text)
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(keyboard)
                        .replyParameters(ReplyParameters(context.messageId))
                ).message().messageId()
            }

        CallbackHandler.register(context.userId, id)
    }

    data class ServerInfo(
        val status: ServerStatus,
        val ping: Long
    )
}
