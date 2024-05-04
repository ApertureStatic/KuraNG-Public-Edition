package dev.dyzjct.kura.module.modules.misc

import base.events.chat.MessageSentEvent
import base.system.event.safeEventListener
import base.utils.chat.ChatUtil
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    val chat = msetting("Chat", Chat.Text)
    val debug = bsetting("Debug",false)

    init {
        safeEventListener<MessageSentEvent> { event ->
                var message = event.message
                when (chat.value) {
                    Chat.Kura -> {
                        message += " Φ kนrค"
                    }

                    Chat.Icon -> {
                        message += " ⚜ⓀⓊⓇⒶ⚜"
                    }

                    Chat.Text -> {
                        message += " | \uD835\uDD76\uD835\uDD9A\uD835\uDD97\uD835\uDD86.\uD835\uDD89\uD835\uDD8A\uD835\uDD9B"
                    }
                }
                if (debug.value) ChatUtil.sendMessage("CHAT SUFFIX IS RUNNING")
        }
    }

    enum class Chat {
        Icon, Kura, Text
    }
}
