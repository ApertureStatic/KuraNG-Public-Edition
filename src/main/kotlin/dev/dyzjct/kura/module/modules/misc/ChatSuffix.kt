package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.event.events.chat.MessageSentEvent
import dev.dyzjct.kura.event.eventbus.safeEventListener
import base.utils.chat.ChatUtil
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    val chat = msetting("Chat", Chat.Text)
    val debug = bsetting("Debug", false)

    init {
        safeEventListener<MessageSentEvent> { event ->
            when (chat.value) {
                Chat.Kura -> {
                    event.message += " Φ kนrค"
                }

                Chat.Icon -> {
                    event.message += " ⚜ⓀⓊⓇⒶ⚜"
                }

                Chat.Text -> {
                    event.message += " | \uD835\uDD76\uD835\uDD9A\uD835\uDD97\uD835\uDD86.\uD835\uDD89\uD835\uDD8A\uD835\uDD9B"
                }
            }
            if (debug.value) ChatUtil.sendMessage("CHAT SUFFIX IS RUNNING")
        }
    }

    enum class Chat {
        Icon, Kura, Text
    }
}
