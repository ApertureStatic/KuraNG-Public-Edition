package dev.dyzjct.kura.command.impl

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.command.Command


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