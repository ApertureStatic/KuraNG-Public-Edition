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


object IRCCommand : Command("irc") {
    init {
        builder.literal {
            irc { message ->
                executor {
                    Kura.ircSocket.chat(message.value())
                    Wrapper.player?.let { player ->
                        when (mode as IRC.Mode) {
                            IRC.Mode.Both -> {
                                sendMessage(DARK_AQUA + "[IRC] " + DARK_BLUE + "[" + BLUE + "Kura" + DARK_BLUE + "] " + WHITE + player.name.string + " say: " + message.value())
                                addNotification(DARK_AQUA + "[IRC] " + DARK_BLUE + "[" + BLUE + "Kura" + DARK_BLUE + "] " + WHITE + player.name.string + " say: " + message.value())
                            }

                            IRC.Mode.Chat -> sendMessage(DARK_AQUA + "[IRC] " + DARK_BLUE + "[" + BLUE + "Kura" + DARK_BLUE + "] " + WHITE + player.name.string + " say: " + message.value())
                            IRC.Mode.Notification -> addNotification(DARK_AQUA + "[IRC] " + DARK_BLUE + "[" + BLUE + "Kura" + DARK_BLUE + "] " + WHITE + player.name.string + " say: " + message.value())
                        }
                    }
                }
            }
        }
    }
}