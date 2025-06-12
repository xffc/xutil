package `fun`.xffc.xutil.util

import com.pengrad.telegrambot.model.request.InlineKeyboardButton

fun censysButton(addr: String): InlineKeyboardButton =
    InlineKeyboardButton("🔍 Censys", url = "https://search.censys.io/hosts/$addr")

fun pingButton(addr: String): InlineKeyboardButton =
    InlineKeyboardButton("📣 Ping", callbackData = "ping:$addr")