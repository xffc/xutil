package `fun`.xffc.xutil.commands

import com.pengrad.telegrambot.model.request.ParseMode
import `fun`.xffc.xutil.bot
import `fun`.xffc.xutil.replyTo
import `fun`.xffc.xutil.asKeyboard
import `fun`.xffc.xutil.callbacks.CallbackHandler
import `fun`.xffc.xutil.util.censysButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

object PingCommand : Command {
    override val name = "ping"

    override val args = listOf(
        Command.Argument.AddressArgument("address", true)
    )

    override suspend fun onCommand(context: Command.Context) {
        val address = context.getArgument("address")!! as String
        val inetAddress = InetAddress.getByName(address)

        val id = bot.replyTo(
            context.chatId,
            context.messageId,
            getResponse(inetAddress)
        ) {
            parseMode(ParseMode.HTML)
            replyMarkup(censysButton(inetAddress.hostAddress).asKeyboard())
        }.message().messageId()

        CallbackHandler.register(context.userId, id)
    }

    suspend fun getResponse(address: InetAddress): String {
        val response = withContext(Dispatchers.IO) {
            val startAt = System.currentTimeMillis()
            val isReachable = address.isReachable(3000)
            isReachable to System.currentTimeMillis() - startAt
        }

        return if (response.first) {
            "✅ Ping successful!\nIP: <code>${address.hostAddress}</code>\nResponse time: <code>${response.second}ms</code>"
        } else {
            "❌ Ping unsuccessful, host is unreachable\nIP: <code>${address.hostAddress}</code>"
        }
    }
}