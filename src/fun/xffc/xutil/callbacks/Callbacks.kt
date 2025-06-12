package `fun`.xffc.xutil.callbacks

import com.mayakapps.kache.InMemoryKache
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage
import kotlin.time.Duration.Companion.minutes

sealed interface CallbackHandler {
    companion object {
        val entries = CallbackHandler::class.sealedSubclasses.mapNotNull { it.objectInstance }

        val keyboardCache = InMemoryKache<Long, Int>(5 * 1024) /*user: message*/ {
            expireAfterWriteDuration = 1.minutes
        }

        suspend fun register(user: Long, message: Int) {
            keyboardCache.put(user, message)
        }
    }

    val name: String

    suspend fun call(update: Update, args: List<String>) {
        onCallback(Context(args, update.callbackQuery().maybeInaccessibleMessage(), update))
    }

    suspend fun onCallback(context: Context)

    data class Context(
        val arguments: List<String>,
        val message: MaybeInaccessibleMessage,
        val update: Update
    ) {
        val chatId: Long = message.chat().id()
        val userId: Long = update.callbackQuery().from().id()
    }
}

suspend fun parseCallback(update: Update): Pair<CallbackHandler, List<String>>? {
    val messageId = CallbackHandler.keyboardCache.get(update.callbackQuery().from().id())

    if (messageId == null)
        throw Exception("You don't have active keyboard to use")

    if (messageId != update.callbackQuery().maybeInaccessibleMessage().messageId())
        throw Exception("You can't use this keyboard")

    val text = update.callbackQuery().data()

    val args: List<String>
    var handlerName: String

    text.split(":").also {
        args = it.drop(1)
        handlerName = it[0]
    }

    return CallbackHandler.entries.find { it.name == handlerName }?.let { it to args }
}