package dev.dyzjct.kura.module.modules.misc

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import melon.events.chat.MessageSentEvent
import melon.system.event.safeEventListener

object ChatSuffix : Module(name = "ChatSuffix", langName = "后缀", category = Category.MISC) {
    init {
        safeEventListener<MessageSentEvent>(true) { event ->
            if (isEnabled) {
                event.message += " ${
                    if (Kura.userState == Kura.UserType.Beta) {
                        "\ud835\udd10\ud835\udd22\ud835\udd29\ud835\udd2c\ud835\udd2b\ud835\udd05\ud835\udd22\ud835\udd31\ud835\udd1e"
                    } else {
                        "\u2c98\u2c89\ud835\udcf5\u2c9f\u2c9b"
                    }
                }"
            }
        }
    }
}