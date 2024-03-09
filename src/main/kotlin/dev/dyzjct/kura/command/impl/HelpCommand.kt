package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.command.Command
import base.utils.chat.ChatUtil

object HelpCommand : Command("help", description = "Print commands description and usage.") {
    init {
        builder.executor {
            val helpMessage = dev.dyzjct.kura.command.CommandManager.getHelpMessage()
            ChatUtil.sendMessage(helpMessage)
        }
    }
}