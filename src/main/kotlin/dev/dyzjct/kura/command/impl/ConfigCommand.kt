package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.manager.FileManager
import base.utils.chat.ChatUtil

object ConfigCommand : Command("config", description = "Config Command (save, reload)") {
    init {
        builder.literal {
            match("save") {
                executor {
                    FileManager.saveAll()
                    ChatUtil.sendMessage("Saved Config!")
                }
            }

            match("reload") {
                executor {
                    FileManager.loadAll()
                    ChatUtil.sendMessage("Config Reloaded!")
                }
            }
        }
    }
}