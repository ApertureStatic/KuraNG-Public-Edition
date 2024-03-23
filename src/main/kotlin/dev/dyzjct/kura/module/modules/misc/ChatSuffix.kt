package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import base.events.chat.MessageSentEvent
import base.system.event.safeEventListener

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    init {
        safeEventListener<MessageSentEvent>(true) { event ->
            if (isEnabled) {
                event.message += " kนrค"
            }
        }
    }
}
