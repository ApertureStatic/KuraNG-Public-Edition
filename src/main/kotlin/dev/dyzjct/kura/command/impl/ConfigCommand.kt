package dev.dyzjct.kura.command.impl

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.command.Command

object ConfigCommand : Command("config", description = "Config Command (save, reload)") {
    init {
        builder.literal {
            match("save") {
                executor {
                    ChatUtil.sendMessage("Saved Config!")
                }
            }

            match("reload") {
                executor {
                    ChatUtil.sendMessage("Config Reloaded!")
                }
            }
        }
    }
}