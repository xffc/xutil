package `fun`.xffc.xutil.commands

import com.pengrad.telegrambot.model.Update
import `fun`.xffc.xutil.username
import java.net.IDN

sealed interface Command {
    companion object {
        val entries = Command::class.sealedSubclasses.mapNotNull { it.objectInstance }
    }

    val name: String
    val args: List<Argument<*>>

    suspend fun call(update: Update, args: List<String>) {
        onCommand(Context(this, args, update))
    }

    suspend fun onCommand(context: Context)

    sealed interface Argument<T> {
        val name: String
        val required: Boolean

        fun getValue(from: String): T

        data class TextArgument(
            override val name: String,
            override val required: Boolean
        ) : Argument<String> {
            override fun getValue(from: String) = from
        }

        data class NumberArgument(
            override val name: String,
            override val required: Boolean
        ) : Argument<Double> {
            override fun getValue(from: String) =
                from.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Argument $name is not a number")
        }

        data class AddressArgument(
            override val name: String,
            override val required: Boolean
        ) : Argument<String> {
            override fun getValue(from: String): String =
                IDN.toASCII(from)
        }

        data class PortArgument(
            override val name: String,
            override val required: Boolean
        ) : Argument<Int> {
            override fun getValue(from: String): Int =
                (from.toIntOrNull() ?: throw IllegalArgumentException("Argument $name is not an integer")).also {
                    require(it in 0..UShort.MAX_VALUE.toInt()) {
                        "Argument $name must be in range (0, ${UShort.MAX_VALUE})"
                    }
                }
        }
    }

    data class Context(
        private val command: Command,
        private val arguments: List<String>,
        val update: Update
    ) {
        val chatId = update.message().chat().id()
        val messageId = update.message().messageId()
        val userId = update.message().from().id()

        fun getArgument(name: String): Any? =
            command.args
                .find { it.name == name }
                .let {
                    if (it == null) throw IllegalArgumentException("Argument $name is undefined")

                    val text = arguments.getOrNull(command.args.indexOf(it))

                    if (text == null) {
                        if (it.required)
                            throw IllegalArgumentException("Required argument $name is not present")
                        return@let null
                    }

                    it.getValue(text)
                }
    }
}

fun parseCommand(update: Update): Pair<Command, List<String>>? {
    val text = update.message().text()

    val args: List<String>
    var commandName: String

    text.split(" ").also {
        args = it.drop(1)
        commandName = it[0].drop(1)
    }

    if ('@' in commandName) {
        commandName.split("@").also {
            if (it.size != 2) return null
            commandName = it[0]
            if (it[1] != username) return null
        }
    }

    return Command.entries.find { it.name == commandName }?.let { it to args }
}