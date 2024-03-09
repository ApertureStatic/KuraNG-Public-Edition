package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.command.Command
import base.utils.chat.ChatUtil

object BindCommand : Command("bind", description = "Bind module to key.") {
    init {
        builder.literal {
            module { moduleArgument ->
                key { keyArgument ->
                    executor {
                        val module = moduleArgument.value()

                        module.bind = keyArgument.value()
                        ChatUtil.sendMessage("Bind ${module.moduleName} to ${keyArgument.originValue()}")
                    }
                }
            }
        }
    }
}