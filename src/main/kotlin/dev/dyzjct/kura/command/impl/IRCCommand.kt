package dev.dyzjct.kura.command.impl

import base.notification.NotificationManager.addNotification
import base.utils.Wrapper
import base.utils.chat.ChatUtil.BLUE
import base.utils.chat.ChatUtil.DARK_AQUA
import base.utils.chat.ChatUtil.DARK_BLUE
import base.utils.chat.ChatUtil.WHITE
import base.utils.chat.ChatUtil.sendMessage
import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.command.Command
import dev.dyzjct.kura.module.modules.client.IRC
import dev.dyzjct.kura.module.modules.client.IRC.mode
import helper.kura.socket.SocketManager


object IRCCommand : Command("irc") {
    init {
        builder.literal {
            irc { message ->
                executor {
                    // 我下个断点看看
                    Kura.ircSocket.chat(message.value())
                }
            }
        }
    }
}