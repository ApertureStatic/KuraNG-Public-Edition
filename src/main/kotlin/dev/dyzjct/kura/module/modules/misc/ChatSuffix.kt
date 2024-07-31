package dev.dyzjct.kura.module.modules.misc

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.event.eventbus.safeEventListener
import dev.dyzjct.kura.event.events.chat.MessageSentEvent
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    private val chat by msetting("Chat", Chat.Custom)
    private val text by ssetting("Text", "Kura-v1.0.4").enumIs(chat, Chat.Custom)
    val debug = bsetting("Debug", false)

    init {
        safeEventListener<MessageSentEvent> { event ->
            when (chat) {
                Chat.Kura -> {
                    event.message += " Φ kนrค"
                }

                Chat.Icon -> {
                    event.message += " ⚜ⓀⓊⓇⒶ⚜"
                }

                Chat.Custom -> {
                    event.message += " | $text"
                }
            }
            if (debug.value) ChatUtil.sendMessage("CHAT SUFFIX IS RUNNING")
        }
    }

    enum class Chat {
        Icon, Kura, Custom
    }
}
