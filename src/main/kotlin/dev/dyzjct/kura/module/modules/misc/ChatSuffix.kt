package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.chat.MessageSentEvent
import base.system.event.safeEventListener
import dev.dyzjct.kura.module.modules.client.UiSetting.Theme
import dev.dyzjct.kura.module.modules.client.UiSetting.theme

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    val chat = msetting("Chat", Chat.Text)
    init {
        safeEventListener<MessageSentEvent>(true) { event ->
            if (isEnabled) {
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
            }
        }
    }
    enum class Chat {
        Icon,Kura,Text
    }
    }
