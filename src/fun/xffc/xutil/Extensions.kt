package `fun`.xffc.xutil

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.ReplyParameters
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.response.SendResponse
import com.pengrad.telegrambot.utility.kotlin.extension.request.sendMessage

fun TelegramBot.replyTo(chatId: Long, messageId: Int, text: String, modifier: SendMessage.() -> Unit = {}): SendResponse =
    sendMessage(chatId, text) {
        modifier()
        replyParameters(ReplyParameters(messageId))
    }

fun Collection<InlineKeyboardButton>.asKeyboard(): InlineKeyboardMarkup =
    InlineKeyboardMarkup(this.toTypedArray())

fun InlineKeyboardButton.asKeyboard(): InlineKeyboardMarkup =
    InlineKeyboardMarkup(arrayOf(this))