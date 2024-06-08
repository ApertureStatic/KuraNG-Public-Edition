package dev.dyzjct.kura.command.impl

import base.utils.chat.ChatUtil.DARK_AQUA
import base.utils.chat.ChatUtil.DARK_BLUE
import base.utils.chat.ChatUtil.WHITE
import base.utils.chat.ChatUtil.sendMessage
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.manager.NotificationManager.addNotification
import dev.dyzjct.kura.module.modules.client.IRC
import dev.dyzjct.kura.module.modules.client.IRC.mode
import net.minecraft.client.MinecraftClient


object IRCCommand : Command("irc") {
    init {
        builder.literal {
            irc { message ->
                executor {
                    // 我下个断点看看
                    Kura.ircSocket.chat(message.value())

                when (mode as IRC.Mode) {
                    IRC.Mode.Both -> {
                        sendMessage(DARK_AQUA + "[IRC] " + DARK_BLUE + WHITE + MinecraftClient.getInstance().getSession().username + " : ${message.value()}")
                        addNotification(DARK_AQUA + "[IRC] " + DARK_BLUE + WHITE + MinecraftClient.getInstance().getSession().username + " : ${message.value()}")
                    }

                    IRC.Mode.Chat -> sendMessage(DARK_AQUA + "[IRC] " + WHITE + MinecraftClient.getInstance().getSession().username + " : ${message.value()}")
                    IRC.Mode.Notification -> addNotification(DARK_AQUA + "[IRC] " + WHITE + MinecraftClient.getInstance().getSession().username + " : ${message.value()}")
                }
            }}
        }
    }
}