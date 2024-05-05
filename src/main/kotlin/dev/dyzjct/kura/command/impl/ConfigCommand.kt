package dev.dyzjct.kura.command.impl

import base.utils.chat.ChatUtil
import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.manager.FileManager
import dev.dyzjct.kura.module.modules.client.CombatSystem

object ConfigCommand : Command("config", description = "Config Command (save, reload)") {
    init {
        builder.literal {
            match("save") {
                executor {
                    FileManager.saveAll(CombatSystem.combatMode.value.name)
                    ChatUtil.sendMessage("Saved Config!")
                }
            }

            match("reload") {
                executor {
                    FileManager.loadCombatSystem()
                    FileManager.loadAll(CombatSystem.combatMode.value.name)
                    ChatUtil.sendMessage("Config Reloaded!")
                }
            }
        }
    }
}