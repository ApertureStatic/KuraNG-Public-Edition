package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.command.Command
import base.utils.chat.ChatUtil

object PrefixCommand : Command("prefix") {
    init {
        builder.literal {
            any { anyArgs ->
                executor {
                    val prefix = anyArgs.value()
                    Kura.commandPrefix.value = prefix
                    ChatUtil.sendMessage("Prefix Has Been Set To: $prefix")
                }
            }
        }
    }
}