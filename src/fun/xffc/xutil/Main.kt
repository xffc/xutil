package `fun`.xffc.xutil

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.utility.kotlin.extension.request.answerCallbackQuery
import com.pengrad.telegrambot.utility.kotlin.extension.request.getMe
import `fun`.xffc.xutil.callbacks.CallbackHandler
import `fun`.xffc.xutil.callbacks.parseCallback
import `fun`.xffc.xutil.commands.parseCommand
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception

val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

val config: Config = File("config.json").let {
    if (it.createNewFile()) it.writeText(prettyJson.encodeToString(Config())) // write default config if created
    prettyJson.decodeFromString(it.readText())
}

val bot = TelegramBot(config.botToken)

val username: String = bot.getMe().user().username()

fun main() {
    bot.setUpdatesListener { updates ->
        updates.forEach { update ->
            CommandScope().launch {
                if (update.callbackQuery() != null && update.callbackQuery().maybeInaccessibleMessage() != null) {
                    try {
                        parseCallback(update)?.also {
                            it.first.call(update, it.second)
                        }
                    } catch (e: Exception) {
                        bot.answerCallbackQuery(
                            update.callbackQuery().id()
                        ) {
                            text("❌ ${e.localizedMessage}")
                            showAlert(true)
                        }
                    }

                    return@launch
                }

                if (update.message() != null &&
                    update.message().text() != null &&
                    update.message().text().startsWith("/")
                ) {
                    try {
                        parseCommand(update)?.also {
                            it.first.call(update, it.second)
                        }
                    } catch (e: Exception) {
                        bot.replyTo(
                            update.message().chat().id(),
                            update.message().messageId(),
                            "❌ <b>${e.localizedMessage}</b>"
                        ) {
                            parseMode(ParseMode.HTML)
                        }
                    }
                }
            }
        }

        UpdatesListener.CONFIRMED_UPDATES_ALL
    }
}

@Serializable
data class Config(
    val botToken: String = "BOT_TOKEN"
)