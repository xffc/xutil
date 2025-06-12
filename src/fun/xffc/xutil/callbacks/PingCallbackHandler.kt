package `fun`.xffc.xutil.callbacks

import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.utility.kotlin.extension.request.sendMessage
import `fun`.xffc.xutil.asKeyboard
import `fun`.xffc.xutil.bot
import `fun`.xffc.xutil.commands.PingCommand
import `fun`.xffc.xutil.util.censysButton
import java.net.IDN
import java.net.InetAddress

object PingCallbackHandler: CallbackHandler {
    override val name = "ping"

    override suspend fun onCallback(context: CallbackHandler.Context) {
        val inetAddress = InetAddress.getByName(IDN.toASCII(context.arguments[0]))

        val id = bot.sendMessage(
            context.chatId,
            PingCommand.getResponse(inetAddress)
        ) {
            parseMode(ParseMode.HTML)
            replyMarkup(censysButton(inetAddress.hostAddress).asKeyboard())
        }.message().messageId()

        CallbackHandler.register(context.userId, id)
    }
}